package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The AMBER tall stem: a single column of stem, cut short by the headroom. */
public final class TallStem implements Grower {

    public static final TallStem INSTANCE = new TallStem();

    private static final int MIN_HEIGHT = 6;
    private static final int MAX_HEIGHT = 14;
    private static final int MIN_FIT = 4;

    private TallStem() {}

    @Override
    public int radius() {
        return 0;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int cap = Math.min(MAX_HEIGHT, headroom - 1);
        if (cap < MIN_FIT) return false;
        int low = Math.min(MIN_HEIGHT, cap);
        int height = low + random.nextInt(cap - low + 1);
        for (int dy = 0; dy < height; dy++) {
            out.put(x, y + dy, z, EndBlock.AMBER_STEM);
        }
        return true;
    }
}
