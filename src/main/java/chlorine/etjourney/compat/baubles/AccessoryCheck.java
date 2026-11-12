package chlorine.etjourney.compat.baubles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import chlorine.etjourney.content.accessory.Talisman;
import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.ResistanceSources;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.growth.Growth;
import chlorine.etjourney.core.growth.GrowthNbt;
import chlorine.etjourney.core.growth.model.AffixRoll;
import chlorine.etjourney.core.growth.model.GrowthData;
import chlorine.etjourney.core.growth.model.Leveling;
import chlorine.etjourney.core.growth.model.Rarity;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.debug.damagecheck.DamageCheckServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * In-game check of the talisman slots, run with -PdamageCheck before its hits, which it holds back until done.
 * Writes accessorycheck.txt next to damagecheck.txt.
 */
public final class AccessoryCheck {

    private static final double EPSILON = 1e-6;
    /** World ticks after joining before the first stage. */
    private static final int JOIN_TICKS = 40;
    /** Long enough for a modifier sync (10 ticks) and a wear (20 ticks) after a change. */
    private static final int SETTLE = 25;

    private final List<String> lines = new ArrayList<>();
    private long start = -1;
    private int stage;
    private boolean finished;
    private int[] slots;

    static void register() {
        DamageCheckServer.hold();
        FMLCommonHandler.instance()
            .bus()
            .register(new AccessoryCheck());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        long now = event.player.worldObj.getTotalWorldTime();
        if (start < 0) start = now + JOIN_TICKS;
        if (now < start + (long) stage * SETTLE) return;
        try {
            if (step(event.player, BaublesApi.getBaubles(event.player))) stage++;
            else finish();
        } catch (RuntimeException e) {
            ModLog.LOG.error("[accessorycheck] crashed", e);
            record("FAIL crash: " + e);
            finish();
        }
    }

    /** False once there is nothing more to do. */
    private boolean step(EntityPlayer player, IInventory inv) {
        int lifetime = Talisman.RHENIUM.lifetime;
        switch (stage) {
            case 0:
                slots = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(BaublesCompat.TALISMAN_TYPE);
                expect("two talisman slots", slots.length == 2, slots.length + " slots");
                if (slots.length < 2) return false;
                int ring = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(BaubleExpandedSlots.ringType)[0];
                expect(
                    "a talisman does not fit a ring slot",
                    !inv.isItemValidForSlot(ring, stack(Talisman.SAKURA_GOLD, 0)),
                    "it fit");
                expect(
                    "a talisman fits a talisman slot",
                    inv.isItemValidForSlot(slots[0], stack(Talisman.SAKURA_GOLD, 0)),
                    "it did not fit");
                inv.setInventorySlotContents(slots[0], stack(Talisman.SAKURA_GOLD, 0));
                inv.setInventorySlotContents(slots[1], stack(Talisman.GOLDEN_PALLADIUM, 0));
                return true;
            case 1:
                attribute("sakura gold adds 1 attack", player, SharedMonsterAttributes.attackDamage, 1 + 1);
                attribute("golden palladium adds 2 health", player, SharedMonsterAttributes.maxHealth, 20 + 2);
                float flat = ResistanceSources.of(player)
                    .flat(ElementRegistry.NEUTRAL);
                expect("golden palladium takes 0.5 off neutral", Math.abs(flat - 0.5f) < EPSILON, "flat " + flat);
                // Two Rhenium: the first nearly spent, so it breaks and the second takes over.
                inv.setInventorySlotContents(slots[0], stack(Talisman.RHENIUM, lifetime - 3));
                inv.setInventorySlotContents(slots[1], stack(Talisman.RHENIUM, 0));
                return true;
            case 2:
                attribute("two rhenium count once for attack", player, SharedMonsterAttributes.attackDamage, 1 + 2);
                attribute("two rhenium count once for health", player, SharedMonsterAttributes.maxHealth, 20 + 4);
                attribute("rhenium adds 10% speed", player, SharedMonsterAttributes.movementSpeed, 0.1 * 1.1);
                int first = damage(inv, slots[0]), second = damage(inv, slots[1]);
                expect(
                    "only the counted rhenium wore, about once a second",
                    (first == lifetime - 2 || first == lifetime - 1) && second == 0,
                    first + " and " + second);
                return true;
            case 3:
            case 4:
                // Wait out the rest of the first Rhenium's life.
                return true;
            case 5:
                expect("the spent rhenium broke", inv.getStackInSlot(slots[0]) == null, "still there");
                attribute("the second rhenium took over", player, SharedMonsterAttributes.attackDamage, 1 + 2);
                inv.setInventorySlotContents(slots[0], null);
                inv.setInventorySlotContents(slots[1], stack(Talisman.DRIFT, 0));
                return true;
            case 6:
                attribute("drift adds 5% speed", player, SharedMonsterAttributes.movementSpeed, 0.1 * 1.05);
                attribute("taking rhenium off removes its health", player, SharedMonsterAttributes.maxHealth, 20);
                attribute("taking rhenium off removes its attack", player, SharedMonsterAttributes.attackDamage, 1);
                expect(
                    "health fits the lowered maximum",
                    player.getHealth() <= player.getMaxHealth(),
                    player.getHealth() + " > " + player.getMaxHealth());
                inv.setInventorySlotContents(slots[1], null);
                return true;
            case 7:
                attribute("empty slots leave speed alone", player, SharedMonsterAttributes.movementSpeed, 0.1);
                inv.setInventorySlotContents(slots[0], stack(Talisman.EMBER, 0));
                inv.setInventorySlotContents(slots[1], stack(Talisman.VAMPIRE, 0));
                return true;
            case 8:
                checkEmberAndVampire(player);
                inv.setInventorySlotContents(slots[0], stack(Talisman.WRATH, 0));
                inv.setInventorySlotContents(slots[1], stack(Talisman.BUBBLE, 0));
                return true;
            case 9:
                checkWrathAndBubble(player);
                inv.setInventorySlotContents(slots[0], null);
                inv.setInventorySlotContents(slots[1], null);
                return true;
            case 10:
                checkWeaponGrowth(player);
                ItemStack grown = stack(Talisman.SAKURA_GOLD, 0);
                GrowthNbt.write(grown, data(Leveling.totalFor(Leveling.MAX_LEVEL), Rarity.UNCOMMON, "fierce", 0));
                inv.setInventorySlotContents(slots[0], grown);
                return true;
            case 11:
                checkAccessoryGrowth(player, inv);
                inv.setInventorySlotContents(slots[0], null);
                return false;
            default:
                return false;
        }
    }

