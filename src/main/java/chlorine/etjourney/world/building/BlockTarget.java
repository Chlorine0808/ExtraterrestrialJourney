package chlorine.etjourney.world.building;

/** Where a building is written; turning the block's metadata is left to the target. */
public interface BlockTarget {

    void set(int x, int y, int z, BlockRef ref, Rotation rotation);

    void setAir(int x, int y, int z);

    /** Whether a foundation may fill (x, y, z); the first position that is not stops the column. */
    boolean replaceable(int x, int y, int z);

    /** Whether the block hangs on a neighbour (a torch, a lever); these are written after the rest of the window. */
    default boolean attachable(BlockRef ref) {
        return false;
    }
}
