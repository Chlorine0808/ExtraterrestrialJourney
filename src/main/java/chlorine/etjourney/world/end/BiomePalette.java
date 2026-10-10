package chlorine.etjourney.world.end;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenEnd;
import net.minecraft.world.biome.BiomeGenHell;
import net.minecraftforge.common.BiomeDictionary;

import chlorine.etjourney.world.end.modifier.BiomePart;

/** Every registered biome, mods' included, with the ground blocks a biosphere of it is built from. */
public final class BiomePalette {

    private static volatile List<Ground> grounds;

    private BiomePalette() {}

    /** The biome a pick selects, or null when none is registered. */
    public static BiomeGenBase biome(double pick) {
        Ground g = ground(pick);
        return g == null ? null : g.biome;
    }

    static Block block(BiomePart part, double pick) {
        Ground g = ground(pick);
        if (g == null) return Blocks.end_stone;
        return part == BiomePart.TOP ? g.top : part == BiomePart.FILLER ? g.filler : g.deep;
    }

    static int meta(BiomePart part, double pick) {
        Ground g = ground(pick);
        if (g == null) return 0;
        return part == BiomePart.TOP ? g.topMeta : part == BiomePart.FILLER ? g.fillerMeta : g.deepMeta;
    }

    private static Ground ground(double pick) {
        List<Ground> all = grounds;
        if (all == null) grounds = all = collect();
        int i = BiomeChoice.index(all.size(), pick);
        return i < 0 ? null : all.get(i);
    }

    /** Read on first use, once every mod has registered its biomes; ordered by biome ID. */
    private static List<Ground> collect() {
        List<Ground> out = new ArrayList<>();
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome != null) out.add(new Ground(biome));
        }
        return Collections.unmodifiableList(out);
    }

    /** Rock under the filler: netherrack in the Nether, end stone in the End, stone anywhere else. */
    private static Block deep(BiomeGenBase biome, Block top, Block filler) {
        // Nether biomes of other mods do not extend BiomeGenHell; they register the type or keep netherrack.
        if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.NETHER) || top == Blocks.netherrack
            || filler == Blocks.netherrack) return Blocks.netherrack;
        return BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.END) ? Blocks.end_stone : Blocks.stone;
    }

    private static final class Ground {

        final BiomeGenBase biome;
        final Block top, filler, deep;
        final int topMeta, fillerMeta, deepMeta;

        Ground(BiomeGenBase biome) {
            this.biome = biome;
            if (biome instanceof BiomeGenHell) {
                // The Nether's ground comes from its chunk provider; the biome itself keeps grass.
                top = filler = deep = Blocks.netherrack;
                topMeta = fillerMeta = deepMeta = 0;
            } else if (biome instanceof BiomeGenEnd) {
                top = filler = deep = Blocks.end_stone;
                topMeta = fillerMeta = deepMeta = 0;
            } else {
                top = biome.topBlock != null ? biome.topBlock : Blocks.grass;
                topMeta = biome.topBlock != null ? biome.field_150604_aj & 15 : 0;
                filler = biome.fillerBlock != null ? biome.fillerBlock : Blocks.dirt;
                // 1.7.10 keeps no filler metadata; field_76754_C next to fillerBlock is a colour.
                fillerMeta = 0;
                deep = deep(biome, top, filler);
                deepMeta = 0;
            }
        }
    }
}
