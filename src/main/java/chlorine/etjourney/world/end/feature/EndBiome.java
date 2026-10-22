package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Surface looks of the continent and its styles, drawn on a map of their own apart from styles and zones. */
public enum EndBiome {

    GROVE(EndBlock.GROVE_TOP, EndBlock.GROVE_FILL),
    HIGHLAND(EndBlock.HIGHLAND_TOP, EndBlock.HIGHLAND_FILL),
    MYCELIA(EndBlock.MYCELIA_TOP, EndBlock.MYCELIA_FILL),
    AMBER(EndBlock.AMBER_TOP, EndBlock.AMBER_FILL);

    private static final double CELL = 576;
    private static final long SALT = 0x5D1E7A3C29B4F086L;
    private static final EndBiome[] ALL = values();

    public final EndBlock top, fill;

    EndBiome(EndBlock top, EndBlock fill) {
        this.top = top;
        this.fill = fill;
    }

    public static EndBiome at(long seed, double x, double z) {
        return ALL[CellPick.at(seed, SALT, CELL, ALL.length, x, z)];
    }
}
