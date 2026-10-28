package chlorine.etjourney.world.end.debug.shoot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.shoot.PlannedShot;
import chlorine.etjourney.world.end.shoot.Report;
import chlorine.etjourney.world.end.shoot.ShotList;
import chlorine.etjourney.world.end.shoot.ShotPlanner;
import chlorine.etjourney.world.end.shoot.ShotRecord;
import chlorine.etjourney.world.end.shoot.WorldPlan;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Drives the End screenshot run when -Detj.shoot names a shot list; otherwise does nothing at all. */
public final class ShootHarness {

    private static final int WIDTH = 1920, HEIGHT = 1080;
    /** Ticks the player must have lived on the server before being sent on, so the client has joined. */
    private static final int JOIN_TICKS = 20;
    /** Client ticks to wait in the End before reporting, so the new world has been set up and drawn. */
    private static final int ARRIVE_TICKS = 40;
    /** Fog start while shooting, beyond any render distance. */
    private static final float FOG_FAR = 4096;

    private enum Stage {
        MENU,
        JOINING,
        ARRIVED,
        PLANNING
    }

    private final ShotList list;
    private Stage stage = Stage.MENU;
    private int arrivedTicks;
    /** Set on the server thread once the player stands in the End, flying with night vision. */
    private volatile boolean inEnd;
    private boolean sentToEnd;
    private CompletableFuture<List<ShotRecord>> plan;

    private ShootHarness(ShotList list) {
        this.list = list;
    }

    /** Starts the run and registers every event handler, only when etj.shoot is set. */
    public static void startIfRequested() {
        String path = ShotList.listPath(System.getProperty("etj.shoot"));
        if (path == null) return;
        ShotList list;
        try {
            list = ShotList.read(Paths.get(path));
        } catch (IOException e) {
            ModLog.LOG.error("[shoot] cannot read the shot list {}", path, e);
            return;
        }
        for (String error : list.errors) ModLog.LOG.warn("[shoot] {}", error);
        ModLog.LOG.info("[shoot] shot list {}: {} shots", path, list.specs.size());
        ShootHarness harness = new ShootHarness(list);
        FMLCommonHandler.instance()
            .bus()
            .register(harness);
        MinecraftForge.EVENT_BUS.register(harness);
        ShootServer.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        switch (stage) {
            case MENU:
                if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                applySettings(mc.gameSettings);
                ShootCapture.enlarge(mc, WIDTH, HEIGHT);
                launchWorld(mc);
                stage = Stage.JOINING;
                break;
            case JOINING:
                if (!inEnd || mc.theWorld == null || mc.theWorld.provider.dimensionId != 1) return;
                stage = Stage.ARRIVED;
                break;
            case ARRIVED:
                if (++arrivedTicks < ARRIVE_TICKS) return;
                ModLog.LOG.info(
                    "[shoot] in the End, framebuffer {}x{}",
                    mc.getFramebuffer().framebufferWidth,
                    mc.getFramebuffer().framebufferHeight);
                plan = ShootServer.submit(this::planRecords);
                stage = Stage.PLANNING;
                break;
            case PLANNING:
                if (!plan.isDone()) return;
                finishPlanning(mc);
                break;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || inEnd) return;
        List<?> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        if (player.ticksExisted < JOIN_TICKS) return;
        if (!sentToEnd) {
            CameraPlacement.toEnd(player);
            sentToEnd = true;
        }
        if (!CameraPlacement.inEnd(player)) return;
        // After the move, so the abilities and the effect reach the client's new player entity.
        CameraPlacement.prepare(player);
        inEnd = true;
    }

    /**
     * Pushes the linear fog out of reach while shooting, so far terrain is not washed out. Angelica also stops
     * drawing chunks beyond the fog end, so cancelling the fog density alone left only the nearest terrain.
     */
    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (stage == Stage.MENU) return;
        GL11.glFogf(GL11.GL_FOG_START, FOG_FAR);
        GL11.glFogf(GL11.GL_FOG_END, FOG_FAR * 2);
    }

    /** Server thread: the expanded shots, each with its first camera so the report can show where it stands. */
    private List<ShotRecord> planRecords() {
        TerrainSampler sampler = EndTerrain.sampler(ShootServer.seed(list));
        List<ShotRecord> records = new ArrayList<>();
        for (PlannedShot s : ShootServer.expand(list)) {
            ShotRecord r = new ShotRecord(s);
            r.camera = ShotPlanner.firstCamera(s, sampler::biome);
            records.add(r);
        }
        return records;
    }

    private void finishPlanning(Minecraft mc) {
        List<ShotRecord> records;
        try {
            records = plan.join();
        } catch (RuntimeException e) {
            ModLog.LOG.error("[shoot] expanding the shot list failed", e);
            mc.shutdown();
            return;
        }
        ModLog.LOG.info("[shoot] {} planned shots", records.size());
        if (list.dryRun) {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            Path index = writeReport(mc, stamp, "End 撮影 " + stamp + "（dry-run）", records);
            ModLog.LOG.info("[shoot] dry run written to {}", index);
        }
        // Task 9 takes the shots here; until then every run ends after planning.
        mc.shutdown();
    }

    private Path writeReport(Minecraft mc, String stamp, String title, List<ShotRecord> records) {
        Path dir = new File(mc.mcDataDir, "etj-shots").toPath()
            .resolve(stamp);
        Path index = dir.resolve("index.html")
            .toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Files.write(
                index,
                Report.html(title, list.errors, records, settings())
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("cannot write " + index, e);
        }
        return index;
    }

    private String settings() {
        return String.format(
            Locale.ROOT,
            "seed %d / %s / origin %.0f %.0f / render-distance %d / sphere-candidates %d%s",
            list.seed,
            list.worldType,
            list.originX,
            list.originZ,
            list.renderDistance,
            list.sphereCandidates,
            list.dryRun ? " / dry-run" : "");
    }

    private void applySettings(GameSettings s) {
        s.hideGUI = true;
        s.pauseOnLostFocus = false;
        s.renderDistanceChunks = list.renderDistance;
        s.fovSetting = 70;
        s.gammaSetting = 1;
        s.thirdPersonView = 0;
    }

    private void launchWorld(Minecraft mc) {
        ISaveFormat saves = mc.getSaveLoader();
        WorldInfo info = saves.getWorldInfo(WorldPlan.SAVE);
        WorldPlan.Action action = WorldPlan.decide(info == null ? null : info.getSeed(), list.seed);
        ModLog.LOG.info("[shoot] world {}: {}", WorldPlan.SAVE, action);
        switch (action) {
            case RECREATE:
                saves.flushCache();
                saves.deleteWorldDirectory(WorldPlan.SAVE);
                break;
            case LOAD_WITH_FRESH_END:
                File save = new File(new File(mc.mcDataDir, "saves"), WorldPlan.SAVE);
                for (String dir : WorldPlan.END_DIRS) deleteTree(new File(save, dir).toPath());
                break;
            case CREATE:
                break;
        }
        WorldType type = WorldType.parseWorldType(list.worldType);
        if (type == null) {
            ModLog.LOG.warn("[shoot] unknown world type {}; using DEFAULT", list.worldType);
            type = WorldType.DEFAULT;
        }
        WorldSettings settings = new WorldSettings(list.seed, WorldSettings.GameType.CREATIVE, false, false, type)
            .enableCommands();
        mc.launchIntegratedServer(WorldPlan.SAVE, WorldPlan.SAVE, settings);
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
            for (Path p : paths) Files.delete(p);
        } catch (IOException e) {
            throw new IllegalStateException("cannot delete " + root, e);
        }
    }
}
