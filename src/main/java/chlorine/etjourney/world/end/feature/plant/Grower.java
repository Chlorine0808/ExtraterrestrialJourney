package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

/** One kind of large plant. */
public interface Grower {

    /** Face steps through leaves within which every leaf a plant grows reaches its wood; farther leaves decay. */
    int LEAF_REACH = 12;

    /** Horizontal reach of the plant from its base, in blocks. */
    int radius();

    /**
     * Grows the plant at (x, y, z), the air just above the surface; {@code headroom} counts the air blocks from y
     * upward. Writes nothing and returns false if the plant does not fit.
     */
    boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out);
}
