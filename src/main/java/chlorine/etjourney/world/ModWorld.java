package chlorine.etjourney.world;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.VoidFalls;
import chlorine.etjourney.world.end.biosphere.BiosphereTransplant;
import chlorine.etjourney.world.end.debug.EndProbe;
import chlorine.etjourney.world.end.debug.EtjCommand;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Entry point of the terrain engine: biome IDs in preInit, generators in init. */
public final class ModWorld {

    private ModWorld() {}

    public static void preInit() {
        EndTerrain.register();
        ModLog.LOG.debug("world: preInit");
    }

    public static void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new EtjCommand());
        EndProbe.runIfRequested();
    }

    public static void init() {
        VoidFalls.register();
        BiosphereTransplant.register();
        ModLog.LOG.debug("world: init");
    }
}
