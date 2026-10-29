package chlorine.etjourney.world.end.feature.plant;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Pure rules for crystal buds stepping through four stages. */
public final class CrystalGrowth {

    /** Chance that one random tick of a budding crystal grows a bud. */
    public static final double GROW_CHANCE = 0.2;

    private CrystalGrowth() {}

    /** The next stage after {@code current} (null is air); null when it cannot grow further. */
    public static EndBlock next(EndBlock current) {
        if (current == null) return EndBlock.CRYSTAL_BUD_SMALL;
        switch (current) {
            case CRYSTAL_BUD_SMALL:
                return EndBlock.CRYSTAL_BUD_MEDIUM;
            case CRYSTAL_BUD_MEDIUM:
                return EndBlock.CRYSTAL_BUD_LARGE;
            case CRYSTAL_BUD_LARGE:
                return EndBlock.CRYSTAL_CLUSTER;
            default:
                return null;
        }
    }

    public static boolean dropsShard(EndBlock block) {
        return block == EndBlock.CRYSTAL_CLUSTER;
    }

    /** The face a bud is attached to; facings follow ForgeDirection order (DOWN, UP, NORTH, SOUTH, WEST, EAST). */
    public static int supportSide(int facing) {
        if (facing < 0 || facing > 5) throw new IllegalArgumentException("facing out of range: " + facing);
        return facing ^ 1;
    }
}
