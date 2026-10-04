package chlorine.etjourney.world.end.modifier;

/** Block-level output for one chunk; coordinates are world block coordinates. */
public interface BlockSink {

    int originX();

    int originZ();

    /** Height of the writable column (128 for the generator's array). */
    int height();

    boolean isAir(int x, int y, int z);

    /** Places a block only into air. */
    void place(int x, int y, int z, EndBlock block);

    /** Overwrites whatever is there. */
    void set(int x, int y, int z, EndBlock block);

    void clear(int x, int y, int z);
}
