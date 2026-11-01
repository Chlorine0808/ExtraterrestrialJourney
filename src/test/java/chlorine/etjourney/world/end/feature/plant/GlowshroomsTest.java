package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GlowshroomsTest {

    private static Glowshrooms.Terrain flat(final double ground) {
        return new Glowshrooms.Terrain() {

            @Override
            public double ground(double x, double z) {
                return ground;
            }

            @Override
            public boolean mycelia(double x, double z) {
                return x >= 0;
            }
        };
    }

    @Test
    void sameSeedSamePlaces() {
        Glowshrooms.Terrain t = flat(60);
        List<Glowshrooms.Anchor> a = Glowshrooms.normalIn(7L, 3, 4, t);
        List<Glowshrooms.Anchor> b = Glowshrooms.normalIn(7L, 3, 4, t);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x);
            assertEquals(a.get(i).y, b.get(i).y);
            assertEquals(a.get(i).z, b.get(i).z);
            assertEquals(a.get(i).shape.scale(), b.get(i).shape.scale());
        }
    }

    @Test
    void zeroToTwoPerCell() {
        Glowshrooms.Terrain t = flat(60);
        int total = 0;
        for (int i = 0; i < 400; i++) {
            int n = Glowshrooms.normalIn(11L, 1 + i % 20, i / 20, t)
                .size();
            assertTrue(n >= 0 && n <= 2);
            total += n;
        }
        assertEquals(1.0, total / 400.0, 0.15);
    }

    @Test
    void onlyOnMycelia() {
        Glowshrooms.Terrain t = flat(60);
        for (int cx = -5; cx < 5; cx++) for (int cz = 0; cz < 5; cz++)
            for (Glowshrooms.Anchor a : Glowshrooms.normalIn(5L, cx, cz, t)) assertTrue(a.x >= 0);
    }

    @Test
    void giantMargin() {
        Glowshrooms.Terrain t = flat(60);
        int found = 0;
        for (int i = 0; i < 50; i++) {
            int cx = 1 + i % 10, cz = i / 10;
            Glowshrooms.Anchor a = Glowshrooms.giantIn(3L, cx, cz, t);
            if (a == null) continue;
            found++;
            assertTrue(a.giant);
            assertTrue(a.x - cx * 256 >= 64 && cx * 256 + 256 - a.x > 64);
            assertTrue(a.z - cz * 256 >= 64 && cz * 256 + 256 - a.z > 64);
        }
        assertTrue(found >= 30, "found " + found);
    }

    @Test
    void giantOnHighGroundFits() {
        Glowshrooms.Terrain t = flat(200);
        for (int i = 0; i < 4; i++) {
            Glowshrooms.Anchor a = Glowshrooms.giantIn(9L, 1 + i, 0, t);
            if (a != null) assertTrue(a.y + a.shape.maxY() <= 250);
        }
    }

    @Test
    void reachingFindsNeighbours() {
        Glowshrooms.Terrain t = flat(60);
        int cx = 10, cz = 10;
        Glowshrooms.Anchor want = null;
        for (int dz = -2; dz <= 2 && want == null; dz++) {
            List<Glowshrooms.Anchor> l = Glowshrooms.normalIn(13L, cx + 2, cz + dz, t);
            if (!l.isEmpty()) want = l.get(0);
        }
        assertTrue(want != null);
        List<Glowshrooms.Anchor> r = Glowshrooms.reaching(13L, cx, cz, t);
        boolean found = false, normalSeen = false;
        for (Glowshrooms.Anchor a : r) {
            if (a.x == want.x && a.y == want.y && a.z == want.z) found = true;
            if (!a.giant) normalSeen = true;
            else assertTrue(!normalSeen, "giant after normal");
        }
        assertTrue(found);
    }
}
