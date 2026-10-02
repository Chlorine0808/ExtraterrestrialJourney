package chlorine.etjourney.content;

import chlorine.etjourney.core.util.ModLog;

/** Entry point that registers every region's blocks, items and entities. */
public final class ModContent {

    private ModContent() {}

    public static void preInit() {
        ModLog.LOG.debug("content: preInit");
    }
}
