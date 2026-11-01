package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

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

    /** A sink over a shared map; wider than 16 on purpose. */
    private static final class MapSink implements BlockSink {

        final Map<Long, EndBlock> cells;
        final int ox, oz, size, y0, y1;

        MapSink(Map<Long, EndBlock> cells, int ox, int oz, int size, int y0, int y1) {
            this.cells = cells;
            this.ox = ox;
            this.oz = oz;
            this.size = size;
            this.y0 = y0;
            this.y1 = y1;
        }

        static long key(int x, int y, int z) {
            return ((long) x & 0xFFFFFFL) << 40 | ((long) y & 0xFFFL) << 24 | ((long) z & 0xFFFFFFL);
        }

        @Override
        public int originX() {
            return ox;
        }

        @Override
        public int originZ() {
            return oz;
        }

        @Override
        public int width() {
            return size;
        }

        @Override
        public int minY() {
            return y0;
        }

        @Override
        public int maxY() {
            return y1;
        }

        private boolean inside(int x, int y, int z) {
            return x >= ox && x < ox + size && z >= oz && z < oz + size && y >= y0 && y < y1;
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return !cells.containsKey(key(x, y, z));
        }

        @Override
        public void place(int x, int y, int z, EndBlock block) {
            if (inside(x, y, z)) cells.putIfAbsent(key(x, y, z), block);
        }

        @Override
        public void set(int x, int y, int z, EndBlock block) {
            if (inside(x, y, z)) cells.put(key(x, y, z), block);
        }

        @Override
        public void clear(int x, int y, int z) {
            cells.remove(key(x, y, z));
        }
    }

    /** What the nine chunks around (cx, cz) would draw, merged into one ordered list. */
    private static List<Glowshrooms.Anchor> around(long seed, int cx, int cz, Glowshrooms.Terrain t) {
        Set<String> seen = new HashSet<>();
        List<Glowshrooms.Anchor> all = new ArrayList<>();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) for (Glowshrooms.Anchor a : Glowshrooms.reaching(seed, cx + i, cz + j, t))
                if (seen.add(a.x + "," + a.z + "," + a.giant)) all.add(a);
        Collections.sort(all, (a, b) -> {
            if (a.giant != b.giant) return a.giant ? -1 : 1;
            int c = Long.compare(a.order, b.order);
            if (c != 0) return c;
            c = Integer.compare(a.x, b.x);
            return c != 0 ? c : Integer.compare(a.z, b.z);
        });
        return all;
    }

    private static Map<Long, EndBlock> perChunk(long seed, int cx, int cz, Glowshrooms.Terrain t, int y0, int y1) {
        Map<Long, EndBlock> cells = new HashMap<>();
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            MapSink sink = new MapSink(cells, (cx + i) * 16, (cz + j) * 16, 16, y0, y1);
            Glowshrooms.draw(Glowshrooms.reaching(seed, cx + i, cz + j, t), sink);
        }
        return cells;
    }

    private static Map<Long, EndBlock> onePiece(long seed, int cx, int cz, Glowshrooms.Terrain t, int y0, int y1) {
        Map<Long, EndBlock> cells = new HashMap<>();
        Glowshrooms.draw(around(seed, cx, cz, t), new MapSink(cells, (cx - 1) * 16, (cz - 1) * 16, 48, y0, y1));
        return cells;
    }

    @Test
    void chunksJoinLikeOneDraw() {
        Glowshrooms.Terrain t = flat(60);
        Map<Long, EndBlock> joined = perChunk(21L, 11, 11, t, 0, 256);
        assertTrue(joined.size() > 100);
        assertEquals(onePiece(21L, 11, 11, t, 0, 256), joined);
    }

    @Test
    void overlappingCapsDrawTheSameInEveryChunk() {
        Glowshrooms.Terrain t = flat(60);
        int hitX = -1, hitZ = -1;
        search: for (int cx = 8; cx < 20; cx++) for (int cz = 8; cz < 20; cz++) {
            List<Glowshrooms.Anchor> l = Glowshrooms.reaching(33L, cx, cz, t);
            for (int i = 0; i < l.size(); i++) for (int j = i + 1; j < l.size(); j++) {
                Glowshrooms.Anchor a = l.get(i), b = l.get(j);
                if (!a.giant && !b.giant && Math.hypot(a.x - b.x, a.z - b.z) < 18 && (a.x >> 4) != (b.x >> 4)) {
                    hitX = a.x >> 4;
                    hitZ = a.z >> 4;
                    break search;
                }
            }
        }
        assertTrue(hitX >= 0, "no overlapping pair found");
        assertEquals(onePiece(33L, hitX, hitZ, t, 0, 256), perChunk(33L, hitX, hitZ, t, 0, 256));
    }

    @Test
    void splitPassesMatchOnePass() {
        Glowshrooms.Terrain t = flat(100);
        Glowshrooms.Anchor g = null;
        for (int cx = 1; cx < 6 && g == null; cx++) g = Glowshrooms.giantIn(5L, cx, 0, t);
        assertTrue(g != null);
        List<Glowshrooms.Anchor> one = Collections.singletonList(g);
        int ox = (g.x >> 4) * 16 - 64, oz = (g.z >> 4) * 16 - 64;
        Map<Long, EndBlock> whole = new HashMap<>();
        Glowshrooms.draw(one, new MapSink(whole, ox, oz, 144, 0, 256));
        Map<Long, EndBlock> split = new HashMap<>();
        Glowshrooms.draw(one, new MapSink(split, ox, oz, 144, 0, 128));
        Glowshrooms.draw(one, new MapSink(split, ox, oz, 144, 128, 256));
        assertTrue(whole.size() > 1000);
        assertEquals(whole, split);
    }

    @Test
    void blockedFurIsLeftOut() {
        Glowshrooms.Terrain t = flat(60);
        Glowshrooms.Anchor a = null;
        for (int cx = 1; cx < 8 && a == null; cx++) for (int cz = 0; cz < 8 && a == null; cz++) {
            List<Glowshrooms.Anchor> l = Glowshrooms.normalIn(7L, cx, cz, t);
            if (!l.isEmpty()) a = l.get(0);
        }
        final Glowshrooms.Anchor anchor = a;
        final int[] run = new int[3];
        final boolean[] found = { false };
        anchor.shape.forEach((dx, dy, dz, block) -> {
            if (!found[0] && block == EndBlock.MYCELIA_VINE) {
                found[0] = true;
                run[0] = dx;
                run[1] = dy;
                run[2] = dz;
            }
        });
        assertTrue(found[0], "no fur");
        int x = anchor.x + run[0], y = anchor.y + run[1], z = anchor.z + run[2];
        Map<Long, EndBlock> cells = new HashMap<>();
        cells.put(MapSink.key(x, y, z), EndBlock.STONE);
        Glowshrooms.draw(
            Collections.singletonList(anchor),
            new MapSink(cells, (x >> 4) * 16 - 64, (z >> 4) * 16 - 64, 144, 0, 256));
        assertEquals(EndBlock.STONE, cells.get(MapSink.key(x, y, z)));
        for (int k = 1; k <= 3; k++) {
            EndBlock b = cells.get(MapSink.key(x, y - k, z));
            assertTrue(b != EndBlock.MYCELIA_VINE && b != EndBlock.MYCELIA_VINE_LAST, "fur at depth " + k);
        }
    }
}
