package chlorine.etjourney.world.building;

import java.util.function.IntUnaryOperator;

/** Turns a block's metadata with the building; the vanilla shapes follow the 1.7.10 data values. */
@FunctionalInterface
public interface MetaRotation {

    int rotate(int meta, Rotation rotation);

    MetaRotation NONE = (meta, r) -> meta;

    /** Builds a rotation from one clockwise quarter turn. */
    static MetaRotation quarter(IntUnaryOperator step) {
        return (meta, r) -> {
            for (int i = 0; i < r.quarterTurns(); i++) meta = step.applyAsInt(meta);
            return meta;
        };
    }

    /** Low bits stepped through a cycle of values; values outside the cycle and the other bits are kept. */
    static MetaRotation cycle(int mask, int... order) {
        return quarter(meta -> {
            int low = meta & mask;
            for (int i = 0; i < order.length; i++)
                if (order[i] == low) return (meta & ~mask) | order[(i + 1) % order.length];
            return meta;
        });
    }

    /** 0 east, 2 south, 1 west, 3 north; 4 is upside down. */
    MetaRotation STAIRS = cycle(3, 0, 2, 1, 3);
    /** Axis bits: 4 along x and 8 along z swap; upright and bark-only stay. */
    MetaRotation LOG = quarter(meta -> {
        int axis = meta & 12;
        if (axis == 4) return (meta & ~12) | 8;
        if (axis == 8) return (meta & ~12) | 4;
        return meta;
    });
    /** 2 north, 5 east, 3 south, 4 west; 0 and 1 (down, up) and bit 8 stay. */
    MetaRotation FACING_2345 = cycle(7, 2, 5, 3, 4);
    MetaRotation PISTON_DISPENSER = FACING_2345;
    /** The lower half turns, 0 east to 3 north; the upper half holds the hinge and stays. */
    MetaRotation DOOR = quarter(meta -> (meta & 8) != 0 ? meta : (meta & ~3) | ((meta + 1) & 3));
    /** Hinge side 1 north, 2 east, 0 south, 3 west; open and top bits stay. */
    MetaRotation TRAPDOOR = cycle(3, 1, 2, 0, 3);
    /** Pointing 1 east, 3 south, 2 west, 4 north; 5 standing stays. */
    MetaRotation TORCH = cycle(7, 1, 3, 2, 4);
    MetaRotation BUTTON = TORCH;
    /** Walls as torches; floor (5, 6) and ceiling (0, 7) swap axis; bit 8 is powered. */
    MetaRotation LEVER = quarter(meta -> {
        int low = meta & 7, rest = meta & 8;
        switch (low) {
            case 5:
                return rest | 6;
            case 6:
                return rest | 5;
            case 0:
                return rest | 7;
            case 7:
                return rest;
            default:
                return TORCH.rotate(meta, Rotation.R90);
        }
    });
    /** 0 south, 1 west, 2 north, 3 east, as beds, fence gates and pumpkins. */
    MetaRotation DIRECTIONAL_0123 = quarter(meta -> (meta & ~3) | ((meta + 1) & 3));
    /** Sixteen directions, 0 south, 4 west. */
    MetaRotation SIGN_POST = quarter(meta -> (meta + 4) & 15);
}
