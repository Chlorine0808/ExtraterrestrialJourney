package chlorine.etjourney.content;

import chlorine.etjourney.content.end.EndBlocks;
import chlorine.etjourney.core.util.ModLog;

/** Entry point that registers every region's blocks, items and entities. */
public final class ModContent {

    private ModContent() {}

    public static void preInit() {
        EndBlocks.preInit();
        ModLog.LOG.debug("content: preInit");
    }
}
