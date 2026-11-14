package chlorine.etjourney.content.rank;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import chlorine.etjourney.content.rank.ability.FamineAbility;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Players marked by a famine mob do not heal and do not start to eat. */
public final class FamineEvents {

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (event.entityLiving instanceof EntityPlayer
            && !event.entityLiving.worldObj.isRemote
            && FamineAbility.marked((EntityPlayer) event.entityLiving))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEat(PlayerUseItemEvent.Start event) {
        if (!event.entityPlayer.worldObj.isRemote && event.item != null
            && event.item.getItem() instanceof ItemFood
            && FamineAbility.marked(event.entityPlayer)) event.setCanceled(true);
    }
}
