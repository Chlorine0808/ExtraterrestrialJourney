package chlorine.etjourney.world;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;

/** Entry point of the terrain engine: biome IDs in preInit, generators in init. */
public final class ModWorld {

    private ModWorld() {}

    public static void preInit() {
        EndTerrain.register();
        ModLog.LOG.debug("world: preInit");
    }

    public static void init() {
        ModLog.LOG.debug("world: init");
    }
}
