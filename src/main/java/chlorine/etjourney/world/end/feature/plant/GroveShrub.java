package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The GROVE shrub: a short stem under a small round bush of leaves. */
public final class GroveShrub implements Grower {

    public static final GroveShrub INSTANCE = new GroveShrub();

    private static final int MIN_HEIGHT = 2;
    private static final int MAX_HEIGHT = 4;

    private GroveShrub() {}

    @Override
    public int radius() {
        return 2;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int cap = Math.min(MAX_HEIGHT, headroom - 1);
        if (cap < MIN_HEIGHT) return false;
        int height = MIN_HEIGHT + random.nextInt(cap - MIN_HEIGHT + 1);
        int bush = 1 + random.nextInt(2);

        // Bush: an ellipsoid 3 tall centred one below the top; the stem cells are left to the stem.
        double limit = (bush + 0.25) * (bush + 0.25);
        for (int dy = height - 2; dy <= height; dy++) {
            double vy = dy - (height - 1);
            for (int dx = -bush; dx <= bush; dx++) {
                for (int dz = -bush; dz <= bush; dz++) {
                    if ((dx * dx + dz * dz) / limit + vy * vy / 1.5 > 1) continue;
                    if (dx == 0 && dz == 0 && dy <= height - 2) continue;
                    out.put(x + dx, y + dy, z + dz, EndBlock.GROVE_LEAVES);
                }
            }
        }
        for (int dy = 0; dy <= height - 2; dy++) {
            out.put(x, y + dy, z, EndBlock.GROVE_LOG);
        }
        return true;
    }
}