    /** The fire sword's physical fire 5 + neutral 3 under Ember, and the share Vampire heals of it. */
    private void checkEmberAndVampire(EntityPlayer player) {
        Item sword = GameRegistry.findItem(ModInfo.MODID, "fire_sword");
        EntityZombie zombie = zombie(player);
        player.setHealth(10);
        float before = zombie.getHealth();
        // Fire 5 x (1 + 0.3 + 0.2) + neutral 3, before the zombie's vanilla armor.
        float dealt = 5 * 1.5f + 3;
        float expected = dealt * (25 - zombie.getTotalArmorValue()) / 25;
        new ItemStack(sword).getItem()
            .onLeftClickEntity(new ItemStack(sword), player, zombie);
        float lost = before - zombie.getHealth();
        zombie.setDead();
        close("ember raises physical fire by 50%", lost, expected);
        close("vampire heals 15% of the melee hit", player.getHealth() - 10, dealt * 0.15f);
        player.setHealth(player.getMaxHealth());
    }

    /** Wrath only below half health; Bubble blocks one creature hit but not plain damage, then breaks. */
    private void checkWrathAndBubble(EntityPlayer player) {
        EntityZombie zombie = zombie(player);
        float armor = (25 - zombie.getTotalArmorValue()) / 25f;
        player.setHealth(player.getMaxHealth());
        close("wrath sleeps at full health", punch(player, zombie), 4 * armor);
        player.setHealth(player.getMaxHealth() / 2);
        close("wrath adds 40% at half health", punch(player, zombie), 4 * 1.4f * armor);
        zombie.setDead();
        player.setHealth(player.getMaxHealth());
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        close("bubble lets plain damage through", hurt(player, DamageSource.generic, 2), 2);
        close("bubble blocks a creature's hit", hurt(player, DamageSource.causeMobDamage(zombie), 4), 0);
        close("the popped bubble is down", hurt(player, DamageSource.causeMobDamage(zombie), 4), 4);
        player.setHealth(player.getMaxHealth());
    }

