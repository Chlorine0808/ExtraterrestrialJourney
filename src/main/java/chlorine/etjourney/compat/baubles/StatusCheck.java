package chlorine.etjourney.compat.baubles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeHooks;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import chlorine.etjourney.content.accessory.Talisman;
import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.status.Ailments;
import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.status.StatusEffects;
import chlorine.etjourney.core.status.StatusSources;
import chlorine.etjourney.core.status.model.StatusMath;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.debug.damagecheck.DamageCheckServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * In-game check of status effects, run with -PdamageCheck after the accessory check and before the hits. Writes
 * statuscheck.txt next to damagecheck.txt.
 */
final class StatusCheck {

    private static final double EPSILON = 1e-6;
    /** Longer than the short effects, so they run out between the two stages. */
    private static final int SETTLE = 30;
    private static final int SHORT = 20;
    private static final ItemStack MILK = new ItemStack(Items.milk_bucket);

    /** Makes a registered source add 50% to the duration of every harmful effect a zombie gives. */
    private static volatile boolean zombiesLinger;

    private final List<String> lines = new ArrayList<>();
    private long next = -1;
    private int stage;
    private boolean finished;
    private EntityPig marked;

    static void register() {
        DamageCheckServer.hold();
        StatusSources.registerBoost(
            (giver, out) -> {
                if (zombiesLinger && giver instanceof EntityZombie) out.addDuration(StatusMath.ANY, 0.5f);
            });
        FMLCommonHandler.instance()
            .bus()
            .register(new StatusCheck());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (finished || !AccessoryCheck.done || event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote)
            return;
        long now = event.player.worldObj.getTotalWorldTime();
        if (next < 0) next = now;
        if (now < next) return;
        next = now + SETTLE;
        try {
            if (Ailments.heavy == null || Ailments.frozen == null) {
                record("FAIL setup: heavy or frozen has no potion ID");
                finish();
            } else if (stage++ == 0) first(event.player);
            else {
                second(event.player);
                finish();
            }
        } catch (RuntimeException e) {
            ModLog.LOG.error("[statuscheck] crashed", e);
            record("FAIL crash: " + e);
            finish();
        }
    }

    private void first(EntityPlayer player) {
        player.clearActivePotions();
        sticksThroughMilk(player);
        bites(player);
        survivesReload(player);
        resisted(player);
        fromIce(player);
        // Both run out before the second stage.
        StatusEffects.apply(player, effect(Ailments.heavy, SHORT, 0), null, true);
        StatusEffects.apply(player, effect(Potion.digSlowdown, SHORT, 0), null, false);
        marked = pig(player);
        StatusEffects.apply(marked, effect(Ailments.heavy, SHORT, 0), null, true);
    }

    private void second(EntityPlayer player) {
        expect("a stubborn effect still runs out", !player.isPotionActive(Ailments.heavy), "heavy is still on");
        expect("a plain effect runs out", !player.isPotionActive(Potion.digSlowdown), "mining fatigue is still on");
        // The pig's stubborn Heavy ended; a plain Heavy after it must not turn stubborn on a reload.
        expect("the pig's short heavy ran out", !marked.isPotionActive(Ailments.heavy), "still on");
        marked.addPotionEffect(effect(Ailments.heavy, 400, 0));
        EntityLivingBase copy = reload(marked);
        copy.curePotionEffects(MILK);
        expect("an ended mark leaves a later plain effect to milk", !copy.isPotionActive(Ailments.heavy), "kept");
        copy.setDead();
        player.clearActivePotions();
    }

    /** Stubborn Heavy stays through milk; a plain poison beside it goes. */
    private void sticksThroughMilk(EntityPlayer player) {
        boolean landed = StatusEffects.apply(player, effect(Ailments.heavy, 200, 1), null, true);
        PotionEffect heavy = player.getActivePotionEffect(Ailments.heavy);
        expect(
            "heavy II lands for 200 ticks",
            landed && heavy != null && heavy.getAmplifier() == 1 && heavy.getDuration() == 200,
            String.valueOf(heavy));
        expect("it is stubborn", StatusEffects.isStubborn(player, Ailments.heavy), "milk cures it");
        StatusEffects.apply(player, effect(Potion.poison, 200, 0), null, false);
        player.curePotionEffects(MILK);
        expect("milk leaves stubborn heavy", player.isPotionActive(Ailments.heavy), "heavy is gone");
        expect("milk takes plain poison", !player.isPotionActive(Potion.poison), "poison is still on");
        player.clearActivePotions();
    }

