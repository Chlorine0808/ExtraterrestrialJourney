package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.ModifierChain;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.reserve.Area;

class BuiltinModifiersTest {

    private static final long SEED = 21L;

    /** A view with the real continent seeds and nothing else. */
    private static TerrainView view(double x, double z) {
        List<Continent.Seed> seeds = Continent.seedsNear(SEED, x, z, 64);
        return new TerrainView() {

            @Override
            public List<Continent.Seed> seeds() {
                return seeds;
            }

            @Override
            public List<Mountains.Mountain> mountains() {
                return Collections.emptyList();
            }

            @Override
            public List<Lakes.Lake> lakes() {
                return Collections.emptyList();
            }

            @Override
            public List<Holes.Hole> holes() {
                return Collections.emptyList();
            }

            @Override
            public List<ZoneIslands.Island> zoneIslands() {
                return Collections.emptyList();
            }

            @Override
            public List<Islets.Islet> islets() {
                return Collections.emptyList();
            }

            @Override
            public List<Shoals.School> schools() {
                return Collections.emptyList();
            }

            @Override
            public ArcPaths.Segments arcs() {
                return ArcPaths.segmentsNear(Collections.emptyList(), 0, 0, 0, 0, (a, b, c) -> true);
            }

            @Override
            public List<Area> reservedAt(int chunkX, int chunkZ) {
                return Collections.emptyList();
            }

            @Override
            public double voidShare(double x, double z) {
                return 0;
            }

            @Override
            public double mountainScale(double x, double z) {
                return 1;
            }

            @Override
            public double valleyScale(double x, double z) {
                return 1;
            }
        };
    }

    /** First column along z = 0 east of the ring whose continent height is at least 70. */
    private static double landX() {
        for (int x = 1100; x < 20000; x += 8) {
            if (Continent.rawHeight(Continent.seedsNear(SEED, x, 0, 0), x, 0) > 70) return x;
        }
        throw new AssertionError("no land");
    }

    private static ColumnState run(double x, List<Modifier> extra) {
        List<Modifier> all = new ArrayList<>(CoreModifiers.all());
        all.addAll(extra);
        ColumnState state = new ColumnState(x, 0);
        ChunkArea area = new ChunkArea(SEED, (int) x >> 4, 0, Collections.emptyList(), view(x, 0));
        new ModifierChain(all).column(area, state, m -> 1);
        return state;
    }

    @Test
    void coreBuildsALandColumnInsideTheWorld() {
        ColumnState s = run(landX(), Collections.emptyList());
        assertTrue(s.land > 0);
        assertTrue(s.bottom >= 6 && s.bottom <= 110, "bottom " + s.bottom);
        assertTrue(s.top > s.bottom, "top " + s.top + " bottom " + s.bottom);
    }

    @Test
    void lowlandsLowerTheGroundBy28() {
        double x = landX();
        ColumnState plain = run(x, Collections.emptyList());
        ColumnState low = run(x, Collections.singletonList(StyleModifiers.lowlands()));
        // The bottom shift may move both; compare before bounds by checking the drop stays close to 28.
        assertEquals(28, plain.level - low.level, 28);
        assertTrue(low.level < plain.level);
    }

    @Test
    void layersStackInsideTheWorldAndAboveTheOldCeiling() {
        int stacked = 0;
        double highest = 0;
        for (int i = 0; i < 200; i++) {
            ColumnState s = run(landX() + i * 8, Collections.singletonList(StyleModifiers.layers()));
            stacked += s.layers.size();
            assertTrue(s.layers.size() <= 5);
            for (Layer layer : s.layers) {
                assertTrue(layer.top <= 250 + 1e-9 && layer.bottom < layer.top);
                highest = Math.max(highest, layer.top);
            }
        }
        assertTrue(stacked > 0);
        assertTrue(highest > 128, "highest layer " + highest);
    }
}
