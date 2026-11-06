package chlorine.etjourney.content;

import chlorine.etjourney.content.end.EndAcid;
import chlorine.etjourney.content.end.EndBlocks;
import chlorine.etjourney.content.end.EndBuildingContent;
import chlorine.etjourney.content.end.EndSpawns;
import chlorine.etjourney.core.util.ModLog;

/** Entry point that registers every region's blocks, items and entities. */
public final class ModContent {

    private ModContent() {}

    public static void preInit() {
        EndBlocks.preInit();
        EndAcid.preInit();
        ModLog.LOG.debug("content: preInit");
    }

    /** After every region has registered its entities in preInit. */
    public static void init() {
        EndSpawns.register();
        EndBuildingContent.init();
        ModLog.LOG.debug("content: init");
    }
}
