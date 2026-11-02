package chlorine.etjourney.content.entity;

import chlorine.etjourney.world.end.feature.EndBiome;

/** Where a natural spawn is being tried, as a spawn rule sees it. */
public final class SpawnSite {

    /** Whether the spot lies past the central island, where the ETJ terrain starts. */
    public final boolean outerEnd;
    /** The continent's biome, or null for bare land. */
    public final EndBiome biome;
    public final int y, light;
    /** Entities of the same class already within the rule's cap radius. */
    public final int nearby;

    public SpawnSite(boolean outerEnd, EndBiome biome, int y, int light, int nearby) {
        this.outerEnd = outerEnd;
        this.biome = biome;
        this.y = y;
        this.light = light;
        this.nearby = nearby;
    }
}
