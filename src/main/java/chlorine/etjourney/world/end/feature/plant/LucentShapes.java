package chlorine.etjourney.world.end.feature.plant;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** Surface displacements the Lucentwoods plants share, fixed by a seed so every chunk sees the same shape. */
final class LucentShapes {

    private LucentShapes() {}

    /** Smooth 3D noise in [-amplitude, amplitude] sampled at coordinates times 0.2, as upstream does. */
    static Sdf.Displacement noise(long seed, double amplitude) {
        return (x, y, z) -> (ValueNoise.noise3(seed, x, y, z, 5) * 2 - 1) * amplitude;
    }

    /**
     * Per-block roughness in [-amplitude, amplitude]. Upstream draws a fresh random number at every evaluation; a hash
     * of the block keeps the shape the same however often it is measured.
     */
    static Sdf.Displacement jitter(long seed, double amplitude) {
        return (x, y, z) -> (Hash.hash01(seed, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)) * 2 - 1)
            * amplitude;
    }
}
