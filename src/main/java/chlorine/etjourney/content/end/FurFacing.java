package chlorine.etjourney.content.end;

/** Which way a fur grows: away from the leaf it clings to, upright when it sits on one. */
final class FurFacing {

    interface Leaves {

        boolean at(int dx, int dy, int dz);
    }

    private static final int[] UP = { 0, 1, 0 }, DOWN = { 0, -1, 0 };
    private static final int[][] SIDES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

    private FurFacing() {}

    /** The direction the fur points, as a unit offset; a leaf below wins, then one beside, then one above. */
    static int[] outward(Leaves leaves) {
        if (leaves.at(0, -1, 0)) return UP;
        for (int[] s : SIDES) if (leaves.at(s[0], s[1], s[2])) return new int[] { -s[0], -s[1], -s[2] };
        if (leaves.at(0, 1, 0)) return DOWN;
        return UP;
    }
}
