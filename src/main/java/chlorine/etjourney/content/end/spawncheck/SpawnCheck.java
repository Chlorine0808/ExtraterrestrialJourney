package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;

import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client half of the spawn check run by -Detj.spawnCheck: opens a fresh world, waits for the server, quits. */
public final class SpawnCheck {

    private static final String SAVE = "etj-spawncheck";
    private static final long SEED = 20261111L;
    private static final long BUDGET_MS = 25 * 60_000L;
    /** Client ticks to wait after the checks so the log is flushed. */
    private static final int LINGER_TICKS = 40;

    private final long started = System.currentTimeMillis();
    private boolean launched;
    private int lingered;

    private SpawnCheck() {}

    public static void startIfRequested() {
        if (!Boolean.getBoolean("etj.spawnCheck")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new SpawnCheck());
        SpawnCheckServer.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (System.currentTimeMillis() - started > BUDGET_MS) {
            ModLog.LOG.error("[spawncheck] FAIL budget of {} ms spent", BUDGET_MS);
            mc.shutdown();
            return;
        }
        if (!launched) {
            if (!(mc.currentScreen instanceof GuiMainMenu)) return;
            ISaveFormat saves = mc.getSaveLoader();
            saves.flushCache();
            saves.deleteWorldDirectory(SAVE);
            WorldSettings settings = new WorldSettings(
                SEED,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                WorldType.FLAT).enableCommands();
            mc.launchIntegratedServer(SAVE, SAVE, settings);
            launched = true;
            return;
        }
        if (SpawnCheckServer.done && ++lingered >= LINGER_TICKS) {
            ModLog.LOG.info("[spawncheck] finished");
            mc.shutdown();
        }
    }
}
