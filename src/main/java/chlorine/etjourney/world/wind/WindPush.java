package chlorine.etjourney.world.wind;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.world.wind.model.Wind;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/** Client side: pushes the local player, who owns their own movement, while inside a wind. */
public final class WindPush {

    private WindPush() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new WindPush());
    }

    /** At the start of the tick, so this tick's move already uses the new speed. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) return;
        EntityPlayer player = event.player;
        if (player != Minecraft.getMinecraft().thePlayer || player.capabilities.isFlying) return;
        Wind u = WindWorld.around(player, WindView.in(player.dimension));
        if (u == null) return;
        double[] motion = u.push(player.motionX, player.motionY, player.motionZ);
        player.motionX = motion[0];
        player.motionY = motion[1];
        player.motionZ = motion[2];
        if (u.breaksFall()) player.fallDistance = 0;
    }
}
