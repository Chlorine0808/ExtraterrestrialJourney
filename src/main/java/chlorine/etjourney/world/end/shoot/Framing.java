package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.debug.EtjArgs;
import chlorine.etjourney.world.end.feature.EndBiome;

/** Chooses camera placements for a target and how each retry differs from the last. */
public final class Framing {

    public static final int MAX_ATTEMPTS = 3;
    /** Aerial tries: three at the usual pitch, then three steep ones between their headings. */
    public static final int AERIAL_ATTEMPTS = 6;
    public static final double EYE = 1.62;
    private static final double AERIAL_PITCH = 35, STEEP_PITCH = 60, OUTSIDE_PITCH = 20, GROUND_PITCH = 8;
    private static final int GROUND_NEAR = 16, GROUND_FAR = 96, GROUND_STEP = 8;
    /** Farthest ground from the expected top that still counts as the continent's, not a pad in the void. */
    public static final double GROUND_REACH = 64;
    /** How far the second ground spot stands from the target, along the best look. */
    public static final double GROUND_SHIFT = 24;

    /** Turns tried away from the +x stand, nearest first. */
    private static final float[] TREE_TURNS = { 0, 45, -45, 90, -90, 135, -135, 180 };
    /** A tree's aerial tries: four around it at this pitch, then four steeper ones between them. */
    private static final double TREE_AERIAL_PITCH = 50, TREE_STEEP_PITCH = 65;

    /**
     * How a tree's ground view stands off: how far, how high above the root, where on the tree it aims, and how far
     * the ground in between may fall below the root or rise above it. Reach is how far the crown spreads.
     */
    public static final class TreeFrame {

        public final double stand, rise, aim, drop, climb, reach;

        TreeFrame(double stand, double rise, double aim, double drop, double climb, double reach) {
            this.stand = stand;
            this.rise = rise;
            this.aim = aim;
            this.drop = drop;
            this.climb = climb;
            this.reach = reach;
        }
    }

    /** A seed tree is about 23 high with a 12-block reach; 28 off, the whole of it fits the view. */
    public static final TreeFrame SEED_TREE = new TreeFrame(28, 4, 10, 8, 10, 12);
    /** A giant tree is 56 high with a 26-block reach; 45 off, the whole of it fits the 70-degree view. */
    public static final TreeFrame GIANT_TREE = new TreeFrame(45, 6, 26, 12, 20, 26);

    public interface BiomeAt {

        EndBiome at(double x, double z);
    }

    /** The topmost ground's y; -100 or lower where there is none. */
    public interface GroundAt {

        double at(double x, double z);
    }

    private Framing() {}

    public static double subjectDistance(Target t) {
        return Math.max(48, Math.min(400, t.size * 2.5));
    }

    public static Camera aerial(Target t, int attempt) {
        if (attempt >= MAX_ATTEMPTS)
            return looking(t, subjectDistance(t), turn(attempt - MAX_ATTEMPTS) + 45f, (float) STEEP_PITCH);
        return looking(t, subjectDistance(t) * Math.pow(1.25, attempt), turn(attempt), (float) AERIAL_PITCH);
    }

    public static Camera outside(Target ball, int attempt) {
        return looking(ball, ball.size * 2.6, turn(attempt), (float) OUTSIDE_PITCH);
    }

    /** The eight compass yaws, those whose forward line crosses most of the wanted biome first. */
    public static List<Float> groundYaws(BiomeAt biomes, double x, double z, EndBiome want) {
        List<Float> yaws = new ArrayList<>();
        int[] hits = new int[8];
        for (int i = 0; i < 8; i++) {
            yaws.add(i * 45f);
            double[] d = EtjArgs.lookDirection(i * 45f, 0);
            for (int r = GROUND_NEAR; r <= GROUND_FAR; r += GROUND_STEP)
                if (biomes.at(x + d[0] * r, z + d[2] * r) == want) hits[i]++;
        }
        yaws.sort(
            (a, b) -> hits[Math.round(b / 45f)] != hits[Math.round(a / 45f)]
                ? hits[Math.round(b / 45f)] - hits[Math.round(a / 45f)]
                : Float.compare(a, b));
        return yaws;
    }

    /** Columns {x, z} to look for ground in: the target's own, then rings 8 and 16 blocks out in 8 directions. */
    public static List<double[]> groundColumns(double x, double z) {
        List<double[]> cols = new ArrayList<>();
        cols.add(new double[] { x, z });
        for (int r = 8; r <= 16; r += 8) {
            for (int i = 0; i < 8; i++) {
                double[] d = EtjArgs.lookDirection(i * 45f, 0);
                cols.add(new double[] { x + d[0] * r, z + d[2] * r });
            }
        }
        return cols;
    }

