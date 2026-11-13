package chlorine.etjourney.content;

import chlorine.etjourney.content.combat.ElementalGear;
import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.craft.CraftContent;
import chlorine.etjourney.content.end.EndAcid;
import chlorine.etjourney.content.end.EndBlocks;
import chlorine.etjourney.content.end.EndBuildingContent;
import chlorine.etjourney.content.end.EndEntities;
import chlorine.etjourney.content.end.EndSpawns;
import chlorine.etjourney.content.growth.GearAffixes;
import chlorine.etjourney.content.rank.EnemyRanks;
import chlorine.etjourney.content.rank.RankCommand;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Entry point that registers every region's blocks, items and entities. */
public final class ModContent {

    private ModContent() {}

    public static void preInit() {
        Elements.register();
        ElementalGear.preInit();
        GearAffixes.register();
        EndBlocks.preInit();
        EndAcid.preInit();
        CraftContent.preInit();
        EndEntities.preInit();
        ModLog.LOG.debug("content: preInit");
    }

    /** After every region has registered its entities in preInit. */
    public static void init() {
        EndSpawns.register();
        EnemyRanks.register();
        EndBuildingContent.init();
        ModLog.LOG.debug("content: init");
    }

    public static void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new RankCommand());
    }
}
