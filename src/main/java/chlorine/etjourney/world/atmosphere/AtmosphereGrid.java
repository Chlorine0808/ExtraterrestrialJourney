package chlorine.etjourney.world.atmosphere;

/** Atmosphere IDs on SIZE × SIZE points spaced CELL blocks apart; point (i, j) stands at block (i·CELL, j·CELL). */
public final class AtmosphereGrid {

    public static final int CELL = 32, SIZE = 5;

    /** Reads the atmosphere ID at one block column. */
    public interface PointSampler {

        byte sample(int blockX, int blockZ);
    }

    private final int originX, originZ;
    private final byte[] ids;

    /** Origin is the first point in cells; ids run x fastest. */
    public AtmosphereGrid(int originX, int originZ, byte[] ids) {
        if (ids.length != SIZE * SIZE) throw new IllegalArgumentException("need " + SIZE * SIZE + " ids");
        this.originX = originX;
        this.originZ = originZ;
        this.ids = ids;
    }

    public static int cellOf(double coord) {
        return (int) Math.floor(coord / CELL);
    }

    /** The grid centred on the cell holding (x, z). */
    public static AtmosphereGrid around(double x, double z, PointSampler sampler) {
        int ox = cellOf(x) - SIZE / 2, oz = cellOf(z) - SIZE / 2;
        byte[] ids = new byte[SIZE * SIZE];
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) ids[j * SIZE + i] = sampler.sample((ox + i) * CELL, (oz + j) * CELL);
        }
        return new AtmosphereGrid(ox, oz, ids);
    }

    /** The ID at a point given in cells; points outside the grid take the nearest edge. */
    public byte idAt(int pointX, int pointZ) {
        int i = Math.max(0, Math.min(SIZE - 1, pointX - originX));
        int j = Math.max(0, Math.min(SIZE - 1, pointZ - originZ));
        return ids[j * SIZE + i];
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public byte[] ids() {
        return ids;
    }
}