    /** A fresh sword rolls and gains from a hit; levels and affixes raise its damage; reforge redraws. */
    private void checkWeaponGrowth(EntityPlayer player) {
        EntityZombie zombie = zombie(player);
        float armor = (25 - zombie.getTotalArmorValue()) / 25f;
        ItemStack sword = new ItemStack(Items.diamond_sword);
        player.inventory.mainInventory[player.inventory.currentItem] = sword;
        punch(player, zombie);
        GrowthData rolled = GrowthNbt.read(player.getHeldItem());
        expect("a fresh sword rolls on its first hit", rolled != null, "no growth");
        expect("the sword gains the hit as experience", rolled != null && rolled.xp == 4, String.valueOf(rolled));
        GrowthNbt.write(sword, data(Leveling.totalFor(Leveling.MAX_LEVEL), Rarity.COMMON, null, 0));
        close("a level 10 sword adds 20%", punch(player, zombie), 4 * 1.2f * armor);
        GrowthNbt.write(sword, data(0, Rarity.LEGENDARY, "keen", 1));
        close("a keen sword adds 15%", punch(player, zombie), 4 * 1.15f * armor);
        Growth.reforge(sword, player.getRNG());
        GrowthData reforged = GrowthNbt.read(sword);
        expect(
            "a reforged legendary sword keeps its rarity and draws four affixes",
            reforged.rarity == Rarity.LEGENDARY && reforged.affixes.size() == 4,
            reforged.rarity + " with " + reforged.affixes.size());
        player.inventory.mainInventory[player.inventory.currentItem] = null;
        zombie.setDead();

        ItemStack plate = new ItemStack(Items.iron_chestplate);
        GrowthNbt.write(plate, data(Leveling.totalFor(Leveling.MAX_LEVEL), Rarity.UNCOMMON, "sturdy", 1));
        player.setCurrentItemOrArmor(3, plate);
        player.setHealth(player.getMaxHealth());
        // Level 10 takes 5% off, sturdy 6%, then the iron chestplate's 6 armor points.
        float expected = 10 * 0.95f * 0.94f * (25 - 6) / 25f;
        close("level and sturdy armor cut a hit", hurt(player, DamageSource.causeMobDamage(zombie), 10), expected);
        player.setCurrentItemOrArmor(3, null);
        player.setHealth(player.getMaxHealth());
    }

    /** A level 10 fierce talisman adds 10% + 5% to physical hits and gains from them. */
    private void checkAccessoryGrowth(EntityPlayer player, IInventory inv) {
        EntityZombie zombie = zombie(player);
        float armor = (25 - zombie.getTotalArmorValue()) / 25f;
        close("a grown fierce talisman adds 15%", punch(player, zombie), 4 * 1.15f * armor);
        zombie.setDead();
        GrowthNbt.write(inv.getStackInSlot(slots[0]), data(0, Rarity.COMMON, null, 0));
        zombie = zombie(player);
        punch(player, zombie);
        zombie.setDead();
        GrowthData after = GrowthNbt.read(inv.getStackInSlot(slots[0]));
        expect("a worn talisman gains from hits", after != null && after.xp == 4, String.valueOf(after));
    }

    private static GrowthData data(int xp, Rarity rarity, String affix, float roll) {
        List<AffixRoll> affixes = new ArrayList<>();
        if (affix != null) affixes.add(new AffixRoll(affix, roll));
        return new GrowthData(xp, rarity, affixes);
    }

    private static EntityZombie zombie(EntityPlayer player) {
        EntityZombie zombie = new EntityZombie(player.worldObj);
        zombie.setPosition(player.posX + 3, player.posY, player.posZ);
        player.worldObj.spawnEntityInWorld(zombie);
        return zombie;
    }

    /** What the zombie loses to a vanilla player hit of 4. */
    private static float punch(EntityPlayer player, EntityZombie zombie) {
        zombie.setHealth(zombie.getMaxHealth());
        zombie.hurtResistantTime = 0;
        float before = zombie.getHealth();
        zombie.attackEntityFrom(DamageSource.causePlayerDamage(player), 4);
        return before - zombie.getHealth();
    }

    private static float hurt(EntityPlayer player, DamageSource source, float amount) {
        player.hurtResistantTime = 0;
        float before = player.getHealth();
        player.attackEntityFrom(source, amount);
        return before - player.getHealth();
    }

    private void close(String name, float value, float expected) {
        expect(name, Math.abs(value - expected) < 0.01f, value + ", expected " + expected);
    }

    private void finish() {
        finished = true;
        write();
        DamageCheckServer.release();
    }

    private static ItemStack stack(Talisman talisman, int used) {
        Item item = GameRegistry.findItem(ModInfo.MODID, talisman.name);
        return new ItemStack(item, 1, used);
    }

    private static int damage(IInventory inv, int slot) {
        ItemStack stack = inv.getStackInSlot(slot);
        return stack == null ? -1 : stack.getItemDamage();
    }

    private void attribute(String name, EntityPlayer player, IAttribute attribute, double expected) {
        double value = player.getEntityAttribute(attribute)
            .getAttributeValue();
        expect(name, Math.abs(value - expected) < EPSILON, value + ", expected " + expected);
    }

    private void expect(String name, boolean ok, String detail) {
        record((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + detail));
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[accessorycheck] {}", line);
    }

    private void write() {
        File file = MinecraftServer.getServer()
            .getFile("accessorycheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[accessorycheck] cannot write {}", file, e);
        }
    }
}
