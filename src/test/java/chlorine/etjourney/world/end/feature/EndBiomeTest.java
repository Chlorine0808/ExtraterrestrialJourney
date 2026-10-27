package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EndBiomeTest {

    private static final long SEED = 7L;

    @Test
    void aboutHalfTheContinentsAreBare() {
        Set<Long> ids = new HashSet<>();
        for (int i = -40; i < 40; i++) {
            for (int j = -40; j < 40; j++) {
                Continent.Seed s = Continent.seedAt(SEED, i * 23, j * 29);
                if (s != null) ids.add(ContinentIds.of(SEED, s));
            }
        }
        int bare = 0;
        Set<EndBiome> seen = EnumSet.noneOf(EndBiome.class);
        for (long id : ids) {
            EndBiome b = EndBiome.ofContinent(SEED, id);
            if (b == null) bare++;
            else seen.add(b);
        }
        assertTrue(ids.size() > 100, "only " + ids.size() + " continents");
        assertTrue(bare > ids.size() * 0.35 && bare < ids.size() * 0.65, bare + " of " + ids.size() + " bare");
        assertEquals(EnumSet.allOf(EndBiome.class), seen);
    }

    /** Walking over land, the biome only changes where the land breaks, not in the middle of a continent. */
    @Test
    void biomeChangesOnlyAcrossTheVoid() {
        int landSteps = 0, changes = 0;
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4 + 0.3;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            EndBiome prev = null;
            boolean prevLand = false;
            for (int step = 0; step < 1500; step++) {
                double d = 1100 + step * 4.0, x = dx * d, z = dz * d;
                boolean land = Land.height(SEED, x, z, 0, 0) > 0;
                EndBiome b = EndBiome.at(SEED, x, z);
                if (land && prevLand) {
                    landSteps++;
                    if (!Objects.equals(b, prev)) changes++;
                }
                prev = b;
                prevLand = land;
            }
        }
        assertTrue(landSteps > 1000, "only " + landSteps + " steps on land");
        assertTrue(changes <= landSteps / 200, changes + " changes in " + landSteps + " steps on land");
    }

    private interface MountainPoint {

        void at(Mountains.Mountain m, double t, double wx, double wz, List<Continent.Seed> seeds);
    }

    /** Points around mountains at a share t of their radius, one mountain at a time. */
    private static void aroundMountains(double[] shares, MountainPoint check) {
        for (Mountains.Mountain m : Mountains.near(SEED, 3000, 0, 2500)) {
            for (double t : shares) {
                for (int k = 0; k < 16; k++) {
                    double wx = m.x + Math.cos(k * Math.PI / 8) * m.radius * t;
                    double wz = m.z + Math.sin(k * Math.PI / 8) * m.radius * t;
                    check.at(m, t, wx, wz, Continent.seedsNear(SEED, wx, wz, 0));
                }
            }
        }
    }

    private static EndBiome own(Continent.Seed s) {
        return EndBiome.ofContinent(SEED, ContinentIds.of(SEED, s));
    }

    /** A mountain reads as one piece: all of its body wears the biome of the continent under its peak. */
    @Test
    void mountainBodyWearsItsContinentsBiome() {
        int[] onOther = new int[1];
        aroundMountains(new double[] { 0.3, 0.6, 0.9 }, (m, t, wx, wz, seeds) -> {
            Continent.Seed land = Continent.highest(seeds, wx, wz);
            if (land != null && ContinentIds.of(SEED, land) != ContinentIds.of(SEED, m.anchor)) onOther[0]++;
            assertEquals(own(m.anchor), EndBiome.atWarped(SEED, seeds, Collections.singletonList(m), wx, wz));
        });
        assertTrue(onOther[0] > 5, "only " + onOther[0] + " body points over another continent");
    }

    /** Outside mountain bodies a continent keeps its biome on all the land it could raise. */
    @Test
    void continentLandOutsideMountainsKeepsItsBiome() {
        int[] cases = new int[1];
        aroundMountains(new double[] { 1.1, 1.3 }, (m, t, wx, wz, seeds) -> {
            if (Continent.rawHeight(seeds, wx, wz) <= 0) return;
            Continent.Seed land = Continent.highest(seeds, wx, wz);
            if (ContinentIds.of(SEED, land) == ContinentIds.of(SEED, m.anchor)) return;
            cases[0]++;
            assertEquals(own(land), EndBiome.atWarped(SEED, seeds, Collections.singletonList(m), wx, wz));
        });
        assertTrue(cases[0] > 5, "only " + cases[0] + " feet on another continent");
    }

    @Test
    void sameSeedSamePlaceSameBiome() {
        for (int i = 0; i < 50; i++) assertEquals(
            EndBiome.at(SEED, 1100 + i * 131.0, -i * 17.0),
            EndBiome.at(SEED, 1100 + i * 131.0, -i * 17.0));
    }
}
