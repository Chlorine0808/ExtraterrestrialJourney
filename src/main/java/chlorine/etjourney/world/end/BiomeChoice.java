package chlorine.etjourney.world.end;

/** Turns a biosphere's pick into a position in the biome list. */
final class BiomeChoice {

    private BiomeChoice() {}

    /** The index pick selects among size entries, or -1 when there are none. */
    static int index(int size, double pick) {
        if (size <= 0) return -1;
        return Math.max(0, Math.min(size - 1, (int) Math.floor(pick * size)));
    }
}
