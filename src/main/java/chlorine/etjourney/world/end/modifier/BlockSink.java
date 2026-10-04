package chlorine.etjourney.world.end.modifier;

/** Block-level output for one chunk; coordinates are world block coordinates. */
public interface BlockSink {

    int originX();

    int originZ();

    /** Writable heights are minY to maxY - 1: 0-127 for the generator's array, 128-255 for the tall pass. */
    int minY();

    int maxY();

    boolean isAir(int x, int y, int z);

    /** Places a block only into air. */
    void place(int x, int y, int z, EndBlock block);

    /** Overwrites whatever is there. */
    void set(int x, int y, int z, EndBlock block);

    void clear(int x, int y, int z);
}
