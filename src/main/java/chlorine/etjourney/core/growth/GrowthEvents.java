package chlorine.etjourney.core.growth;

import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.growth.model.GearSlot;
import chlorine.etjourney.core.growth.model.GrowthBonus;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/** Rolls gear when crafted or picked up, and gives players' gear experience from hits. */
public final class GrowthEvents {

    private GrowthEvents() {}

    public static void register() {
        GrowthEvents events = new GrowthEvents();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.player.worldObj.isRemote) Growth.ensureRolled(event.crafting, event.player.getRNG());
    }

    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (!event.entityPlayer.worldObj.isRemote)
            Growth.ensureRolled(event.item.getEntityItem(), event.entityPlayer.getRNG());
    }

    /** After the combat hooks, so a blocked hit gives nothing. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving.worldObj.isRemote || event.ammount <= 0) return;
        int xp = GrowthBonus.xpForHit(event.ammount);
        if (event.source.getEntity() instanceof EntityPlayer && event.source.getEntity() != event.entityLiving) {
            EntityPlayer attacker = (EntityPlayer) event.source.getEntity();
            ItemStack held = attacker.getHeldItem();
            if (Gear.slot(held) == GearSlot.WEAPON) gain(attacker, held, xp);
            gainAccessories(attacker, xp);
        }
        if (event.entityLiving instanceof EntityPlayer) {
            EntityPlayer target = (EntityPlayer) event.entityLiving;
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
