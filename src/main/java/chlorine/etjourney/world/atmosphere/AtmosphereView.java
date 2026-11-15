package chlorine.etjourney.world.atmosphere;

/** The client's latest atmosphere grid and the dimension it belongs to. */
public final class AtmosphereView {

    private static volatile Snapshot snapshot;

    private AtmosphereView() {}

    private static final class Snapshot {

        final int dimension;
        final AtmosphereGrid grid;

        Snapshot(int dimension, AtmosphereGrid grid) {
            this.dimension = dimension;
            this.grid = grid;
        }
    }

    static void set(int dimension, AtmosphereGrid grid) {
        snapshot = new Snapshot(dimension, grid);
    }

    /** The grid for this dimension, or null when the client has none for it. */
    public static AtmosphereGrid grid(int dimension) {
        Snapshot s = snapshot;
        return s != null && s.dimension == dimension ? s.grid : null;
    }

    public static void clear() {
        snapshot = null;
    }
}
