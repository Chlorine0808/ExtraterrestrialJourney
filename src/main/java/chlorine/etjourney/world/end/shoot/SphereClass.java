package chlorine.etjourney.world.end.shoot;

import chlorine.etjourney.world.end.feature.BiosphereSource;

/** Screenshot category of a biosphere, derived from its resolved source. */
public enum SphereClass {

    SURFACE,
    WATER,
    CAVE,
    NETHER;

    /** {@code waterPoint} only matters for a surface site. */
    public static SphereClass of(BiosphereSource.Kind site, boolean waterPoint) {
        switch (site) {
            case CAVE:
                return CAVE;
            case NETHER:
                return NETHER;
            default:
                return waterPoint ? WATER : SURFACE;
        }
    }
}
