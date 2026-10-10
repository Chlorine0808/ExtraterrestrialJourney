package chlorine.etjourney.world.end.feature;

/**
 * Picks what a cut-out holds where its sample has bedrock or no world at all: the outermost rock of the column,
 * so a ball reaching past the floor or a ceiling is not hollow there.
 */
public final class Bedrock {

    /** One column of the sample. */
    public interface Column {

        boolean bedrock(int y);

        /** Solid and not bedrock. */
        boolean solid(int y);
    }

    /** The lowest and highest rock of a column, or -1 where it has none. */
    public static final class Rock {

        final int lowest, highest;

        Rock(int lowest, int highest) {
            this.lowest = lowest;
            this.highest = highest;
        }
    }

    private Bedrock() {}

    /** Finds the outermost rock of a column of a world height blocks high, once per column. */
    public static Rock rock(Column column, int height) {
        int lowest = -1, highest = -1;
        for (int y = 0; y < height && lowest < 0; y++) if (column.solid(y)) lowest = y;
        for (int y = height - 1; y >= 0 && highest < 0; y--) if (column.solid(y)) highest = y;
        return new Rock(lowest, highest);
    }

    /**
     * The height whose block the cut-out takes for the sample's y: y itself inside the world unless it is bedrock;
     * else, below the middle, the lowest rock; above it, the highest rock under a ceiling or air (-1) under sky.
     */
    public static int source(Column column, Rock rock, int y, int height, boolean ceiling) {
        if (y >= 0 && y < height && !column.bedrock(y)) return y;
        if (y < height / 2) return rock.lowest;
        return ceiling ? rock.highest : -1;
    }
}
