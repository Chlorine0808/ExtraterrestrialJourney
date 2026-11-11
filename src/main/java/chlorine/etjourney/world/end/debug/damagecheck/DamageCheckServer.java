package chlorine.etjourney.world.end.debug.damagecheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.ResistanceSources;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Server half of the damage check: hits the joined player with and without immunity armor and writes the results. */
public final class DamageCheckServer {

    /** Ticks the player must have lived before the checks start: past the 60-tick join invulnerability. */
    private static final int JOIN_TICKS = 100;
    private static final float EPSILON = 0.01f;
    /** DE shield points written into each armor piece. */
    private static final float SHIELD_POINTS = 1000;

    static volatile boolean done;

    private final List<String> lines = new ArrayList<>();
    private boolean started;
    /** Whether the last LivingDeathEvent of the player reached the end of the bus uncancelled. */
    private Boolean deathPassed;
    /** Translation key of the death message the combat tracker would print at that death. */
    private String deathKey;
    /** Makes a HIGHEST handler cancel any hurt event and zero it, as an early-cancelling mod would. */
    private boolean cancelHurtFirst;
    /** Makes a registered resistance source halve chaos on the player. */
    private static volatile boolean halveChaos;

    /** Other checks that change the player and must finish before the hits start. */
    private static final AtomicInteger HOLDS = new AtomicInteger();

    private DamageCheckServer() {}

    /** Whether this run is the damage check; other checks may join it. */
    public static boolean requested() {
        return Boolean.getBoolean("etj.damageCheck");
    }

    /** Holds the hits back until {@link #release()}. */
    public static void hold() {
        HOLDS.incrementAndGet();
    }

    public static void release() {
        HOLDS.decrementAndGet();
    }

    static void register() {
        ResistanceSources.register((target, out) -> {
            Element chaos = ElementRegistry.get("chaos");
            if (halveChaos && chaos != null) out.multiply(chaos, 0.5f);
        });
        DamageCheckServer server = new DamageCheckServer();
        FMLCommonHandler.instance()
            .bus()
            .register(server);
        MinecraftForge.EVENT_BUS.register(server);
    }

