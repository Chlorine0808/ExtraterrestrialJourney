package chlorine.etjourney.world.end.feature;

/**
 * 16 x 16 silhouettes of tools for the giant tool structures, drawn for this mod. Row 0 is the top; the handle runs
 * from the bottom left to the head at the top right.
 */
public enum ToolSprite {

    PICKAXE(false, "................", ".....#######....", "....#########...", "...###.....####.", "...........#####",
        "..........###.##", ".........###..##", "........###...##", ".......###....##", "......###.....##",
        ".....###.......#", "....###.........", "...###..........", "..###...........", ".###............",
        "###............."),
    AXE(false, "................", "........####....", ".......#######..", "......#########.", "......#########.",
        ".......########.", "......###.#####.", ".....###...###..", "....###.........", "...###..........",
        "..###...........", ".###............", "###.............", "##..............", "##..............",
        "##.............."),
    HOE(false, "................", ".........######.", "........#######.", "........###..##.", ".......###......",
        "......###.......", ".....###........", "....###.........", "...###..........", "..###...........",
        ".###............", "###.............", "##..............", "##..............", "##..............",
        "##.............."),
    SWORD(true, ".............###", "............####", "...........####.", "..........####..", ".........####...",
        "........####....", ".......####.....", "......####......", "..#..####.......", "..######........",
        "...####.........", "...####.........", "..##..##........", ".##.............", "##..............",
        "#..............."),
    SHOVEL(false, "............##..", "...........####.", "..........######", "..........######", "...........####.",
        "..........###...", ".........###....", "........###.....", ".......###......", "......###.......",
        ".....###........", "....###.........", "...###..........", "..###...........", ".###............",
        "###............."),
    BOW(false, "................", ".........######.", ".......#######..", "......####..#...", "....####...#....",
        "....###...#.....", "...###...#......", "..###...#.......", "..##...#........", ".###..#.........",
        ".##..#..........", ".##.#...........", ".###............", ".##.............", ".#..............",
        "................"),
    HAMMER(false, ".........###....", "........#####...", ".......#######..", "......#########.", ".......#########",
        "........#######.", ".......###.###..", "......###...#...", ".....###........", "....###.........",
        "...###..........", "..###...........", ".###............", "###.............", "##..............",
        "##.............."),
    PAXEL(false, "................", ".....#######....", "....#########...", "...###.######...", "......########..",
        "......#######.#.", ".....###.####.##", "....###...##..##", "...###........##", "..###.........##",
        ".###...........#", "###.............", "##..............", "##..............", "##..............",
        "##.............."),
    WRENCH(false, "...........##.##", "..........###.##", "..........###.##", "..........######", ".........######.",
        "........###.....", ".......###......", "......###.......", ".....###........", "....###.........",
        "...###..........", "..###...........", ".#####..........", "##..##..........", "##..##..........",
        ".####...........");

    /** The thickest a tool gets, in pixels, so a broad axe blade stays a blade rather than a block. */
    public static final double MAX_THICKNESS = 3;

    /** Whether the tool is driven in tip first, as a sword is, rather than by its handle. */
    public final boolean tipFirst;
    private final boolean[][] filled = new boolean[16][16];
    private final double[][] thickness;

    ToolSprite(boolean tipFirst, String... rows) {
        this.tipFirst = tipFirst;
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) filled[col][row] = rows[row].charAt(col) == '#';
        }
        thickness = localThickness();
    }

    /** Thickness of a pixel in pixels: the width of the widest disc inside the sprite that covers it, capped. */
    public double thickness(int col, int row) {
        return filled(col, row) ? thickness[col][row] : 0;
    }

    private double[][] localThickness() {
        // The radius of the widest disc centred on each pixel, measured to the nearest empty pixel's centre.
        double[][] radius = new double[16][16];
        for (int col = 0; col < 16; col++) {
            for (int row = 0; row < 16; row++) {
                if (!filled[col][row]) continue;
                double nearest = Double.MAX_VALUE;
                for (int c = -1; c <= 16; c++) {
                    for (int r = -1; r <= 16; r++) {
                        if (!filled(c, r)) nearest = Math.min(nearest, Math.hypot(c - col, r - row));
                    }
                }
                radius[col][row] = nearest - 0.5;
            }
        }
        double[][] out = new double[16][16];
        for (int col = 0; col < 16; col++) {
            for (int row = 0; row < 16; row++) {
                if (!filled[col][row]) continue;
                double widest = 0;
                for (int c = 0; c < 16; c++) {
                    for (int r = 0; r < 16; r++) {
                        if (filled[c][r] && Math.hypot(c - col, r - row) <= radius[c][r] + 0.5)
                            widest = Math.max(widest, 2 * radius[c][r]);
                    }
                }
                out[col][row] = Math.min(widest, MAX_THICKNESS);
            }
        }
        return out;
    }

    public boolean filled(int col, int row) {
        return col >= 0 && col < 16 && row >= 0 && row < 16 && filled[col][row];
    }

    /** {col, row} of the filled pixel nearest the planted corner: bottom left, or top right when tip first. */
    public int[] planted() {
        int cornerCol = tipFirst ? 15 : 0, cornerRow = tipFirst ? 0 : 15;
        int[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int col = 0; col < 16; col++) {
            for (int row = 0; row < 16; row++) {
                double d = Math.hypot(col - cornerCol, row - cornerRow);
                if (filled[col][row] && d < bestDistance) {
                    bestDistance = d;
                    best = new int[] { col, row };
                }
            }
        }
        return best;
    }
}
