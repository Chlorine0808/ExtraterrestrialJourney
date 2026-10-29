package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The AMBER lightroot: roots crawling over the ground around a glowing seed ball on a short stalk. */
public final class Lightroot implements Grower {

    public static final Lightroot INSTANCE = new Lightroot();

    private static final int MIN_ROOTS = 4;
    private static final int MAX_ROOTS = 7;
    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 8;

    private Lightroot() {}

    @Override
    public int radius() {
        return MAX_LENGTH;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int ball = 1 + random.nextInt(2);
        if (headroom - 1 - ball < 2) ball = 1;
        int centre = Math.min(2 + random.nextInt(3), headroom - 1 - ball);
        if (centre < 2) return false;

        List<int[]> roots = new ArrayList<>();
        List<int[]> tips = new ArrayList<>();
        // The stalk lifts the ball off the ground.
        for (int dy = 0; dy < centre - ball; dy++) {
            roots.add(new int[] { 0, dy, 0 });
        }
        int count = MIN_ROOTS + random.nextInt(MAX_ROOTS - MIN_ROOTS + 1);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double angle = start + Math.PI * 2 * i / count + (random.nextDouble() - 0.5) * 0.6;
            int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
            double px = 0, pz = 0;
            int[] tip = null;
            for (int s = 1; s <= length; s++) {
                angle += (random.nextDouble() - 0.5) * 0.4;
                px += Math.cos(angle);
                pz += Math.sin(angle);
                tip = new int[] { (int) Math.round(px), random.nextInt(5) == 0 ? 1 : 0, (int) Math.round(pz) };
                roots.add(tip);
            }
            tips.add(tip);
        }

        // Seeds first, so that a sink which refuses non-air cells still gets whole roots.
        for (int dx = -ball; dx <= ball; dx++) {
            for (int dy = -ball; dy <= ball; dy++) {
                for (int dz = -ball; dz <= ball; dz++) {
                    if (dx * dx + dy * dy + dz * dz > ball * ball + 1) continue;
                    putSeed(out, roots, x, y, z, dx, centre + dy, dz);
                }
            }
        }
        for (int[] tip : tips) {
            if (random.nextBoolean()) putSeed(out, roots, x, y, z, tip[0], tip[1] + 1, tip[2]);
        }
        for (int[] c : roots) {
            out.put(x + c[0], y + c[1], z + c[2], EndBlock.AMBER_ROOT);
        }
        return true;
    }

    private static void putSeed(PlantSink out, List<int[]> roots, int x, int y, int z, int dx, int dy, int dz) {
        for (int[] c : roots) {
            if (c[0] == dx && c[1] == dy && c[2] == dz) return;
        }
        out.put(x + dx, y + dy, z + dz, EndBlock.AMBER_SEED);
    }
}
