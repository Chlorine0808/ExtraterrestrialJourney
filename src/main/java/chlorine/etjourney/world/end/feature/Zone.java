package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The four zones of the outer End; each zone island takes the zone of its centre. */
public enum Zone {

    SOLAR(EndBlock.SOLAR_TOP, EndBlock.SOLAR_FILL),
    VORTEX(EndBlock.VORTEX_TOP, EndBlock.VORTEX_FILL),
    NEBULA(EndBlock.NEBULA_TOP, EndBlock.NEBULA_FILL),
    STARDUST(EndBlock.STARDUST_TOP, EndBlock.STARDUST_FILL);

    private static final double CELL = 640;
    private static final long SALT = 0x632BE59BD9B4E019L;
    private static final Zone[] ALL = values();

    public final EndBlock top, fill;

    Zone(EndBlock top, EndBlock fill) {
        this.top = top;
        this.fill = fill;
    }

    public static Zone at(long seed, double x, double z) {
        return ALL[CellPick.at(seed, SALT, CELL, ALL.length, x, z)];
    }
}
