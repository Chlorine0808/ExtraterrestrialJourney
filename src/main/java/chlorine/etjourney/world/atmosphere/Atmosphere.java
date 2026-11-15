package chlorine.etjourney.world.atmosphere;

/**
 * How the air looks in one place: fog colour (0..1), fog density (1 = vanilla) and how far the sky leans to the fog.
 */
public final class Atmosphere {

    public final float r, g, b, density, skyMix;

    public Atmosphere(float r, float g, float b, float density, float skyMix) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.density = density;
        this.skyMix = skyMix;
    }

    public static Atmosphere ofRgb(int rgb, float density, float skyMix) {
        return new Atmosphere((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, density, skyMix);
    }
}
