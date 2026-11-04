package chlorine.etjourney.world.end.modifier.builtin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.plant.GiantTree;
import chlorine.etjourney.world.end.feature.plant.GiantTrees;
import chlorine.etjourney.world.end.feature.plant.Glowshrooms;
import chlorine.etjourney.world.end.feature.plant.LucentTrees;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Block modifiers for plants too large for the populate window. */
public final class PlantModifiers {

    /** How far a tree's root may be from the chunk's centre and still reach the chunk. */
    private static final int REACH = GiantTrees.FOOTPRINT + 12;

    /** Distances from the root at which the footprint is checked against reserved fades: the foot and the root tips. */
    private static final double[] FADE_RADII = { 14, GiantTrees.FOOTPRINT };

    private static final AtomicLong NANOS = new AtomicLong(), PASSES = new AtomicLong();

    private PlantModifiers() {}

    /** Average microseconds the giant-tree modifier spends per sink, and the number of sinks, for the probe. */
    public static long[] giantTreeStats() {
        long passes = PASSES.get();
        return new long[] { passes == 0 ? 0 : NANOS.get() / passes / 1000, passes };
    }

    /**
     * On a reserved island, or with any of the footprint on ground an island's fade lowers: the tree reads the ground
     * without the fade, so its trunk and roots would float there.
     */
    static boolean blocked(List<Area> reserved, double[] a) {
        if (Reservations.touches(reserved, a[0], a[2], GiantTrees.FOOTPRINT)) return true;
        if (Reservations.suppression(reserved, a[0], a[2]) > 0.1) return true;
        for (double r : FADE_RADII) {
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4;
                if (Reservations.suppression(reserved, a[0] + r * Math.cos(angle), a[2] + r * Math.sin(angle)) > 0.1)
                    return true;
            }
        }
        return false;
    }

    /**
     * The giant trees reaching the chunk, found from the continent seeds near it. A tree whose footprint touches a
     * reserved area, or stands in its fade, as seen from its root's chunk, is left out; so is a tree outside the sink's
     * heights.
     */
    public static Modifier giantTrees(int order) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            long start = System.nanoTime();
            try {
                drawGiantTrees(area, view, sink);
            } finally {
                NANOS.addAndGet(System.nanoTime() - start);
                PASSES.incrementAndGet();
            }
        });
    }

    private static void drawGiantTrees(ChunkArea area, TerrainView view, BlockSink sink) {
        double cx = sink.originX() + 8, cz = sink.originZ() + 8;
        // A root is a seed of its continent, so the seeds within REACH find every tree that reaches the chunk.
        int c0x = (int) Math.floor((cx - REACH) / Continent.SEED_CELL);
        int c1x = (int) Math.floor((cx + REACH) / Continent.SEED_CELL);
        int c0z = (int) Math.floor((cz - REACH) / Continent.SEED_CELL);
        int c1z = (int) Math.floor((cz + REACH) / Continent.SEED_CELL);
        Set<Long> seen = new HashSet<>();
        for (int i = c0x; i <= c1x; i++) {
            for (int j = c0z; j <= c1z; j++) {
                Continent.Seed s = Continent.seedAt(area.seed, i, j);
                if (s == null || Math.hypot(s.x - cx, s.z - cz) > REACH) continue;
                if (!seen.add(ContinentIds.of(area.seed, s))) continue;
                for (double[] a : GiantTrees.anchors(area.seed, s, view.structures())) {
                    if (Math.hypot(a[0] - cx, a[2] - cz) > REACH) continue;
                    if (!GiantTree.reaches(a, sink.minY(), sink.maxY())) continue;
                    List<Area> reserved = view.reservedAt((int) Math.floor(a[0]) >> 4, (int) Math.floor(a[2]) >> 4);
                    if (blocked(reserved, a)) continue;
                    new GiantTree(area.seed, a, view.structures()).draw(sink);
                }
            }
        }
    }

    /** LAYERED stacks tiers over the probe's ground, so the probe's ground is not its top. */
    private static final double LAYERED_LIMIT = 0.05;

    /**
     * The glowshrooms reaching the chunk. An anchor whose cap touches a reserved area, or whose root stands in a
     * fade, as seen from the root's chunk, is left out, so every chunk judges it alike.
     */
    public static Modifier glowshrooms(int order) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            Glowshrooms.Terrain terrain = new Glowshrooms.Terrain() {

                @Override
                public double ground(double x, double z) {
                    return view.plants()
                        .ground(x, z);
                }

                @Override
                public boolean mycelia(double x, double z) {
                    // The same integer sample the surface paint takes.
                    int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
                    // Anchors lie outside the chunk's window, so the biome looks up its own seeds and mountains.
                    return EndBiome.at(area.seed, bx, bz, view.voidShare(bx, bz)) == EndBiome.MYCELIA
                        && view.weight("LAYERED", bx, bz) < LAYERED_LIMIT;
                }

                @Override
                public boolean wet(double x, double z, double range) {
                    return view.plants()
                        .wet(x, z, range);
                }
            };
            List<Glowshrooms.Anchor> kept = new ArrayList<>();
            for (Glowshrooms.Anchor a : Glowshrooms
                .reaching(area.seed, sink.originX() >> 4, sink.originZ() >> 4, terrain)) {
                List<Area> reserved = view.reservedAt(a.x >> 4, a.z >> 4);
                double radius = Math.ceil(20 * a.shape.scale());
                if (Reservations.touches(reserved, a.x, a.z, radius)) continue;
                if (Reservations.suppression(reserved, a.x, a.z) > 0.1) continue;
                kept.add(a);
            }
            Glowshrooms.draw(kept, sink);
        });
    }

    /**
     * The Lucentwoods trees reaching the chunk, judged against reserved areas as the glowshrooms are: from the root's
     * chunk, the crown's reach and the root's fade.
     */
    public static Modifier lucentTrees(int order) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            LucentTrees.Terrain terrain = new LucentTrees.Terrain() {

                @Override
                public double ground(double x, double z) {
                    return view.structures()
                        .ground(x, z);
                }

                @Override
                public boolean lucent(double x, double z) {
                    int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
                    return EndBiome.at(area.seed, bx, bz, view.voidShare(bx, bz)) == EndBiome.LUCENTWOODS
                        && view.weight("LAYERED", bx, bz) < LAYERED_LIMIT;
                }
            };
            List<LucentTrees.Anchor> kept = new ArrayList<>();
            for (LucentTrees.Anchor a : LucentTrees
                .reaching(area.seed, sink.originX() >> 4, sink.originZ() >> 4, terrain)) {
                List<Area> reserved = view.reservedAt(a.x >> 4, a.z >> 4);
                double radius = Math
                    .max(Math.max(-a.shape.minX(), a.shape.maxX()), Math.max(-a.shape.minZ(), a.shape.maxZ()));
                if (Reservations.touches(reserved, a.x, a.z, radius)) continue;
                if (Reservations.suppression(reserved, a.x, a.z) > 0.1) continue;
                kept.add(a);
            }
            LucentTrees.draw(kept, sink);
        });
    }
}
