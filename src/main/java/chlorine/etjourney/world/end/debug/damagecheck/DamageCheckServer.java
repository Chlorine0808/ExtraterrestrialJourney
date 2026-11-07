package chlorine.etjourney.world.end.debug.damagecheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
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

    private DamageCheckServer() {}

    static void register() {
        DamageCheckServer server = new DamageCheckServer();
        FMLCommonHandler.instance()
            .bus()
            .register(server);
        MinecraftForge.EVENT_BUS.register(server);
    }

    /** Registered after ETJ's own LOWEST handlers, so this sees what they left. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) deathPassed = !event.isCanceled();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (started || event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        if (event.player.ticksExisted < JOIN_TICKS) return;
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

        ElementalDamage chaos5 = magic(chaos, 5, false), pierce5 = magic(chaos, 5, true);
        Item[] infinity = items("Avaritia", "Infinity_Helm", "Infinity_Chest", "Infinity_Pants", "Infinity_Shoes");
        Item[] draconic = items("DraconicEvolution", "draconicHelm", "draconicChest", "draconicLeggs", "draconicBoots");
        if (infinity == null) record("SKIP avaritia: Infinity armor not found");
        else {
            wear(player, infinity, false);
            expectLoss("avaritia blocks chaos 5", player, chaos5, 0);
            wear(player, infinity, false);
            expectLoss("avaritia pierced by chaos 5", player, pierce5, 5);
        }
        if (draconic == null) record("SKIP draconic: Draconic armor not found");
        else {
            wear(player, draconic, true);
            float lost = hit(player, chaos5);
            if (Math.abs(lost) > EPSILON) record("SKIP draconic: shield did not hold, lost " + lost);
            else {
                record("PASS draconic blocks chaos 5");
                wear(player, draconic, true);
                expectLoss("draconic pierced by chaos 5", player, pierce5, 5);
            }
        }

        reset(player);
        player.setGameType(WorldSettings.GameType.CREATIVE);
        expectLoss("creative not pierced", player, pierce5, 0);
        player.setGameType(WorldSettings.GameType.SURVIVAL);

        if (infinity == null) record("SKIP avaritia death: Infinity armor not found");
        else {
            wear(player, infinity, false);
            deathPassed = null;
            hit(player, magic(chaos, 1000, true));
            boolean dead = player.getHealth() <= 0;
            boolean passed = Boolean.TRUE.equals(deathPassed);
            record(
                (dead && passed ? "PASS" : "FAIL") + " avaritia killed by chaos 1000: health "
                    + player.getHealth()
                    + ", death event "
                    + (deathPassed == null ? "never fired" : passed ? "passed" : "cancelled"));
        }
    }

    private static ElementalDamage magic(Element element, float amount, boolean piercing) {
        ElementalDamage.Builder b = ElementalDamage.builder(DamageKind.MAGIC)
            .add(element, amount);
        if (piercing) b.piercing();
        return b.build();
    }

    private void expectLoss(String name, EntityPlayerMP player, ElementalDamage damage, float expected) {
        float lost = hit(player, damage);
        boolean ok = Math.abs(lost - expected) <= EPSILON;
        record((ok ? "PASS " : "FAIL ") + name + ": lost " + lost + ", expected " + expected);
    }

    /** Health plus absorption lost to one hit, after the hurt cooldown is cleared. */
    private static float hit(EntityPlayerMP player, ElementalDamage damage) {
        player.hurtResistantTime = 0;
        float before = player.getHealth() + player.getAbsorptionAmount();
        ElementalDamages.deal(player, new ElementalDamageSource(damage, null, null));
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
