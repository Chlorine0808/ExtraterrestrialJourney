package chlorine.etjourney.world.end.feature;

/** Picks what a cut-out holds in place of a sample's bedrock: the nearest rock beside it in the same column. */
public final class Bedrock {

    /** One column of the sample. */
    public interface Column {

        boolean bedrock(int y);

        /** Solid and not bedrock. */
        boolean solid(int y);
    }

    /** How far along the column the rock may lie. */
    private static final int REACH = 8;

    private Bedrock() {}

    /**
     * The height whose block stands in for the one at y: y itself unless it is bedrock, else the nearest rock
     * within reach toward the middle of a world height blocks high, or -1 for air.
     */
    public static int standIn(Column column, int y, int height) {
        if (!column.bedrock(y)) return y;
        int step = y < height / 2 ? 1 : -1;
        for (int k = 1; k <= REACH; k++) {
            if (column.solid(y + step * k)) return y + step * k;
        }
        return -1;
    }
}
