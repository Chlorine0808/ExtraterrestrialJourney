package chlorine.etjourney.core.growth;

import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.combat.CombatHit;
import chlorine.etjourney.core.combat.LandedHits;
import chlorine.etjourney.core.growth.model.GearSlot;
import chlorine.etjourney.core.growth.model.GrowthBonus;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/** Rolls gear when crafted or picked up, and gives players' gear experience from the health their hits took. */
public final class GrowthEvents {

    private GrowthEvents() {}

    public static void register() {
        GrowthEvents events = new GrowthEvents();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);
        LandedHits.listen(GrowthEvents::onLanded);
    }

    /**
     * The click that crafted skips its own sync, as the client guessed the stack right before it was rolled, so the
     * rolled stack is sent here.
     */
    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || Gear.slot(event.crafting) == null) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (event.crafting == player.inventory.getItemStack()) {
            Growth.ensureRolled(event.crafting, player.getRNG());
            player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-1, -1, event.crafting));
            return;
        }
        // Shift-clicking merged a copy into the inventory before this event, so the copies are rolled instead.
        Container container = player.openContainer;
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            ItemStack stack = slot.getStack();
            if (slot instanceof SlotCrafting || stack == null
                || stack.getItem() != event.crafting.getItem()
                || GrowthNbt.read(stack) != null) continue;
            Growth.ensureRolled(stack, player.getRNG());
            player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(container.windowId, i, stack));
        }
    }

    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (!event.entityPlayer.worldObj.isRemote)
            Growth.ensureRolled(event.item.getEntityItem(), event.entityPlayer.getRNG());
    }

    private static void onLanded(CombatHit hit) {
        int xp = GrowthBonus.xpForHit(hit.amount());
        if (hit.attacker instanceof EntityPlayer && hit.attacker != hit.target) {
            EntityPlayer attacker = (EntityPlayer) hit.attacker;
            ItemStack held = attacker.getHeldItem();
            if (Gear.slot(held) == GearSlot.WEAPON) gain(attacker, held, xp);
            gainAccessories(attacker, xp);
        }
        if (hit.target instanceof EntityPlayer) {
            EntityPlayer target = (EntityPlayer) hit.target;
            for (ItemStack armor : target.inventory.armorInventory)
                if (Gear.slot(armor) != null) gain(target, armor, xp);
            gainAccessories(target, xp);
        }
    }

    /** Accessory slots sync only when set, so each grown accessory is put back as a copy. */
    private static void gainAccessories(EntityPlayer player, int xp) {
        for (ItemStack worn : WornAccessories.of(player)) {
            ItemStack grown = worn.copy();
            gain(player, grown, xp);
            WornAccessories.replace(player, worn, grown);
        }
    }

    private static void gain(EntityLivingBase owner, ItemStack stack, int xp) {
        Random random = owner.getRNG();
        int level = Growth.gainXp(stack, xp, random);
        if (level > 0 && owner instanceof EntityPlayer) ((EntityPlayer) owner)
            .addChatMessage(new ChatComponentTranslation("etjourney.growth.level_up", stack.func_151000_E(), level));
    }
}