    /** Frozen stops walking; Heavy II halves a jump and Frozen stops it. */
    private void bites(EntityPlayer player) {
        StatusEffects.apply(player, effect(Ailments.frozen, 200, 0), null, false);
        double speed = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
            .getAttributeValue();
        expect("frozen stops walking", Math.abs(speed) < EPSILON, "speed " + speed);
        player.clearActivePotions();
        EntityPig pig = pig(player);
        StatusEffects.apply(pig, effect(Ailments.heavy, 200, 1), null, false);
        close("heavy II halves a jump", jump(pig), 0.42 * 0.5);
        StatusEffects.apply(pig, effect(Ailments.frozen, 200, 0), null, false);
        close("frozen stops a jump", jump(pig), 0);
        pig.setDead();
    }

    /** A stubborn effect saved and loaded again is stubborn once it joins the world. */
    private void survivesReload(EntityPlayer player) {
        EntityPig pig = pig(player);
        StatusEffects.apply(pig, effect(Potion.poison, 400, 0), null, true);
        EntityLivingBase copy = reload(pig);
        copy.curePotionEffects(MILK);
        expect("stubborn poison stays through a reload and milk", copy.isPotionActive(Potion.poison), "milk cured it");
        copy.setDead();
    }

    /** Ward halves and weakens harmful effects only; a zombie's linger bonus adds before the ward halves. */
    private void resisted(EntityPlayer player) {
        int[] slots = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(BaublesCompat.TALISMAN_TYPE);
        IInventory inv = BaublesApi.getBaubles(player);
        inv.setInventorySlotContents(slots[0], new ItemStack(GameRegistry.findItem(ModInfo.MODID, Talisman.WARD.name)));
        StatusEffects.apply(player, effect(Potion.poison, 200, 1), null, false);
        landed("ward makes poison II for 200 into poison I for 100", player, Potion.poison, 0, 100);
        StatusEffects.apply(player, effect(Potion.moveSpeed, 200, 1), null, false);
        landed("ward leaves speed alone", player, Potion.moveSpeed, 1, 200);
        player.clearActivePotions();
        EntityZombie zombie = new EntityZombie(player.worldObj);
        zombiesLinger = true;
        StatusEffects.apply(player, effect(Potion.poison, 200, 1), zombie, false);
        zombiesLinger = false;
        landed("a giver's +50% adds before the ward halves", player, Potion.poison, 0, 150);
        inv.setInventorySlotContents(slots[0], null);
        player.clearActivePotions();
    }

    /** The element effect slot freezes what an ice hit lands on. */
    private void fromIce(EntityPlayer player) {
        EntityPig pig = pig(player);
        ElementalDamage ice = ElementalDamage.builder(DamageKind.MAGIC)
            .add(Elements.ICE, 2)
            .build();
        ElementalDamages.deal(pig, new ElementalDamageSource(ice, player, player));
        expect("an ice hit freezes", pig.isPotionActive(Ailments.frozen), "not frozen");
        pig.setDead();
    }

    private static PotionEffect effect(Potion potion, int ticks, int amplifier) {
        return new PotionEffect(potion.id, ticks, amplifier);
    }

    private static EntityPig pig(EntityPlayer player) {
        EntityPig pig = new EntityPig(player.worldObj);
        pig.setPosition(player.posX + 3, player.posY, player.posZ);
        player.worldObj.spawnEntityInWorld(pig);
        return pig;
    }

    /** Saves the creature, removes it, and brings it back from the save as a new entity. */
    private static EntityLivingBase reload(EntityLiving living) {
        NBTTagCompound tag = new NBTTagCompound();
        living.writeToNBTOptional(tag);
        living.setDead();
        Entity copy = EntityList.createEntityFromNBT(tag, living.worldObj);
        living.worldObj.spawnEntityInWorld(copy);
        return (EntityLivingBase) copy;
    }

    /** The upward speed a jump leaves, past ETJ's jump handler. */
    private static double jump(EntityLivingBase living) {
        living.motionY = 0.42;
        ForgeHooks.onLivingJump(living);
        return living.motionY;
    }

    private void landed(String name, EntityPlayer player, Potion potion, int amplifier, int ticks) {
        PotionEffect e = player.getActivePotionEffect(potion);
        expect(name, e != null && e.getAmplifier() == amplifier && e.getDuration() == ticks, String.valueOf(e));
    }

    private void close(String name, double value, double expected) {
        expect(name, Math.abs(value - expected) < 1e-3, value + ", expected " + expected);
    }

    private void expect(String name, boolean ok, String detail) {
        record((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + detail));
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[statuscheck] {}", line);
    }

    private void finish() {
        finished = true;
        File file = MinecraftServer.getServer()
            .getFile("statuscheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[statuscheck] cannot write {}", file, e);
        }
        DamageCheckServer.release();
    }
}
