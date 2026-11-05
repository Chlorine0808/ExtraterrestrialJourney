package chlorine.etjourney.world.building;

/** A quarter-turn of a building about its anchor, clockwise seen from above (east turns to south). */
public enum Rotation {

    R0,
    R90,
    R180,
    R270;

    public int quarterTurns() {
        return ordinal();
    }

    public static Rotation of(int degrees) {
        switch (degrees) {
            case 0:
                return R0;
            case 90:
                return R90;
            case 180:
                return R180;
            case 270:
                return R270;
            default:
                throw new IllegalArgumentException("not a quarter turn: " + degrees);
        }
    }

    public static Rotation ofQuarterTurns(int turns) {
        return values()[Math.floorMod(turns, 4)];
    }

    /** Turns an offset (dx, dy, dz). */
    public int[] turn(int dx, int dy, int dz) {
        switch (this) {
            case R90:
                return new int[] { -dz, dy, dx };
            case R180:
                return new int[] { -dx, dy, -dz };
            case R270:
                return new int[] { dz, dy, -dx };
            default:
                return new int[] { dx, dy, dz };
        }
    }

    public int[] turn(int[] d) {
        return turn(d[0], d[1], d[2]);
    }

    /** Where template cell (lx, ly, lz) lands, relative to the anchor's world position. */
    public int[] toWorld(Template t, int lx, int ly, int lz) {
        return turn(lx - t.anchorX, ly - t.anchorY, lz - t.anchorZ);
    }

    /** The template cell that lands at (dx, dy, dz) from the anchor; may lie outside the template. */
    public int[] toLocal(Template t, int dx, int dy, int dz) {
        int[] d = ofQuarterTurns(-quarterTurns()).turn(dx, dy, dz);
        return new int[] { d[0] + t.anchorX, d[1] + t.anchorY, d[2] + t.anchorZ };
    }

    /** The turned footprint relative to the anchor: {minDx, minDz, maxDx, maxDz}. */
    public int[] rotatedBounds(Template t) {
        int[] a = toWorld(t, 0, 0, 0), b = toWorld(t, t.sizeX - 1, 0, t.sizeZ - 1);
        return new int[] { Math.min(a[0], b[0]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[2], b[2]) };
    }
}
