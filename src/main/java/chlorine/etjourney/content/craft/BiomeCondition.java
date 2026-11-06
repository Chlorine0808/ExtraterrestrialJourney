package chlorine.etjourney.content.craft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** The device stands in one of the named biomes. */
public final class BiomeCondition implements ICondition {

    private final Set<String> biomes;

    public BiomeCondition(String... biomes) {
        this.biomes = new HashSet<>(Arrays.asList(biomes));
    }

    @Override
    public boolean isMet(ConditionContext context) {
        return biomes.contains(context.biome);
    }
}