    /** Registered after ETJ's own LOWEST handlers, so this sees what they left. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        deathPassed = !event.isCanceled();
        IChatComponent message = event.entityLiving.func_110142_aN()
            .func_151521_b();
        deathKey = message instanceof ChatComponentTranslation ? ((ChatComponentTranslation) message).getKey()
            : message.getUnformattedText();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurtFirst(LivingHurtEvent event) {
        if (!cancelHurtFirst) return;
        event.ammount = 0;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (started || event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        if (event.player.ticksExisted < JOIN_TICKS || HOLDS.get() > 0) return;
        started = true;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        try {
            runAll(player);
        } catch (RuntimeException e) {
            ModLog.LOG.error("[damagecheck] crashed", e);
            record("FAIL crash: " + e);
        }
        write();
        done = true;
    }

    private void runAll(EntityPlayerMP player) {
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        Element fire = ElementRegistry.get("fire"), chaos = ElementRegistry.get("chaos");
        if (fire == null || chaos == null) {
            record("FAIL elements: fire or chaos is not registered");
            return;
        }
        ElementalDamage sword = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(ElementRegistry.NEUTRAL, 6)
            .add(fire, 4)
            .build();
        reset(player);
        expectLoss("plain neutral 6 + fire 4", player, sword, 10);

        ElementalDamage fire5 = magic(fire, 5), pierce5 = magic(chaos, 5);
        Item[] infinity = items("Avaritia", "Infinity_Helm", "Infinity_Chest", "Infinity_Pants", "Infinity_Shoes");
        Item[] draconic = items("DraconicEvolution", "draconicHelm", "draconicChest", "draconicLeggs", "draconicBoots");
        if (infinity == null) record("SKIP avaritia: Infinity armor not found");
        else {
            wear(player, infinity, false);
            expectLoss("avaritia blocks fire 5", player, fire5, 0);
            wear(player, infinity, false);
            expectLoss("avaritia pierced by chaos 5", player, pierce5, 5);
        }
        if (draconic == null) record("SKIP draconic: Draconic armor not found");
        else {
            wear(player, draconic, true);
            float lost = hit(player, fire5, null);
            if (Math.abs(lost) > EPSILON) record("SKIP draconic: shield did not hold, lost " + lost);
            else {
                record("PASS draconic blocks fire 5");
                wear(player, draconic, true);
                expectLoss("draconic pierced by chaos 5", player, pierce5, 5);
            }
        }

        // A zombie, because on a player the fallback would make up for a wrong amount.
        EntityZombie zombie = new EntityZombie(player.worldObj);
        zombie.setPosition(player.posX + 3, player.posY, player.posZ);
        player.worldObj.spawnEntityInWorld(zombie);
        halveChaos = true;
        cancelHurtFirst = true;
        float zombieBefore = zombie.getHealth();
        ElementalDamages.deal(zombie, new ElementalDamageSource(magic(chaos, 10), null, null));
        float zombieLost = zombieBefore - zombie.getHealth();
        cancelHurtFirst = false;
        halveChaos = false;
        zombie.setDead();
        record(
            (Math.abs(zombieLost - 5) <= EPSILON ? "PASS" : "FAIL") + " resisted after an early cancel: lost "
                + zombieLost
                + ", expected 5.0");

        String client = DamageCheck.clientResult;
        record(client == null ? "FAIL client side: never ran" : client);

        reset(player);
        MinecraftServer server = MinecraftServer.getServer();
        boolean pvp = server.isPVPEnabled();
        server.setAllowPvp(false);
        EntityPlayer other = FakePlayerFactory.getMinecraft((WorldServer) player.worldObj);
        expectLoss("pvp off blocks a player", player, magic(fire, 5), 0, other);
        expectLoss("pvp off blocks a piercing player", player, magic(chaos, 5), 0, other);
        server.setAllowPvp(pvp);

        if (draconic == null) record("SKIP draconic death: Draconic armor not found");
        else {
            wear(player, draconic, true);
            expectDeath("draconic killed by chaos 1000", player, chaos);
            player.setHealth(player.getMaxHealth());
        }

        reset(player);
        player.setGameType(WorldSettings.GameType.CREATIVE);
        expectLoss("creative not pierced", player, pierce5, 0);
        player.setGameType(WorldSettings.GameType.SURVIVAL);

        checkGear(player, fire, chaos, pierce5);

        if (infinity == null) record("SKIP avaritia death: Infinity armor not found");
        else {
            wear(player, infinity, false);
            expectDeath("avaritia killed by chaos 1000", player, chaos);
        }
    }

    /** ETJ's death immunity on the order set, the fire set's resistance and the fire sword; ends dead and refilled. */
    private void checkGear(EntityPlayerMP player, Element fire, Element chaos, ElementalDamage pierce5) {
        Item[] order = items(ModInfo.MODID, "order_helmet", "order_chestplate", "order_leggings", "order_boots");
        Item[] fireSet = items(ModInfo.MODID, "fire_helmet", "fire_chestplate", "fire_leggings", "fire_boots");
        Item fireSword = GameRegistry.findItem(ModInfo.MODID, "fire_sword");
        if (order == null || fireSet == null || fireSword == null) {
            record("FAIL gear: elemental gear is not registered");
            return;
        }
        wear(player, order, false);
        expectLoss("etj immunity blocks fire 5", player, magic(fire, 5), 0);
        wear(player, order, false);
        record(vanilla(player, 5) == 0 ? "PASS etj immunity blocks generic 5" : "FAIL etj immunity let generic 5 in");
        wear(player, order, false);
        expectLoss("etj immunity pierced by chaos 5", player, pierce5, 5);
        ElementalDamage mixed = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(chaos, 5)
            .add(ElementRegistry.NEUTRAL, 3)
            .build();
        wear(player, order, false);
        expectLoss("etj immunity lets only the chaos part of chaos 5 + neutral 3 in", player, mixed, 5);
        wear(player, order, false);
        player.setCurrentItemOrArmor(1, null);
        float lost = vanilla(player, 5);
        record((lost > 0 ? "PASS" : "FAIL") + " three order pieces are not immune: lost " + lost);

        wear(player, fireSet, false);
        // 0.85 per piece and 0.5 off per piece, four pieces.
        expectLoss("fire set resists fire 10", player, magic(fire, 10), (float) (10 * Math.pow(0.85, 4) - 2));

        wear(player, fireSet, false);
        // Vanilla armor 3 + 7 + 5 + 2 cuts the neutral 3 alone; chaos takes no armor.
        expectLoss(
            "vanilla armor cuts only the neutral of chaos 5 + neutral 3",
            player,
            mixed,
            5 + 3f * (25 - 17) / 25);

        reset(player);
        EntityZombie zombie = new EntityZombie(player.worldObj);
        zombie.setPosition(player.posX + 3, player.posY, player.posZ);
        player.worldObj.spawnEntityInWorld(zombie);
        float before = zombie.getHealth();
        float expected = 8f * (25 - zombie.getTotalArmorValue()) / 25;
        new ItemStack(fireSword).getItem()
            .onLeftClickEntity(new ItemStack(fireSword), player, zombie);
        float zombieLost = before - zombie.getHealth();
        zombie.setDead();
        record(
            (Math.abs(zombieLost - expected) <= EPSILON ? "PASS" : "FAIL") + " fire sword hits a zombie: lost "
                + zombieLost
                + ", expected "
                + expected);

        wear(player, order, false);
        expectDeath("etj immunity killed by chaos 1000", player, chaos);
        player.setHealth(player.getMaxHealth());
    }

