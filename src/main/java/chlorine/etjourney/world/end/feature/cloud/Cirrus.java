package chlorine.etjourney.world.end.feature.cloud;

import java.util.List;

import chlorine.etjourney.world.end.feature.Fade;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;
import chlorine.etjourney.world.end.region.RegionMap;

/** CIRRUS: high streaks one or two blocks thick, running along the wind of their region. */
public final class Cirrus {

    private static final long SALT = 0x2E7B4D9A1C5F3068L;
    /** Ridged value above which a streak forms at full weight. */
    private static final double THRESHOLD = 0.75;

    private Cirrus() {}

    public static void sheets(long seed, double x, double z, double ground, double weight, List<Layer> out) {
        double fade = Fade.of(weight);
        if (fade <= 0) return;
        long s = seed ^ SALT;
        // The wind of the region with the most weight here. Measured from that region's centre, the rotated
        // coordinates stay a few hundred blocks wide, so the streaks hold together far from the origin.
        RegionMap.CellWeight cell = dominant(seed, x, z);
        double[] c = RegionMap.cellCentre(seed, cell.cx, cell.cz);
        double wind = wind(seed, cell.cx, cell.cz);
        // A broad warp bends the streaks.
        double[] w = Warp.warp(s + 1, x, z, 60, 400);
        double lx = w[0] - c[0], lz = w[1] - c[1];
        double u = lx * Math.cos(wind) + lz * Math.sin(wind);
        double v = -lx * Math.sin(wind) + lz * Math.cos(wind);
        // Stretched 160 blocks along the wind and 12 across it.
        double r = Fractal.ridged(s + 2 + 31L * cell.cx + cell.cz, u, v * (160 / 12.0), 160, 3);
        double threshold = 1 - (1 - THRESHOLD) * fade;
        if (r < threshold) return;
        int bottom = (int) Math.floor(200 + 40 * ValueNoise.mask(s + 3, x, z, 600));
        if (ground > bottom - 3) return;
        int top = r > threshold + 0.1 * fade ? bottom + 1 : bottom;
        out.add(new Layer(top, bottom));
    }

    /** Direction, in radians, the streaks of region cell (cx, cz) run in. */
    public static double wind(long seed, int cx, int cz) {
        return Hash.hash01(seed ^ SALT, cx, cz) * Math.PI * 2;
    }

    /** The region cell with the most weight at (x, z); across a border the streaks turn to the other wind. */
    private static RegionMap.CellWeight dominant(long seed, double x, double z) {
        RegionMap.CellWeight best = null;
        for (RegionMap.CellWeight cell : RegionMap.nearCells(seed, x, z)) {
            if (best == null || cell.weight > best.weight) best = cell;
        }
        return best;
    }
}
