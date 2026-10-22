package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/** Springs set into the cliffs at continent rims, so water pours off the edge into the void. */
public final class Falls {

    public static final int ATTEMPTS = 6;
    /** How far below the rim top the spring sits, and how much open void the face must have below it. */
    private static final int INSET = 3, DROP = 24;
    private static final int[][] SIDES = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private Falls() {}

    /** Top solid Y of a column, or -1 when the column is empty. */
    public interface Ground {

        int top(int x, int z);
    }

    /** Whether a chunk sets springs at the VOID_FALLS weight there, thinning out across the border. */
    public static boolean forms(long seed, int chunkX, int chunkZ, double weight) {
        return Fade.forms(weight, Hash.hash01(seed ^ 0x3A7F1C5E9B2D4068L, chunkX, chunkZ));
    }

    /**
     * A spring position {x, y, z} for one attempt in the 16x16 area at (originX, originZ), or null. The spring is a
     * rim block whose outward neighbour is open void for at least DROP blocks below it.
     */
    public static int[] spring(long seed, int originX, int originZ, int attempt, Ground ground) {
        long s = seed ^ 0x5D1E3B7A9C2F6048L;
        int x = originX + (int) (16 * Hash.hash01(s + attempt * 2L, originX, originZ));
        int z = originZ + (int) (16 * Hash.hash01(s + attempt * 2L + 1, originX, originZ));
        int top = ground.top(x, z);
        if (top < DROP + INSET) return null;
        int y = top - INSET;
        for (int[] side : SIDES) {
            int nx = x + side[0], nz = z + side[1];
            // Open void beside the spring and below it: the next column's top is far down or missing.
            if (ground.top(nx, nz) < y - DROP && ground.top(nx + side[0], nz + side[1]) < y - DROP) {
                return new int[] { x, y, z };
            }
        }
        return null;
    }
}
