package chlorine.etjourney.world.updraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.AxisAlignedBB;

import chlorine.etjourney.core.network.ETJNetwork;
import chlorine.etjourney.world.updraft.model.Updraft;
import chlorine.etjourney.world.updraft.model.Updrafts;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Server side of the updrafts: holds them, sends each dimension's to its players, and keeps a lifted player from
 * taking fall damage or being kicked for flying. The client moves the player (see UpdraftLift).
 */
public final class UpdraftWorld {

    /** Not saved: sources register their updrafts again when they load. */
    public static final Updrafts SERVER = new Updrafts();

    private UpdraftWorld() {}

    public static void register() {
        ETJNetwork.channel()
            .registerMessage(UpdraftSync.Handler.class, UpdraftSync.class, ETJNetwork.nextId(), Side.CLIENT);
        SERVER.onChange(
            dimension -> ETJNetwork.channel()
                .sendToDimension(sync(dimension), dimension));
        FMLCommonHandler.instance()
            .bus()
            .register(new UpdraftWorld());
    }

    /** Drops the updrafts of a previous server in the same game. */
    public static void serverStarting() {
        SERVER.clearAll();
    }

    /** The strongest updraft holding the player in the list, or null. */
    static Updraft around(EntityPlayer player, Iterable<Updraft> updrafts) {
        AxisAlignedBB b = player.boundingBox;
        return Updraft.strongest(updrafts, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }

    private static UpdraftSync sync(int dimension) {
        return new UpdraftSync(
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
        Updraft u = around(
            player,
            SERVER.in(player.dimension)
                .values());
        if (u == null) return;
        player.fallDistance = 0;
        if (player instanceof EntityPlayerMP) {
            NetHandlerPlayServer net = ((EntityPlayerMP) player).playerNetServerHandler;
            // A dedicated server without allow-flight kicks a player held in the air for 80 ticks.
            if (net != null) ObfuscationReflectionHelper
                .setPrivateValue(NetHandlerPlayServer.class, net, 0, "floatingTickCount", "field_147365_f");
        }
    }
}
