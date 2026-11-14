package chlorine.etjourney.content.area.check;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;

import chlorine.etjourney.content.area.EntityLingeringArea;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client half of the area check run by -Detj.areaCheck: opens a fresh world, watches areas, quits. */
public final class AreaCheck {

    private static final String SAVE = "etj-areacheck";
    private static final long SEED = 20261114L;
    /** The default flat layers without villages, so nothing stands near the spawn. */
    private static final String FLAT = "2;7,2x3,2;1;";
    private static final long BUDGET_MS = 10 * 60_000L;
    private static final int LINGER_TICKS = 40;

    private final long started = System.currentTimeMillis();
    private boolean launched;
    private int lingered;

    private AreaCheck() {}

    public static void startIfRequested() {
        if (!Boolean.getBoolean("etj.areaCheck")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new AreaCheck());
        AreaCheckServer.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (System.currentTimeMillis() - started > BUDGET_MS) {
            ModLog.LOG.error("[areacheck] FAIL budget of {} ms spent", BUDGET_MS);
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
            settings.func_82750_a(FLAT);
            mc.launchIntegratedServer(SAVE, SAVE, settings);
            launched = true;
            return;
        }
        if (mc.theWorld != null && AreaCheckServer.clientSaw == null) watch(mc);
        if (AreaCheckServer.done && ++lingered >= LINGER_TICKS) {
            ModLog.LOG.info("[areacheck] finished");
            mc.shutdown();
        }
    }

    /** The client must learn each area's kind and radius, or it can scatter no particles. */
    private static void watch(Minecraft mc) {
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLingeringArea)) continue;
            EntityLingeringArea area = (EntityLingeringArea) o;
            if (area.spec() != null && area.radius() > 0)
                AreaCheckServer.clientSaw = area.spec().name + " r=" + area.radius();
            return;
        }
    }
}
