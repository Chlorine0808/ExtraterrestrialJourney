package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The HIGHLAND twisted tree: a trunk that shifts up to one block sideways per level, under a small crown. */
public final class TwistedTree implements Grower {

    public static final TwistedTree INSTANCE = new TwistedTree();

    private static final int MIN_HEIGHT = 5;
    private static final int MAX_HEIGHT = 9;
    private static final int MAX_DRIFT = 3;
    private static final int MAX_CROWN = 3;

    private TwistedTree() {}

    @Override
    public int radius() {
        return MAX_DRIFT + MAX_CROWN;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int cap = Math.min(MAX_HEIGHT, headroom - 2);
        if (cap < MIN_HEIGHT) return false;
        int height = MIN_HEIGHT + random.nextInt(cap - MIN_HEIGHT + 1);
        int crown = 2 + random.nextInt(MAX_CROWN - 1);

        // A level that shifts also keeps the cell below it, so the trunk stays face-connected.
        List<int[]> trunk = new ArrayList<>();
        int ox = 0, oz = 0;
        for (int dy = 0; dy < height; dy++) {
            trunk.add(new int[] { ox, dy, oz });
            if (dy == 0 || random.nextInt(3) == 0) continue;
            int step = random.nextBoolean() ? 1 : -1;
            int nx = ox, nz = oz;
            if (random.nextBoolean()) nx += step;
            else nz += step;
            if (Math.abs(nx) > MAX_DRIFT || Math.abs(nz) > MAX_DRIFT) continue;
            ox = nx;
            oz = nz;
            trunk.add(new int[] { ox, dy, oz });
        }

        int top = height - 1;
        for (int dy = top - 1; dy <= top + 2; dy++) {
            int r = Math.max(0, dy <= top ? crown : crown - (dy - top));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    if (!isTrunk(trunk, ox + dx, dy, oz + dz)) {
                        out.put(x + ox + dx, y + dy, z + oz + dz, EndBlock.HIGHLAND_LEAVES);
                    }
                }
            }
        }
        for (int[] c : trunk) {
            out.put(x + c[0], y + c[1], z + c[2], EndBlock.HIGHLAND_LOG);
        }
        return true;
    }

    private static boolean isTrunk(List<int[]> trunk, int dx, int dy, int dz) {
        for (int[] c : trunk) {
            if (c[0] == dx && c[1] == dy && c[2] == dz) return true;
        }
        return false;
    }
}