    /** Health plus absorption lost to plain vanilla damage. */
    private static float vanilla(EntityPlayerMP player, float amount) {
        player.hurtResistantTime = 0;
        float before = player.getHealth() + player.getAbsorptionAmount();
        player.attackEntityFrom(DamageSource.generic, amount);
        return before - (player.getHealth() + player.getAbsorptionAmount());
    }

    /** Kills with piercing chaos 1000; the death event must pass and name the chaos death message. */
    private void expectDeath(String name, EntityPlayerMP player, Element chaos) {
        deathPassed = null;
        deathKey = null;
        hit(player, magic(chaos, 1000), null);
        boolean dead = player.getHealth() <= 0;
        boolean passed = Boolean.TRUE.equals(deathPassed);
        boolean named = "death.attack.etjourney.chaos".equals(deathKey);
        record(
            (dead && passed && named ? "PASS " : "FAIL ") + name
                + ": health "
                + player.getHealth()
                + ", death event "
                + (deathPassed == null ? "never fired" : passed ? "passed" : "cancelled")
                + ", message "
                + deathKey);
    }

    private static ElementalDamage magic(Element element, float amount) {
        return ElementalDamage.builder(DamageKind.MAGIC)
            .add(element, amount)
            .build();
    }

    private void expectLoss(String name, EntityPlayerMP player, ElementalDamage damage, float expected) {
        expectLoss(name, player, damage, expected, null);
    }

    private void expectLoss(String name, EntityPlayerMP player, ElementalDamage damage, float expected,
        EntityPlayer attacker) {
        float lost = hit(player, damage, attacker);
        boolean ok = Math.abs(lost - expected) <= EPSILON;
        record((ok ? "PASS " : "FAIL ") + name + ": lost " + lost + ", expected " + expected);
    }

    /** Health plus absorption lost to one hit, after the hurt cooldown is cleared. */
    private static float hit(EntityPlayerMP player, ElementalDamage damage, EntityPlayer attacker) {
        player.hurtResistantTime = 0;
        float before = player.getHealth() + player.getAbsorptionAmount();
        ElementalDamages.deal(player, new ElementalDamageSource(damage, attacker, attacker));
        return before - (player.getHealth() + player.getAbsorptionAmount());
    }

    private static void reset(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.armorInventory.length; i++) player.inventory.armorInventory[i] = null;
        player.clearActivePotions();
        player.setAbsorptionAmount(0);
        player.setHealth(player.getMaxHealth());
        player.hurtResistantTime = 0;
    }

    /** Puts on helm, chest, legs and boots in that order; fills DE shield points when asked. */
    private static void wear(EntityPlayerMP player, Item[] armor, boolean shield) {
        reset(player);
        for (int i = 0; i < 4; i++) {
            ItemStack stack = new ItemStack(armor[i]);
            if (shield) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setFloat("ProtectionPoints", SHIELD_POINTS);
                stack.setTagCompound(tag);
            }
            player.setCurrentItemOrArmor(4 - i, stack);
        }
    }

    /** The four items of one mod, or null when any is missing. */
    private static Item[] items(String modid, String... names) {
        Item[] out = new Item[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = GameRegistry.findItem(modid, names[i]);
            if (out[i] == null) return null;
        }
        return out;
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[damagecheck] {}", line);
    }

    private void write() {
        File file = MinecraftServer.getServer()
            .getFile("damagecheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[damagecheck] cannot write {}", file, e);
        }
    }
}
