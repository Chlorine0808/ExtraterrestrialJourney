package chlorine.etjourney.world;

import chlorine.etjourney.core.util.ModLog;

/** Entry point of the terrain engine: biome IDs in preInit, generators in init. */
public final class ModWorld {

    private ModWorld() {}

    public static void preInit() {
        ModLog.LOG.debug("world: preInit");
    }

    public static void init() {
        ModLog.LOG.debug("world: init");
    }
}
