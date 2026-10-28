package chlorine.etjourney.world.end.debug.shoot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.world.chunk.EmptyChunk;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EtjArgs;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.region.Style;
import chlorine.etjourney.world.end.shoot.Camera;
import chlorine.etjourney.world.end.shoot.Framing;
import chlorine.etjourney.world.end.shoot.Inspection;
import chlorine.etjourney.world.end.shoot.PlannedShot;
import chlorine.etjourney.world.end.shoot.Pocket;
import chlorine.etjourney.world.end.shoot.Report;
import chlorine.etjourney.world.end.shoot.ShotList;
import chlorine.etjourney.world.end.shoot.ShotRecord;
import chlorine.etjourney.world.end.shoot.Stability;
import chlorine.etjourney.world.end.shoot.Target;
import chlorine.etjourney.world.end.shoot.View;

/** Takes the planned shots one by one on the client: place, wait, check the view, capture, inspect, retry. */
final class ShotTaker {

    /** Chunk radius that must be ready on the server around the target and the camera. */
    private static final int TARGET_CHUNKS = 6, CAMERA_CHUNKS = 4;
    /** Chunk radius around the camera that the client must have received. */
    private static final int CLIENT_CHUNKS = 10, TARGET_CLIENT_CHUNKS = 2;
    /** Missing chunks the server loads per readiness poll. */
    private static final int LOAD_BUDGET = 32;
    private static final long MIN_WAIT_MS = 8_000, THUMB_MS = 1_000;
    private static final int POLL_TICKS = 10;
    private static final int THUMB_W = 64, THUMB_H = 36;
    /** Blocks the look is traced for views that stand at their subject. */
    private static final int NEAR_RANGE = 32;
    /** Ranked inside spots to consider, and how far apart the tried ones must be. */
    private static final int POCKET_RANKED = 64;
    private static final double POCKET_GAP = 8;

    private enum Phase {
        DECIDING,
        PLACING,
        WAITING,
        CHECKING,
        SETTLING,
        DONE
    }

    /** The cameras to try for one shot, worked out on the server, with what the report says about the place. */
    private static final class Plan {

        final List<Camera> cameras = new ArrayList<>();
        String note, biome, styles;
    }

    private final ShotList list;
    private final List<ShotRecord> records;
    private final Path dir;
    private final Runnable progress;
    private final Runnable rewrite;

    private int current = -1;
    private Phase phase = Phase.DONE;
    private ShotRecord rec;
    private Plan plan;
    private int attempt;
    private Camera cam;
    private CompletableFuture<?> pending;
    private boolean serverReady, blocked;
    private long placedAt, lastThumb;
    private int ticks;
    private Stability stability;
    private final List<String> notes = new ArrayList<>();
    private int[] lastImage;
    private int lastW, lastH;

    /**
     * progress is called on every step forward (for the watchdog), rewrite after every finished shot (for the
     * report).
     */
    ShotTaker(ShotList list, List<ShotRecord> records, Path dir, Runnable progress, Runnable rewrite) {
        this.list = list;
        this.records = records;
        this.dir = dir;
        this.progress = progress;
        this.rewrite = rewrite;
    }

    boolean finished() {
        return current >= records.size();
    }

    /** Marks this shot and every later one as out of time. */
    void timeUp() {
        for (int i = Math.max(0, current); i < records.size(); i++) {
            ShotRecord r = records.get(i);
            if (r.status != ShotRecord.Status.PENDING) continue;
            r.status = ShotRecord.Status.TIMED_OUT;
            r.reason = "時間切れ";
        }
        current = records.size();
        phase = Phase.DONE;
    }

