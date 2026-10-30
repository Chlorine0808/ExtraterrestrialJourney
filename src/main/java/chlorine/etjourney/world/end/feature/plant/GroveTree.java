package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The GROVE tall tree: a straight trunk under a flattened round crown with a few blossoms inside. */
public final class GroveTree implements Grower {

    public static final GroveTree INSTANCE = new GroveTree();

    private static final int MIN_HEIGHT = 14;
    private static final int MAX_HEIGHT = 30;
    private static final int THICK_FROM = 20;

    private GroveTree() {}

    @Override
    public int radius() {
        return 8;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int cap = Math.min(MAX_HEIGHT, headroom - 2);
        if (cap < MIN_HEIGHT) return false;
        int height = MIN_HEIGHT + random.nextInt(cap - MIN_HEIGHT + 1);
        int thick = height >= THICK_FROM ? 1 : 0;
        int crown = 4 + random.nextInt(4);

        // Crown: an ellipsoid 7 tall centred 3 below the top, around the trunk's centre.
        double limit = (crown + 0.25) * (crown + 0.25);
        List<int[]> leaves = new ArrayList<>();
        for (int dy = height - 6; dy <= height; dy++) {
            double vy = (dy - (height - 3)) / 3.5;
            for (int dx = -crown; dx <= crown + thick; dx++) {
                for (int dz = -crown; dz <= crown + thick; dz++) {
                    double ox = dx - thick / 2.0;
                    double oz = dz - thick / 2.0;
                    if ((ox * ox + oz * oz) / limit + vy * vy > 1) continue;
                    boolean trunk = dx >= 0 && dx <= thick && dz >= 0 && dz <= thick && dy <= height - 2;
                    if (!trunk) leaves.add(new int[] { dx, dy, dz });
                }
            }
        }

        int blossoms = Math.max(1, Math.round(leaves.size() * (0.03f + 0.02f * random.nextFloat())));
        Collections.shuffle(leaves, random);
        for (int i = 0; i < leaves.size(); i++) {
            int[] c = leaves.get(i);
            out.put(x + c[0], y + c[1], z + c[2], i < blossoms ? EndBlock.GROVE_BLOSSOM : EndBlock.GROVE_LEAVES);
        }
        for (int dy = 0; dy <= height - 2; dy++) {
            for (int dx = 0; dx <= thick; dx++) {
                for (int dz = 0; dz <= thick; dz++) {
                    out.put(x + dx, y + dy, z + dz, EndBlock.GROVE_LOG);
                }
            }
        }
        return true;
    }
}
