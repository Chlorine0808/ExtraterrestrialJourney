package chlorine.etjourney.world.end.noise;

/** splitmix64-based lattice hashes in [0, 1). */
public final class Hash {

    private Hash() {}

    public static double hash01(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        // The full finaliser: a single multiply left neighbouring seeds strongly correlated.
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) / (double) (1L << 53);
    }

    public static double hash01(long seed, int x, int y, int z) {
        return hash01(seed + y * 0x9E3779B97F4A7C15L, x, z);
    }
}