    /** Client tick: moves the current shot on, or starts the next one. */
    void tick(Minecraft mc) {
        try {
            step(mc);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /** Render tick start: keeps the view on the camera however the mouse moved. */
    void holdView(Minecraft mc) {
        if (cam == null || phase == Phase.DONE || phase == Phase.DECIDING) return;
        EntityClientPlayerMP p = mc.thePlayer;
        if (p == null) return;
        p.rotationYaw = p.prevRotationYaw = p.rotationYawHead = p.prevRotationYawHead = cam.yaw;
        p.rotationPitch = p.prevRotationPitch = cam.pitch;
    }

    /** Render tick end: the frame is drawn, so thumbnails and the final capture are read here. */
    void afterFrame(Minecraft mc) {
        try {
            capture(mc);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    private void step(Minecraft mc) {
        if (finished()) return;
        if (phase == Phase.DONE) {
            next();
            return;
        }
        if (pending != null && !pending.isDone()) return;
        switch (phase) {
            case DECIDING:
                plan = (Plan) join();
                if (plan.cameras.isEmpty()) throw new IllegalStateException("カメラの置き場所が無い");
                rec.biome = plan.biome;
                rec.styles = plan.styles;
                startAttempt();
                break;
            case PLACING:
                join();
                placedAt = System.currentTimeMillis();
                stability = new Stability(3, 2.0);
                serverReady = false;
                phase = Phase.WAITING;
                progress.run();
                break;
            case WAITING:
                if (pending != null) {
                    serverReady = (Boolean) join();
                    pending = null;
                }
                if (++ticks % POLL_TICKS != 0) return;
                if (!serverReady) {
                    Target t = rec.shot.target;
                    Camera at = cam;
                    pending = ShootServer.submit(
                        () -> ShootServer.ensure(t.x, t.z, TARGET_CHUNKS, LOAD_BUDGET)
                            & ShootServer.ensure(at.x, at.z, CAMERA_CHUNKS, LOAD_BUDGET));
                    return;
                }
                if (System.currentTimeMillis() - placedAt < MIN_WAIT_MS || !clientReady(mc)) return;
                Camera c = cam;
                PlannedShot s = rec.shot;
                pending = ShootServer.submit(() -> viewProblem(c, s));
                phase = Phase.CHECKING;
                progress.run();
                break;
            case CHECKING:
                String problem = (String) join();
                pending = null;
                if (problem != null) {
                    notes.add(attemptNote(problem));
                    if (attempt + 1 < attempts()) {
                        nextAttempt();
                        return;
                    }
                    // The last try: keep its picture so the report shows what was in the way.
                    blocked = true;
                }
                lastThumb = 0;
                phase = Phase.SETTLING;
                progress.run();
                break;
            default:
                break;
        }
    }

    private void next() {
        current++;
        if (finished()) return;
        rec = records.get(current);
        attempt = 0;
        cam = null;
        plan = null;
        notes.clear();
        lastImage = null;
        PlannedShot s = rec.shot;
        if (s.target == null) {
            rec.status = ShotRecord.Status.MISSING;
            endShot();
            return;
        }
        ModLog.LOG.info("[shoot] #{} {} {}", s.index, s.group, s.view);
        pending = ShootServer.submit(() -> plan(s));
        phase = Phase.DECIDING;
        progress.run();
    }

    private int attempts() {
        return plan.cameras.size();
    }

    private void startAttempt() {
        cam = plan.cameras.get(attempt);
        rec.camera = cam;
        rec.attempts = attempt + 1;
        blocked = false;
        ticks = 0;
        Camera c = cam;
        pending = ShootServer.submit(() -> {
            CameraPlacement.place(ShootServer.player(), c);
            return null;
        });
        phase = Phase.PLACING;
        progress.run();
    }

    private void nextAttempt() {
        attempt++;
        startAttempt();
    }

    private Object join() {
        try {
            return pending.join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : e;
        } finally {
            pending = null;
        }
    }

    private boolean clientReady(Minecraft mc) {
        EntityClientPlayerMP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) return false;
        if (Math.hypot(p.posX - cam.x, p.posZ - cam.z) > 2 || Math.abs(p.boundingBox.minY - cam.y) > 2) return false;
        Target t = rec.shot.target;
        return received(mc, cam.x, cam.z, CLIENT_CHUNKS) && received(mc, t.x, t.z, TARGET_CLIENT_CHUNKS);
    }

    /** Whether the client holds every chunk within radius chunks of (x, z). */
    private static boolean received(Minecraft mc, double x, double z, int radius) {
        int ccx = (int) Math.floor(x) >> 4, ccz = (int) Math.floor(z) >> 4;
        for (int cx = ccx - radius; cx <= ccx + radius; cx++) {
            for (int cz = ccz - radius; cz <= ccz + radius; cz++) {
                if (mc.theWorld.getChunkFromChunkCoords(cx, cz) instanceof EmptyChunk) return false;
            }
        }
        return true;
    }

    private void capture(Minecraft mc) {
        if (finished() || (phase != Phase.WAITING && phase != Phase.CHECKING && phase != Phase.SETTLING)) return;
        long now = System.currentTimeMillis();
        boolean late = now - placedAt > list.settleSeconds * 1000L;
        if (!late && (phase != Phase.SETTLING || now - lastThumb < THUMB_MS || !ShootCapture.terrainBuilt())) return;
        lastThumb = now;
        int w = mc.getFramebuffer().framebufferWidth, h = mc.getFramebuffer().framebufferHeight;
        int[] frame = ShootCapture.grab(mc);
        if (late) {
            pending = null;
            keep(frame, w, h);
            notes.add("落ち着くまでに " + list.settleSeconds + " 秒を超えた");
            finish(ShotRecord.Status.TIMED_OUT, false);
            return;
        }
        if (!stability.offer(Stability.thumbnail(frame, w, h, THUMB_W, THUMB_H))) return;
        keep(frame, w, h);
        if (blocked) {
            finish(ShotRecord.Status.FAILED, true);
            return;
        }
        Inspection.Fault fault = Inspection.inspect(frame, w, h);
        if (fault == Inspection.Fault.NONE) {
            notes.clear();
            finish(ShotRecord.Status.OK, false);
            return;
        }
        notes.add(attemptNote(Report.reason(fault)));
        if (attempt + 1 < attempts()) nextAttempt();
        else finish(ShotRecord.Status.FAILED, true);
    }

    private void keep(int[] frame, int w, int h) {
        lastImage = frame;
        lastW = w;
        lastH = h;
    }

    private String attemptNote(String why) {
        return "試行 " + (attempt + 1) + ": " + why;
    }

    private void finish(ShotRecord.Status status, boolean failed) {
        rec.status = status;
        if (lastImage != null) {
            String name = Report.imageName(rec.shot, failed);
            ShootCapture.write(
                lastImage,
                lastW,
                lastH,
                dir.resolve(name)
                    .toFile());
            rec.image = name;
            rec.width = lastW;
            rec.height = lastH;
        }
        List<String> why = new ArrayList<>();
        if (plan != null && plan.note != null) why.add(plan.note);
        why.addAll(notes);
        rec.reason = why.isEmpty() ? null : String.join(" / ", why);
        ModLog.LOG.info("[shoot] #{} {}{}", rec.shot.index, status, rec.reason == null ? "" : ": " + rec.reason);
        endShot();
    }

    /** Any exception fails the current shot with its first line, and the run goes on. */
    private void fail(RuntimeException e) {
        if (rec == null || finished()) throw e;
        ModLog.LOG.error("[shoot] #{} failed", rec.shot.index, e);
        pending = null;
        String line = String.valueOf(e);
        int nl = line.indexOf('\n');
        notes.add(nl < 0 ? line : line.substring(0, nl));
        try {
            finish(ShotRecord.Status.FAILED, true);
        } catch (RuntimeException again) {
            ModLog.LOG.error("[shoot] #{} could not be written", rec.shot.index, again);
            rec.status = ShotRecord.Status.FAILED;
            rec.reason = String.join(" / ", notes);
            endShot();
        }
    }

    private void endShot() {
        phase = Phase.DONE;
        cam = null;
        lastImage = null;
        ShootServer.submit(() -> {
            ShootServer.release(list.renderDistance);
            return null;
        });
        rewrite.run();
        progress.run();
    }

    /** Server thread: the cameras to try for the shot, best first, and the biome and styles at its target. */
    private Plan plan(PlannedShot s) {
        TerrainSampler sampler = EndTerrain.sampler(ShootServer.seed(list));
        Target t = s.target;
        Plan p = new Plan();
        EndBiome biome = sampler.biome(t.x, t.z);
        p.biome = biome == null ? "なし" : biome.name();
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Map.Entry<Style, Double> e : sampler.weights(t.x, t.z)
            .asMap()
            .entrySet()) weights.put(e.getKey().name, e.getValue());
        p.styles = Report.styles(weights);
        if (s.fixed != null) {
            p.cameras.add(s.fixed);
            return p;
        }
        switch (s.view) {
            case AERIAL:
                for (int i = 0; i < Framing.MAX_ATTEMPTS; i++) p.cameras.add(Framing.aerial(t, i));
                break;
            case OUTSIDE:
                for (int i = 0; i < Framing.MAX_ATTEMPTS; i++) p.cameras.add(Framing.outside(t, i));
                break;
            case GROUND:
                ground(sampler, t, biome, p);
                break;
            case INSIDE:
                inside(sampler, t, p);
                break;
        }
        return p;
    }

    /** Three looks from the target's column, then three from a column further into the biome. */
    private static void ground(TerrainSampler sampler, Target t, EndBiome biome, Plan p) {
        float best = Framing.groundYaws(sampler::biome, t.x, t.z, biome)
            .get(0);
        for (double[] spot : Framing.groundSpots(t.x, t.z, best)) {
            ShootServer.ensure(spot[0], spot[1], 1, Integer.MAX_VALUE);
            int x = (int) Math.floor(spot[0]), z = (int) Math.floor(spot[1]);
            int y = ShootServer.standY(x, t.y, z);
            List<Float> yaws = Framing.groundYaws(sampler::biome, x + 0.5, z + 0.5, biome);
            for (int i = 0; i < Framing.MAX_ATTEMPTS; i++)
                p.cameras.add(Framing.ground(x + 0.5, y, z + 0.5, yaws.get(i)));
        }
    }

    private static void inside(TerrainSampler sampler, Target t, Plan p) {
        Biosphere b = ShootServer.ball(sampler, t.x, t.z, t.key);
        if (b == null) throw new IllegalStateException("球が見つからない");
        ShootServer.ensure(b.centreX, b.centreZ, (int) Math.ceil((b.radius + 1) / 16) + 1, Integer.MAX_VALUE);
        ShootServer.BallGrid g = ShootServer.grid(b);
        List<Camera> ranked = Pocket.rank(g, g.x0, g.y0, g.z0, g.x1, g.y1, g.z1, b.centreX, b.centreZ, POCKET_RANKED);
        p.cameras.addAll(Pocket.spread(ranked, Framing.MAX_ATTEMPTS, POCKET_GAP));
        if (!p.cameras.isEmpty()) return;
        p.cameras.add(Framing.outside(t, 2));
        p.note = String.format(Locale.ROOT, "中に立てる空気が無いので外から反対側を撮った（中の空気 %d / %d）", g.air, g.inside);
    }

    /**
     * Server thread: why camera c cannot see the subject of s, or null when it can. Views that stand at their
     * subject are traced level, since a slightly lowered look meets flat ground within a dozen blocks.
     */
    private static String viewProblem(Camera c, PlannedShot s) {
        if (s.fixed != null) return null;
        double eyeY = c.y + Framing.EYE;
        if (!ShootServer.air(c.x, c.y, c.z) || !ShootServer.air(c.x, eyeY, c.z)) return "カメラがブロックに埋まっている";
        double[] eye = { c.x, eyeY, c.z };
        Target t = s.target;
        double[] dir;
        int range;
        double subject;
        if (s.view == View.AERIAL || s.view == View.OUTSIDE) {
            double dx = t.x - eye[0], dy = t.y - eye[1], dz = t.z - eye[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dir = new double[] { dx / d, dy / d, dz / d };
            range = (int) Math.ceil(d);
            // The ball's glass is the subject seen from outside, not its centre.
            subject = s.view == View.OUTSIDE ? d - t.size : d;
        } else {
            dir = EtjArgs.lookDirection(c.yaw, 0);
            range = NEAR_RANGE;
            subject = 0;
        }
        double hit = ShootServer.firstHit(eye, dir, range);
        if (!Framing.clear(hit, subject, s.view)) return String.format(Locale.ROOT, "視界がふさがれている（%.0f ブロック先）", hit);
        if (s.view == View.GROUND && !Framing
            .seesGround(ShootServer.firstHit(eye, EtjArgs.lookDirection(c.yaw, c.pitch), Framing.GROUND_SIGHT)))
            return "地面が見えない（虚空を向いている）";
        return null;
    }
}
