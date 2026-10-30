package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;
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
            out.add(new Drawn(1000L + i * 7919L, new double[] { x, GROUND + (i % 3) * 0.4, z }, 0, 256, 0));
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
                assertTrue(p[1] >= t.base - GiantTree.MAX_DROP - GiantTree.SINK, t + " reaches down to " + p[1]);
            }
        }
        // A tree crossing Y 128 drawn into either half stays inside it (the test sink throws otherwise).
        new Drawn(5L, new double[] { 8, 100, 8 }, 0, 128, 0);
        new Drawn(5L, new double[] { 8, 100, 8 }, 128, 256, 0);
    }

    /**
     * On a slope the probe's ground falls well below the root's: the trunk still carries on below the local ground
     * on the low side, and the far roots still touch it.
     */
    @Test
    void followsSlopedGround() {
        for (int i = 0; i < 6; i++) {
            Drawn t = new Drawn(77L + i, new double[] { 8 + 32 * i, GROUND, 8 }, 0, 256, 0.6);
            Map<Long, Integer> lowest = new HashMap<>();
            for (Map.Entry<Long, EndBlock> e : t.blocks.entrySet()) {
                if (e.getValue() != EndBlock.GROVE_LOG) continue;
                int[] p = unpack(e.getKey());
                lowest.merge(((long) p[0] << 32) ^ (p[2] & 0xFFFFFFFFL), p[1], Math::min);
            }
            int trunk = 0, roots = 0;
            for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
                int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
                double d = t.distance(new int[] { x, 0, z }), ground = t.ground(x + 0.5, z + 0.5);
                if (d <= 11) {
                    trunk++;
                    assertTrue(e.getValue() < ground - 2, t + " trunk ends at " + e.getValue() + " over " + ground);
                } else if (d >= 18) {
                    roots++;
                    assertTrue(e.getValue() <= ground + 1, t + " root floats at " + e.getValue() + " over " + ground);
                }
            }
            assertTrue(trunk > 100 && roots > 10, t + ": " + trunk + " trunk and " + roots + " root columns");
        }
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

    /** Leaves decay beyond Grower.LEAF_REACH face steps of wood through leaves; every leaf and blossom lies within. */
    @Test
    void leavesStayNearWood() {
        int worst = 0;
        for (Drawn t : trees()) {
            int steps = LeafReach.worst(t.blocks, GiantTreeTest::unpack);
            worst = Math.max(worst, steps);
            assertTrue(steps <= Grower.LEAF_REACH, t + " has a leaf " + steps + " steps from wood");
        }
        System.out.println("giant tree: leaves at most " + worst + " steps from wood");
    }

    /** A sink whose heights the tree misses is left alone, so the pass need not build the tree for it. */
    @Test
    void reachesOnlyTheHeightsItDraws() {
        double[] anchor = { 8, 100, 8 };
        assertTrue(!GiantTree.reaches(anchor, 0, 64) && !GiantTree.reaches(anchor, 160, 256));
        assertTrue(GiantTree.reaches(anchor, 0, 128) && GiantTree.reaches(anchor, 128, 256));
        for (int minY = 0; minY < 256; minY += 16) {
            if (GiantTree.reaches(anchor, minY, minY + 16)) continue;
            assertTrue(new Drawn(5L, anchor, minY, minY + 16, 0.6).blocks.isEmpty(), "drew into " + minY);
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

        final double slope;

        /** The ground falls by slope blocks per block towards negative z, level with the root at the root. */
        Drawn(long seed, double[] anchor, int minY, int maxY, double slope) {
            this.seed = seed;
            this.anchor = anchor;
            this.slope = slope;
            this.base = (int) Math.floor(anchor[1]);
            GiantTree tree = new GiantTree(seed, anchor, new StructureProbe() {

                @Override
                public double weight(String style, double x, double z) {
                    return 0;
                }

                @Override
                public double land(double x, double z) {
                    return 1;
                }

                @Override
                public double ground(double x, double z) {
                    return Drawn.this.ground(x, z);
                }
            });
            int reach = GiantTrees.FOOTPRINT + 16;
            int c0x = (int) Math.floor((anchor[0] - reach) / 16), c1x = (int) Math.floor((anchor[0] + reach) / 16);
            int c0z = (int) Math.floor((anchor[2] - reach) / 16), c1z = (int) Math.floor((anchor[2] + reach) / 16);
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) tree.draw(new Sink(cx * 16, cz * 16, minY, maxY));
            }
        }

        double ground(double x, double z) {
            return anchor[1] + slope * (z - anchor[2]);
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
