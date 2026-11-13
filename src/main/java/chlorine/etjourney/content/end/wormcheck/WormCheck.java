package chlorine.etjourney.content.end.wormcheck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;

import chlorine.etjourney.content.end.EntityFlameWorm;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client half of the worm check run by -Detj.wormCheck: opens a fresh world, compares part IDs, quits. */
public final class WormCheck {

    private static final String SAVE = "etj-wormcheck";
    private static final long SEED = 20261113L;
    private static final long BUDGET_MS = 15 * 60_000L;
    private static final int LINGER_TICKS = 40;

    private final long started = System.currentTimeMillis();
    private boolean launched, compared;
    private int lingered;

    private WormCheck() {}

    public static void startIfRequested() {
        if (!Boolean.getBoolean("etj.wormCheck")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new WormCheck());
        WormCheckServer.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (System.currentTimeMillis() - started > BUDGET_MS) {
            ModLog.LOG.error("[wormcheck] FAIL budget of {} ms spent", BUDGET_MS);
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
        if (!compared && WormCheckServer.partIds != null && mc.theWorld != null) compare(mc);
        if (WormCheckServer.done && ++lingered >= LINGER_TICKS) {
            ModLog.LOG.info("[wormcheck] finished");
            mc.shutdown();
        }
    }

    /** The client must give a worm's parts the server's IDs, or clicks on them hit nothing. */
    private void compare(Minecraft mc) {
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityFlameWorm)) continue;
            Entity[] parts = ((EntityFlameWorm) o).getParts();
            int[] want = WormCheckServer.partIds;
            boolean same = parts.length == want.length;
            for (int i = 0; same && i < parts.length; i++) same = parts[i].getEntityId() == want[i];
            WormCheckServer.clientVerdict(same, parts.length);
            compared = true;
            return;
        }
    }
}
