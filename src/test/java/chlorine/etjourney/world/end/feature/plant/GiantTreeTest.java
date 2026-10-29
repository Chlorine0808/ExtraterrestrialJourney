package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * The giant tree's shape, drawn chunk by chunk into empty air: a trunk about 20 across rising about 50, roots that
 * spread out along the ground, and nothing beyond its footprint or outside the sink it is given.
 */
class GiantTreeTest {

    private static final int GROUND = 70;

    /** Several trees: different seeds and roots, the roots on chunk centres like the continent seeds they stand on. */
    private static List<Drawn> trees() {
        List<Drawn> out = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double x = 16 * (i * 7 - 40) + 8, z = 16 * (i * 3 - 17) + 8;
            out.add(new Drawn(1000L + i * 7919L, new double[] { x, GROUND + (i % 3) * 0.4, z }, 0, 256));
        }
        return out;
    }

    @Test
    void trunkIsAboutTwentyAcross() {
        for (Drawn t : trees()) {
            int y = t.base + 15, minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE,
                maxZ = Integer.MIN_VALUE;
            for (Map.Entry<Long, EndBlock> e : t.blocks.entrySet()) {
                int[] p = unpack(e.getKey());
                if (p[1] != y || e.getValue() != EndBlock.GROVE_LOG || t.distance(p) > 16) continue;
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minZ = Math.min(minZ, p[2]);
                maxZ = Math.max(maxZ, p[2]);
            }
            int acrossX = maxX - minX + 1, acrossZ = maxZ - minZ + 1;
            assertTrue(acrossX >= 18 && acrossX <= 22, t + " is " + acrossX + " across in x");
            assertTrue(acrossZ >= 18 && acrossZ <= 22, t + " is " + acrossZ + " across in z");
        }
    }

    @Test
    void risesAboutFifty() {
        for (Drawn t : trees()) {
            int top = Integer.MIN_VALUE;
            for (Map.Entry<Long, EndBlock> e : t.blocks.entrySet()) {
                if (e.getValue() == EndBlock.GROVE_LOG) top = Math.max(top, unpack(e.getKey())[1]);
            }
            int rise = top - t.base;
            assertTrue(rise >= 45 && rise <= 55, t + " rises " + rise);
        }
    }

    @Test
    void rootsSpreadOut() {
        for (Drawn t : trees()) {
            // Far roots near the ground, counted by the eighth of the circle they lie in.
            boolean[] octants = new boolean[8];
            for (Map.Entry<Long, EndBlock> e : t.blocks.entrySet()) {
                int[] p = unpack(e.getKey());
                if (e.getValue() != EndBlock.GROVE_LOG || Math.abs(p[1] - t.base) > 2 || t.distance(p) < 16) continue;
                double angle = Math.atan2(p[2] + 0.5 - t.anchor[2], p[0] + 0.5 - t.anchor[0]);
                octants[(int) Math.floorMod((int) Math.floor(angle / (Math.PI / 4)), 8)] = true;
            }
            int reached = 0;
            for (boolean o : octants) if (o) reached++;
            assertTrue(reached >= 4, t + " has far roots in " + reached + " of 8 directions");
        }
    }

    @Test
    void staysWithinFootprint() {
        for (Drawn t : trees()) {
            for (long k : t.blocks.keySet()) {
                int[] p = unpack(k);
                assertTrue(t.distance(p) <= GiantTrees.FOOTPRINT, t + " reaches " + t.distance(p));
                assertTrue(p[1] < t.base + GiantTrees.HEIGHT, t + " reaches up to " + p[1]);
                assertTrue(p[1] >= t.base - 16, t + " reaches down to " + p[1]);
            }
        }
        // A tree crossing Y 128 drawn into either half stays inside it (the test sink throws otherwise).
        new Drawn(5L, new double[] { 8, 100, 8 }, 0, 128);
        new Drawn(5L, new double[] { 8, 100, 8 }, 128, 256);
    }

    @Test
    void someLeavesBlossom() {
        for (Drawn t : trees()) {
            int leaves = 0, blossoms = 0;
            for (EndBlock b : t.blocks.values()) {
                if (b == EndBlock.GROVE_LEAVES) leaves++;
                if (b == EndBlock.GROVE_BLOSSOM) blossoms++;
            }
            double share = blossoms / (double) (leaves + blossoms);
            assertTrue(share >= 0.025 && share <= 0.055, t + " blossom share " + share);
        }
    }

    @Test
    void reportsBlockCount() {
        long logs = 0, leaves = 0;
        List<Drawn> all = trees();
        for (Drawn t : all) {
            for (EndBlock b : t.blocks.values()) {
                if (b == EndBlock.GROVE_LOG) logs++;
                else leaves++;
            }
        }
        System.out
            .println("giant tree: " + logs / all.size() + " logs and " + leaves / all.size() + " leaves per tree");
        assertTrue(logs > 0);
    }

    private static int[] unpack(long k) {
        return new int[] { (int) (k >> 40), (int) (k & 0xFF), (int) ((k << 24) >> 40) };
    }

    /** One tree drawn into every chunk its footprint reaches, over empty air. */
    private static final class Drawn {

        final long seed;
        final double[] anchor;
        final int base;
        final Map<Long, EndBlock> blocks = new HashMap<>();

        Drawn(long seed, double[] anchor, int minY, int maxY) {
            this.seed = seed;
            this.anchor = anchor;
            this.base = (int) Math.floor(anchor[1]);
            GiantTree tree = new GiantTree(seed, anchor);
            int reach = GiantTrees.FOOTPRINT + 16;
            int c0x = (int) Math.floor((anchor[0] - reach) / 16), c1x = (int) Math.floor((anchor[0] + reach) / 16);
            int c0z = (int) Math.floor((anchor[2] - reach) / 16), c1z = (int) Math.floor((anchor[2] + reach) / 16);
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) tree.draw(new Sink(cx * 16, cz * 16, minY, maxY));
            }
        }

        double distance(int[] p) {
            return Math.hypot(p[0] + 0.5 - anchor[0], p[2] + 0.5 - anchor[2]);
        }

        @Override
        public String toString() {
            return "tree " + seed + " at " + anchor[0] + "," + anchor[2];
        }

        /** A chunk of empty air writing into the shared map; it throws on any write outside itself. */
        private final class Sink implements BlockSink {

            private final int ox, oz, minY, maxY;

            Sink(int ox, int oz, int minY, int maxY) {
                this.ox = ox;
                this.oz = oz;
                this.minY = minY;
                this.maxY = maxY;
            }

            private long key(int x, int y, int z) {
                if (x < ox || x >= ox + 16 || z < oz || z >= oz + 16 || y < minY || y >= maxY)
                    throw new IllegalArgumentException("outside the sink: " + x + "," + y + "," + z);
                return ((long) x << 40) ^ (((long) z & 0xFFFFFFL) << 16) ^ y;
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
            public int minY() {
                return minY;
            }

            @Override
            public int maxY() {
                return maxY;
            }

            @Override
            public boolean isAir(int x, int y, int z) {
                return !blocks.containsKey(key(x, y, z));
            }

            @Override
            public void place(int x, int y, int z, EndBlock block) {
                if (isAir(x, y, z)) blocks.put(key(x, y, z), block);
            }

            @Override
            public void set(int x, int y, int z, EndBlock block) {
                blocks.put(key(x, y, z), block);
            }

            @Override
            public void clear(int x, int y, int z) {
                blocks.remove(key(x, y, z));
            }
        }
    }
}
