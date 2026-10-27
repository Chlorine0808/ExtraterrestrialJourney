package chlorine.etjourney.world.end.feature;

import java.util.Map;
import java.util.Set;

/** The mod a biome belongs to, for the biosphere blacklist. */
public final class BiomeOwner {

    /** Mutated biomes sit this far above the biome they vary. */
    private static final int MUTATION = 128;

    private BiomeOwner() {}

    /**
     * "minecraft" for vanilla's own IDs and their mutations, whatever class now fills them (Biomes O' Plenty swaps in
     * its own), and for vanilla classes; otherwise the mod whose files hold the class's package, or null.
     */
    public static String of(int id, String className, Set<Integer> vanillaIds, Map<String, String> packageOwners) {
        if (vanillaIds.contains(id) || id >= MUTATION && vanillaIds.contains(id - MUTATION)) return "minecraft";
        if (className.startsWith("net.minecraft.")) return "minecraft";
        int dot = className.lastIndexOf('.');
        return dot < 0 ? null : packageOwners.get(className.substring(0, dot));
    }
}
