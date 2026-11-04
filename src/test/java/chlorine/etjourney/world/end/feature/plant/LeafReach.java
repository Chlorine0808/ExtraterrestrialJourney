package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongFunction;

import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Face steps from each leaf to the nearest wood, walking only through leaves: what EndLeavesBlock's decay scan
 * counts. A per-axis distance undercounts it, since a path must go around the gaps of a crown.
 */
final class LeafReach {

    private LeafReach() {}

    private static boolean isWood(EndBlock b) {
        return b == EndBlock.GROVE_LOG || b == EndBlock.HIGHLAND_LOG || b == EndBlock.LUCENT_LOG;
    }

    private static boolean isLeaf(EndBlock b) {
        return b == EndBlock.GROVE_LEAVES || b == EndBlock.GROVE_BLOSSOM
            || b == EndBlock.HIGHLAND_LEAVES
            || b == EndBlock.LUCENT_LEAVES;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (z & 0x1FFFFF) << 21) | (y & 0x1FFFFF);
    }

    /** The most steps any leaf needs, or Integer.MAX_VALUE if one cannot reach wood at all; unpack gives x, y, z. */
    static int worst(Map<Long, EndBlock> cells, LongFunction<int[]> unpack) {
        Map<Long, EndBlock> blocks = new HashMap<>();
        for (Map.Entry<Long, EndBlock> e : cells.entrySet()) {
            int[] p = unpack.apply(e.getKey());
            blocks.put(pack(p[0], p[1], p[2]), e.getValue());
        }
        // One search outwards from all the wood at once gives every leaf its nearest wood.
        Map<Long, Integer> steps = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (Map.Entry<Long, EndBlock> e : cells.entrySet()) {
            if (!isWood(e.getValue())) continue;
            int[] p = unpack.apply(e.getKey());
            steps.put(pack(p[0], p[1], p[2]), 0);
            queue.add(new int[] { p[0], p[1], p[2], 0 });
        }
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int face = 0; face < 6; face++) {
                int x = at[0] + (face == 4 ? -1 : face == 5 ? 1 : 0);
                int y = at[1] + (face == 0 ? -1 : face == 1 ? 1 : 0);
                int z = at[2] + (face == 2 ? -1 : face == 3 ? 1 : 0);
                long k = pack(x, y, z);
                EndBlock b = blocks.get(k);
                if (b == null || !isLeaf(b) || steps.containsKey(k)) continue;
                steps.put(k, at[3] + 1);
                queue.add(new int[] { x, y, z, at[3] + 1 });
            }
        }
        int worst = 0;
        for (Map.Entry<Long, EndBlock> e : blocks.entrySet()) {
            if (!isLeaf(e.getValue())) continue;
            Integer s = steps.get(e.getKey());
            worst = Math.max(worst, s == null ? Integer.MAX_VALUE : s);
        }
        return worst;
    }

    /** Unpacks a Voxels key. */
    static int[] voxel(long k) {
        return new int[] { (int) (k >> 42), (int) (k & 0xFFFFF), (int) ((k << 22) >> 42) };
    }
}
