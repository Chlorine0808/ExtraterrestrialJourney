package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** A biome belongs to the mod that registered its ID, which for vanilla's IDs is vanilla even when overridden. */
class BiomeOwnerTest {

    private static final Set<Integer> VANILLA = new HashSet<>(Arrays.asList(0, 1, 8));
    private static final Map<String, String> PACKAGES = Collections
        .singletonMap("biomesoplenty.common.biome.overworld", "BiomesOPlenty");

    /** Biomes O' Plenty swaps vanilla's plains for a class of its own; excluding the mod must not take plains. */
    @Test
    void anOverriddenVanillaBiomeStaysVanilla() {
        assertEquals(
            "minecraft",
            BiomeOwner.of(1, "biomesoplenty.common.biome.overridden.BiomeGenBOPPlains", VANILLA, PACKAGES));
    }

    @Test
    void aMutatedVanillaBiomeIsVanilla() {
        assertEquals("minecraft", BiomeOwner.of(129, "net.minecraft.world.biome.BiomeGenMutated", VANILLA, PACKAGES));
        assertEquals("minecraft", BiomeOwner.of(129, "biomesoplenty.Whatever", VANILLA, PACKAGES));
    }

    @Test
    void aModBiomeBelongsToTheModHoldingItsPackage() {
        assertEquals(
            "BiomesOPlenty",
            BiomeOwner.of(40, "biomesoplenty.common.biome.overworld.BiomeGenAlps", VANILLA, PACKAGES));
    }

    @Test
    void aVanillaClassAtAModsIdIsVanilla() {
        assertEquals("minecraft", BiomeOwner.of(60, "net.minecraft.world.biome.BiomeGenPlains", VANILLA, PACKAGES));
    }

    @Test
    void aClassNoModHoldsHasNoOwner() {
        assertNull(BiomeOwner.of(61, "somewhere.Else", VANILLA, PACKAGES));
        assertNull(BiomeOwner.of(62, "NoPackage", VANILLA, PACKAGES));
    }
}
