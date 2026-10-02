package chlorine.etjourney.core.network;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** Owns the mod's single packet channel; packets register against nextId(). */
public final class ETJNetwork {

    private static SimpleNetworkWrapper channel;
    private static int nextId = 0;

    private ETJNetwork() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
    }

    public static SimpleNetworkWrapper channel() {
        if (channel == null) throw new IllegalStateException("ETJNetwork.init() has not run yet");
        return channel;
    }

    public static int nextId() {
        return nextId++;
    }
}
