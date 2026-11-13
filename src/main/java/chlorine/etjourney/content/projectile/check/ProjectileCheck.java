package chlorine.etjourney.content.projectile.check;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;

import chlorine.etjourney.content.projectile.EntityEtjProjectile;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client half of the projectile check run by -Detj.projectileCheck: opens a fresh world, watches shots, quits. */
public final class ProjectileCheck {

    private static final String SAVE = "etj-projectilecheck";
    private static final long SEED = 20261114L;
    private static final long BUDGET_MS = 10 * 60_000L;
    private static final int LINGER_TICKS = 40;

    private final long started = System.currentTimeMillis();
    private boolean launched;
    private int lingered;

    private ProjectileCheck() {}

    public static void startIfRequested() {
        if (!Boolean.getBoolean("etj.projectileCheck")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new ProjectileCheck());
        ProjectileCheckServer.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (System.currentTimeMillis() - started > BUDGET_MS) {
            ModLog.LOG.error("[projectilecheck] FAIL budget of {} ms spent", BUDGET_MS);
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
        if (mc.theWorld != null && ProjectileCheckServer.clientSaw == null) watch(mc);
        if (ProjectileCheckServer.done && ++lingered >= LINGER_TICKS) {
            ModLog.LOG.info("[projectilecheck] finished");
            mc.shutdown();
        }
    }

    /** The client must learn each shot's kind from its spawn data, or it can neither draw nor move it. */
    private static void watch(Minecraft mc) {
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityEtjProjectile)) continue;
            EntityEtjProjectile shot = (EntityEtjProjectile) o;
            if (shot.spec() != null) ProjectileCheckServer.clientSaw = shot.spec().name;
            return;
        }
    }
}
