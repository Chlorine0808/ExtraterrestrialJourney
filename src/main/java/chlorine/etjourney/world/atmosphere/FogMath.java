package chlorine.etjourney.world.atmosphere;

/**
 * Fog distances for a density, as BCLib does: the end shrinks toward half the view distance, never past vanilla.
 */
public final class FogMath {

    private static final float VANILLA_START = 0.75f, DENSE_START = 0.25f, MIN_END = 0.5f;

    private FogMath() {}

    public static float end(float far, float density) {
        if (density <= 1) return far;
        return far * (MIN_END + (1 - MIN_END) / density);
    }

    /** The start factor eases from vanilla's 0.75 at density 1 to 0.25 at 2, so a blend crossing 1 does not jump. */
    public static float start(float far, float density) {
        float t = Math.max(0, Math.min(1, density - 1));
        return end(far, density) * (VANILLA_START + (DENSE_START - VANILLA_START) * t);
    }
}
