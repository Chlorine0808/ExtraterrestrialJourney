package chlorine.etjourney.compat.baubles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import chlorine.etjourney.content.accessory.Talisman;
import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.damage.ResistanceSources;
import chlorine.etjourney.core.damage.model.ElementRegistry;
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
                return false;
            default:
                return false;
        }
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
