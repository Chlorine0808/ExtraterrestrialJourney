package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Shoals: schools of tiny arrowhead platforms all pointing the same way, like a school of fish to hop across. They
 * fill SHOALS regions and the thinned ring around HEE islands.
 */
public final class Shoals {

    public static final int CELL = 43;
    private static final double CHANCE = 0.9;
    /** How far a platform strays along the heading; sideways and vertical spreads are Gaussian. */
    public static final double EXTENT = 32;
    private static final double SIDE_SIGMA = 5, VERTICAL_SIGMA = 4;
    private static final double MIN_Y = 30, MAX_Y = 110;
    private static final double STEEP_CHANCE = 0.35;
    private static final long SALT = 0x2B992DDFA23249D6L;
    private static final CellCache<School> CELLS = new CellCache<>(16384);

    private Shoals() {}

    public interface Probe {

        /** True inside a SHOALS region. */
        boolean dense(double x, double z);

        List<ZoneIslands.Island> zoneIslandsNear(double x, double z);
    }

    public static final class School {

        public final double x, z, y, angle, pitch;
        public final int count;
        /** True in a SHOALS region; otherwise the school only forms beside an HEE island. */
        public final boolean always;
        final long seed;

        School(double x, double z, double y, double angle, double pitch, int count, boolean always, long seed) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.angle = angle;
            this.pitch = pitch;
            this.count = count;
            this.always = always;
            this.seed = seed;
        }
    }

    public static List<School> near(long seed, double x, double z, double range, Probe probe) {
        List<School> out = new ArrayList<>();
        double reach = range + EXTENT + 4 * SIDE_SIGMA;
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                School school = CELLS.get(seed, cx, cz, (s, i, j) -> compute(s, i, j, probe));
                if (school != null) out.add(school);
            }
        }
        return out;
    }

    private static School compute(long seed, int cx, int cz, Probe probe) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        double x = (cx + Hash.hash01(s + 1, cx, cz)) * CELL;
        double z = (cz + Hash.hash01(s + 2, cx, cz)) * CELL;
        if (Math.hypot(x, z) < 1000) return null;
        // Stay out of zone islands, which share the open void.
        for (ZoneIslands.Island island : probe.zoneIslandsNear(x, z)) {
            if (Math.hypot(x - island.x, z - island.z) < island.radius * 1.3 + EXTENT) return null;
        }
        boolean always = probe.dense(x, z);
        double y = MIN_Y + (MAX_Y - MIN_Y) * Hash.hash01(s + 3, cx, cz);
        double angle = Hash.hash01(s + 4, cx, cz) * Math.PI * 2;
        double pitch = 0;
        if (Hash.hash01(s + 6, cx, cz) < STEEP_CHANCE) {
            double tilt = (50 + 35 * Hash.hash01(s + 7, cx, cz)) * Math.PI / 180;
            pitch = Hash.hash01(s + 8, cx, cz) < 0.5 ? tilt : -tilt;
        }
        int count = 50 + (int) (70 * Hash.hash01(s + 5, cx, cz));
        long schoolSeed = s ^ ((long) cx * 0x9E3779B97F4A7C15L) ^ ((long) cz * 0xC2B2AE3D27D4EB4FL);
        return new School(x, z, y, angle, pitch, count, always, schoolSeed);
    }

    /**
     * Whether a school forms: never reaching into a reserved area's island, otherwise when it always forms or sits in
     * the thinned ring around a reserved area. reserved must be the list for the school's own centre chunk, so every
     * chunk it spans agrees.
     */
    public static boolean forms(School school, List<Area> reserved) {
        if (Reservations.insideFootprint(reserved, school.x, school.z, EXTENT + 4 * SIDE_SIGMA)) return false;
        if (school.always) return true;
        for (Area area : reserved) {
            if (Math.hypot(school.x - area.x, school.z - area.z) < area.radius * 1.15 + Reservations.FADE + 30) {
                return true;
            }
        }
        return false;
    }

    /**
     * Platform blocks {x, y, z}: arrowheads 2 or 3 rows long (now and then 4 or 5), all facing the heading. Level
     * schools lay them flat, pointing along the heading snapped to an axis; steep schools stand them upright.
     */
    public static List<int[]> blocks(School school) {
        List<int[]> out = new ArrayList<>();
        Random r = new Random(school.seed);
        double cos = Math.cos(school.angle), sin = Math.sin(school.angle);
        double up = Math.sin(school.pitch), flat = Math.cos(school.pitch);
        boolean upright = school.pitch != 0;
        int dirX = Math.abs(cos) >= Math.abs(sin) ? (int) Math.signum(cos) : 0;
        int dirZ = dirX == 0 ? (int) Math.signum(sin) : 0;
        int tipY = school.pitch > 0 ? 1 : -1;
        for (int k = 0; k < school.count; k++) {
            double along = (r.nextDouble() - 0.5) * 2 * EXTENT * 0.9;
            double across = r.nextGaussian() * SIDE_SIGMA;
            int px = (int) Math.floor(school.x + along * flat * cos - across * sin);
            int pz = (int) Math.floor(school.z + along * flat * sin + across * cos);
            int py = (int) Math.floor(school.y + along * up + r.nextGaussian() * VERTICAL_SIGMA);
            double roll = r.nextDouble();
            int rows = roll < 0.05 ? 5 : roll < 0.12 ? 4 : roll < 0.56 ? 3 : 2;
            for (int row = 0; row < rows; row++) {
                for (int side = -row; side <= row; side++) {
                    if (upright) out.add(new int[] { px + side * dirZ, py - row * tipY, pz + side * dirX });
                    else out.add(new int[] { px - row * dirX + side * dirZ, py, pz - row * dirZ + side * dirX });
                }
            }
        }
        return out;
    }
}
