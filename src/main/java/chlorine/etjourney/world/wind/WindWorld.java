package chlorine.etjourney.world.wind;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.AxisAlignedBB;

import chlorine.etjourney.core.network.ETJNetwork;
import chlorine.etjourney.world.wind.model.Wind;
import chlorine.etjourney.world.wind.model.Winds;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Server side of the winds: holds them, sends each dimension's to its players, and keeps a player held in one from
 * being kicked for flying, or from taking fall damage when the air rises. The client moves the player (see
 * WindPush).
 */
public final class WindWorld {

    /** Not saved: sources register their winds again when they load. */
    public static final Winds SERVER = new Winds();

    private WindWorld() {}

    public static void register() {
        ETJNetwork.channel()
            .registerMessage(WindSync.Handler.class, WindSync.class, ETJNetwork.nextId(), Side.CLIENT);
        SERVER.onChange(
            dimension -> ETJNetwork.channel()
                .sendToDimension(sync(dimension), dimension));
        FMLCommonHandler.instance()
            .bus()
            .register(new WindWorld());
    }

    /** Drops the winds of a previous server in the same game. */
    public static void serverStarting() {
        SERVER.clearAll();
    }

    /** The strongest wind holding the player in the list, or null. */
    static Wind around(EntityPlayer player, Iterable<Wind> winds) {
        AxisAlignedBB b = player.boundingBox;
        return Wind.strongest(winds, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    private static WindSync sync(int dimension) {
        return new WindSync(
            dimension,
            SERVER.in(dimension)
                .values());
    }

    private static void sendTo(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) ETJNetwork.channel()
            .sendTo(sync(player.dimension), (EntityPlayerMP) player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sendTo(event.player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendTo(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendTo(event.player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) return;
        EntityPlayer player = event.player;
        Wind u = around(
            player,
            SERVER.in(player.dimension)
                .values());
        if (u == null) return;
        if (u.breaksFall()) player.fallDistance = 0;
        if (player instanceof EntityPlayerMP) {
            NetHandlerPlayServer net = ((EntityPlayerMP) player).playerNetServerHandler;
            // A dedicated server without allow-flight kicks a player held in the air for 80 ticks.
            if (net != null) ObfuscationReflectionHelper
                .setPrivateValue(NetHandlerPlayServer.class, net, 0, "floatingTickCount", "field_147365_f");
        }
    }
}
