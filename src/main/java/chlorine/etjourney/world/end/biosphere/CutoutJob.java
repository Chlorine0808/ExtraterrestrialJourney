package chlorine.etjourney.world.end.biosphere;

import java.util.List;

import net.minecraft.block.BlockFalling;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.feature.Cutout;

/** A ball's cut-out made a step at a time: its sample chunks one by one, then the ball column by column. */
final class CutoutJob {

    final Biosphere ball;
    private final long seed;
    private final BiosphereSource.Kind[] kinds;
    private int kindIndex = -1;
    private BiosphereSource.Kind kind;
    private WorldServer world;
    private int[] point;
    private List<int[]> chunks;
    private int nextChunk;
    private Cutout cut;
    private int lift, nextX;
    private long busy;
    private boolean done;
    private Cutout result;

    CutoutJob(long seed, Biosphere ball) {
        this.seed = seed;
        this.ball = ball;
        this.kinds = BiosphereSource.order(BiosphereSource.kind(seed, ball));
    }

    /** Works for up to budget nanoseconds, stopping between steps; true once the job is finished. */
    boolean step(long budget) {
        long start = System.nanoTime();
        while (!done) {
            long t = System.nanoTime();
            try {
                if (world == null && !nextKind()) {
                    done = true;
                    break;
                }
                advance();
            } catch (Throwable e) {
                // Another mod's generator failing in the sample dimension; try the other world.
                SampleWorlds.rethrowFatal(e);
                // Vanilla population sets this for its duration; an exception part way would leave sand falling
                // instantly everywhere.
                BlockFalling.fallInstantly = false;
                ModLog.LOG.warn(
                    "Biosphere at {},{} could not be cut from {}",
                    (int) ball.centreX,
                    (int) ball.centreZ,
                    kind,
                    e);
                if (world != null) CutoutMaker.discard(world);
                world = null;
            }
            busy += System.nanoTime() - t;
            if (System.nanoTime() - start >= budget) break;
        }
        return done;
    }

    /** The cut-out, or null when the job is not finished or neither world could make it. */
    Cutout result() {
        return result;
    }

    boolean done() {
        return done;
    }

    /** Whether the job holds a sample world that is no longer loaded, as after the server stopped part way. */
    boolean stale() {
        return world != null && DimensionManager.getWorld(world.provider.dimensionId) != world;
    }

    /** Stops the job and throws its sample chunks away, so the next cut starts from an empty sample world. */
    void abort() {
        if (world != null) CutoutMaker.discard(world);
        world = null;
        done = true;
    }

    private boolean nextKind() {
        while (++kindIndex < kinds.length) {
            kind = kinds[kindIndex];
            world = SampleWorlds.world(kind);
            if (world == null) continue;
            WorldServer w = world;
            point = BiosphereSource.point(seed, ball, kind, (x, z) -> SampleWorlds.isWater(w, x, z));
            chunks = CutoutMaker.chunks(ball, point);
            nextChunk = 0;
            cut = null;
            return true;
        }
        return false;
    }

    private void advance() {
        if (nextChunk < chunks.size()) {
            int[] c = chunks.get(nextChunk++);
            world.theChunkProviderServer.loadChunk(c[0], c[1]);
            return;
        }
        if (cut == null) {
            lift = CutoutMaker.lift(world, kind, point, ball);
            cut = new Cutout(ball);
            nextX = (int) Math.floor(ball.minX());
            return;
        }
        if (nextX <= (int) Math.ceil(ball.maxX())) {
            CutoutMaker.cutColumn(world, ball, cut, point, lift, nextX++);
            return;
        }
        if (ETJConfig.debugLogging) ModLog.LOG.info(
            "Biosphere {},{} cut from {} {},{} in {} ms",
            (int) Math.floor(ball.centreX),
            (int) Math.floor(ball.centreZ),
            kind,
            point[0],
            point[1],
            busy / 1_000_000);
        CutoutMaker.discard(world);
        world = null;
        result = cut;
        done = true;
    }
}
