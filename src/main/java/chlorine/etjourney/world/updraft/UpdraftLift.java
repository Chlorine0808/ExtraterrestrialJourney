package chlorine.etjourney.world.updraft;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.world.updraft.model.Updraft;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/** Client side: lifts the local player, who owns their own movement, while inside an updraft. */
public final class UpdraftLift {

    private UpdraftLift() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new UpdraftLift());
    }

    /** At the start of the tick, so this tick's move already uses the new speed. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) return;
        EntityPlayer player = event.player;
        if (player != Minecraft.getMinecraft().thePlayer || player.capabilities.isFlying) return;
        Updraft u = UpdraftWorld.around(player, UpdraftView.in(player.dimension));
        if (u == null) return;
        player.motionY = u.push(player.motionY);
        player.fallDistance = 0;
    }
}
