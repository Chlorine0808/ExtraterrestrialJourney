package chlorine.etjourney.content.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import chlorine.etjourney.world.end.feature.EndBiome;

/** Where and how often one entity spawns naturally in the outer End; ranges include both ends. */
public final class SpawnRule {

    public final Set<EndBiome> biomes;
    /** Whether bare land, with no continent biome, counts as a listed biome. */
    public final boolean bare;
    public final int minY, maxY, maxLight;
    /** A spawn passes only while fewer than cap of the same class stand within capRadius blocks. */
    public final int cap, capRadius;
    public final int weight, groupMin, groupMax;

    private SpawnRule(Builder b) {
        biomes = Collections.unmodifiableSet(b.biomes);
        bare = b.bare;
        minY = b.minY;
        maxY = b.maxY;
        maxLight = b.maxLight;
        cap = b.cap;
        capRadius = b.capRadius;
        weight = b.weight;
        groupMin = b.groupMin;
        groupMax = b.groupMax;
    }

    public static Builder builder(Set<EndBiome> biomes, boolean bare) {
        return new Builder(biomes, bare);
    }

    public boolean allows(SpawnSite site) {
        if (!site.outerEnd) return false;
        if (site.biome == null ? !bare : !biomes.contains(site.biome)) return false;
        if (site.y < minY || site.y > maxY) return false;
        if (site.light > maxLight) return false;
        return site.nearby < cap;
    }

    public static final class Builder {

        private final EnumSet<EndBiome> biomes;
        private final boolean bare;
        private int minY = 0, maxY = 255, maxLight = 7;
        private int cap = 4, capRadius = 32;
        private int weight = 10, groupMin = 1, groupMax = 2;

        private Builder(Set<EndBiome> biomes, boolean bare) {
            this.biomes = biomes.isEmpty() ? EnumSet.noneOf(EndBiome.class) : EnumSet.copyOf(biomes);
            this.bare = bare;
        }

        public Builder height(int min, int max) {
            minY = min;
            maxY = max;
            return this;
        }

        public Builder maxLight(int light) {
            maxLight = light;
            return this;
        }

        public Builder cap(int count, int radius) {
            cap = count;
            capRadius = radius;
            return this;
        }

        public Builder weight(int weight, int groupMin, int groupMax) {
            this.weight = weight;
            this.groupMin = groupMin;
            this.groupMax = groupMax;
            return this;
        }

        public SpawnRule build() {
            if (minY > maxY) throw new IllegalArgumentException("height " + minY + ".." + maxY);
            if (weight <= 0 || groupMin < 1 || groupMin > groupMax)
                throw new IllegalArgumentException("weight " + weight + ", group " + groupMin + ".." + groupMax);
            if (cap <= 0 || capRadius <= 0) throw new IllegalArgumentException("cap " + cap + " in " + capRadius);
            return new SpawnRule(this);
        }
    }
}