    /**
     * Index of the floor (NaN for none) nearest wantY within GROUND_REACH, the earlier on a tie; -1 when there is
     * none.
     */
    public static int pickGround(double wantY, double[] floors) {
        int best = -1;
        for (int i = 0; i < floors.length; i++) {
            if (Double.isNaN(floors[i]) || Math.abs(floors[i] - wantY) > GROUND_REACH) continue;
            if (best < 0 || Math.abs(floors[i] - wantY) < Math.abs(floors[best] - wantY)) best = i;
        }
        return best;
    }

    /** Columns {x, z} to stand a ground view on: the target's own, then one further along the best look. */
    public static List<double[]> groundSpots(double x, double z, float bestYaw) {
        double[] d = EtjArgs.lookDirection(bestYaw, 0);
        List<double[]> spots = new ArrayList<>();
        spots.add(new double[] { x, z });
        spots.add(new double[] { x + d[0] * GROUND_SHIFT, z + d[2] * GROUND_SHIFT });
        return spots;
    }

    public static Camera ground(double x, double groundTop, double z, float yaw) {
        return new Camera(x, groundTop, z, yaw, (float) GROUND_PITCH);
    }

    /** A seed tree's first ground view; see treeGrounds. */
    public static Camera treeGround(Target t, GroundAt ground) {
        return treeGrounds(t, ground, SEED_TREE).get(0);
    }

    /**
     * A tree's ground views, one per turn away from the +x stand, raised above the ground in between and looking
     * back at the tree. Turns whose ground will do come first, nearest first; the rest follow at the root's height.
     */
    public static List<Camera> treeGrounds(Target t, GroundAt ground, TreeFrame f) {
        List<Camera> good = new ArrayList<>(), rest = new ArrayList<>();
        for (float turn : TREE_TURNS) {
            float yaw = 90f + turn;
            double top = highestGround(t, ground, yaw, f);
            if (!Double.isNaN(top) && top <= t.y + f.climb) good.add(standOff(t, yaw, top, f));
            else rest.add(standOff(t, yaw, Double.NaN, f));
        }
        good.addAll(rest);
        return good;
    }

    /** A tree's aerial views of aim from dist blocks: four around it, then four steeper ones between them. */
    public static List<Camera> treeAerials(Target aim, double dist) {
        List<Camera> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) out.add(looking(aim, dist, 90f + turn(i), (float) TREE_AERIAL_PITCH));
        for (int i = 0; i < 4; i++) out.add(looking(aim, dist, 135f + turn(i), (float) TREE_STEEP_PITCH));
        return out;
    }

    /** The highest ground on the line from the tree to its view, NaN when any of it is missing or falls away. */
    private static double highestGround(Target t, GroundAt ground, float yaw, TreeFrame f) {
        double[] d = EtjArgs.lookDirection(yaw, 0);
        double top = Double.NEGATIVE_INFINITY;
        for (int r = 1; r <= f.stand; r++) {
            double g = ground.at(t.x - d[0] * r, t.z - d[2] * r);
            if (g <= -100 || g < t.y - f.drop) return Double.NaN;
            top = Math.max(top, g);
        }
        return top;
    }

    private static Camera standOff(Target t, float yaw, double groundTop, TreeFrame f) {
        double[] d = EtjArgs.lookDirection(yaw, 0);
        double feet = t.y + f.rise;
        if (!Double.isNaN(groundTop)) feet = Math.max(feet, Math.floor(groundTop) + 3);
        float pitch = (float) Math.toDegrees(Math.atan2(feet + EYE - (t.y + f.aim), f.stand));
        return new Camera(t.x - d[0] * f.stand, feet, t.z - d[2] * f.stand, yaw, pitch);
    }

    /** Whether a view whose ray hit at hitDistance (NaN when it hit nothing) is not blocked in front of its subject. */
    public static boolean clear(double hitDistance, double subjectDistance, View view) {
        if (Double.isNaN(hitDistance)) return true;
        if (view == View.AERIAL || view == View.OUTSIDE)
            return hitDistance >= subjectDistance - Math.max(8, 0.15 * subjectDistance);
        return hitDistance >= 12;
    }

    private static float turn(int attempt) {
        return attempt * 90f;
    }

    /** A camera dist blocks from t along the reverse of the view, so that the view centres on t. */
    private static Camera looking(Target t, double dist, float yaw, float pitch) {
        double[] d = EtjArgs.lookDirection(yaw, pitch);
        return new Camera(t.x - d[0] * dist, t.y - d[1] * dist - EYE, t.z - d[2] * dist, yaw, pitch);
    }
}
