package chlorine.etjourney.world.atmosphere;

import chlorine.etjourney.world.end.feature.EndBiome;

/** The atmospheres by byte ID: 0 is the vanilla End air, then one per End biome. */
public final class Atmospheres {

    /** WorldProviderEnd's fog colour, 0xA080A0 scaled by 0.15. */
    public static final Atmosphere NONE = new Atmosphere(
        0xA0 / 255f * 0.15f,
        0x80 / 255f * 0.15f,
        0xA0 / 255f * 0.15f,
        1,
        0);

    private static final float SKY_MIX = 0.33f;
    private static final Atmosphere[] BY_BIOME = new Atmosphere[EndBiome.values().length];

    static {
        put(EndBiome.GROVE, 0x82C878, 1.10f);
        put(EndBiome.HIGHLAND, 0xB2D1F8, 1.00f);
        put(EndBiome.MYCELIA, 0xD7A5F0, 1.20f);
        put(EndBiome.AMBER, 0xFFB847, 2.00f);
        put(EndBiome.LUCENTWOODS, 0x2D161E, 1.00f);
    }

    private Atmospheres() {}

    private static void put(EndBiome biome, int rgb, float density) {
        BY_BIOME[biome.ordinal()] = Atmosphere.ofRgb(rgb, density, SKY_MIX);
    }

    public static byte idOf(EndBiome biome) {
        return (byte) (biome == null ? 0 : biome.ordinal() + 1);
    }

    public static Atmosphere byId(int id) {
        if (id < 1 || id > BY_BIOME.length) return NONE;
        Atmosphere a = BY_BIOME[id - 1];
        return a == null ? NONE : a;
    }
}
