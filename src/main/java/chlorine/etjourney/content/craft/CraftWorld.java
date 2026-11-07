package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.noise.Distance;

/** Turns stacks and places into what the recipe core reads, and recipe outputs back into stacks. */
final class CraftWorld {

    private static final int END = 1;

    private CraftWorld() {}

    static ItemKey key(ItemStack stack) {
        List<String> oreNames = new ArrayList<>();
        for (int id : OreDictionary.getOreIDs(stack)) oreNames.add(OreDictionary.getOreName(id));
        return new ItemKey(Item.itemRegistry.getNameForObject(stack.getItem()), stack.getItemDamage(), oreNames);
    }

    /** Null when the output names an item that is not registered. */
    static ItemStack stack(ItemOutput output) {
        Item item = (Item) Item.itemRegistry.getObject(output.id);
        return item == null ? null : new ItemStack(item, output.amount, output.meta);
    }

    static ConditionContext context(World world, int x, int y, int z) {
        return new ConditionContext(
            biome(world, x, z),
            world.provider.dimensionId,
            world.canBlockSeeTheSky(x, y + 1, z),
            !world.provider.hasNoSky,
            world.getWorldTime());
    }

    /**
     * The End terrain's biomes are drawn by the terrain engine rather than stored, so they are read from it and named
     * etjourney:grove and so on; everywhere else the vanilla biome name is used. Bare End ground reads as
     * etjourney:bare.
     */
    static String biome(World world, int x, int z) {
        Long seed = EndTerrain.seed();
        if (world.provider.dimensionId == END && seed != null
            && Distance.planar(x + 0.5, z + 0.5) >= EndTerrain.TERRAIN_START) {
            EndBiome biome = EndBiome.at(seed, x + 0.5, z + 0.5);
            return endBiome(biome);
        }
        return world.getBiomeGenForCoords(x, z).biomeName;
    }

    static String endBiome(EndBiome biome) {
        return endBiomeName(
            biome == null ? "bare"
                : biome.name()
                    .toLowerCase(Locale.ROOT));
    }

    static String endBiomeName(String name) {
        return "etjourney:" + name;
    }
}
