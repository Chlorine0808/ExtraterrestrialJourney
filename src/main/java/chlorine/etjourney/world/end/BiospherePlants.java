package chlorine.etjourney.world.end;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenFlowers;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.core.util.ShadowFields;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/** Grows the trees, flowers and grass of each biosphere's biome on its floor at populate time. */
public final class BiospherePlants implements IWorldGenerator {

    private static final int END = 1;
    /** Most tries of each kind per populate window, against biomes that decorate very densely. */
    private static final int MAX_TREES = 8, MAX_FLOWERS = 8, MAX_GRASS = 24;

    private static final Map<BiomeGenBase, int[]> COUNTS = new ConcurrentHashMap<>();

    public static void register() {
        GameRegistry.registerWorldGenerator(new BiospherePlants(), 0);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        // The populate window is offset by 8, as in the vanilla decorator.
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        if (Math.hypot(ox + 8, oz + 8) < EndTerrain.TERRAIN_START) return;
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        List<Biosphere> balls = Biosphere.KIND.near(
            seed,
            ox + 8,
            oz + 8,
            12,
            EndTerrain.sampler(seed)
                .structureProbe());
        if (balls.isEmpty()) return;
        ChunkPlan plan = EndTerrain.plan(generator, world, seed, chunkX, chunkZ);
        for (Biosphere b : balls) {
            if (!StructureModifiers.stands(plan, b, Biosphere.KIND)) continue;
            BiomeGenBase biome = BiomePalette.biome(b.pick);
            if (biome == null) continue;
            try {
                decorate(world, random, biome, b, ox, oz);
            } catch (RuntimeException e) {
                // A mod's generator may assume its own dimension; lose the plants, not the world.
                ModLog.LOG.warn("Biosphere plants of {} failed at {},{}", biome.biomeName, ox, oz, e);
            }
        }
    }

    private static void decorate(World world, Random random, BiomeGenBase biome, Biosphere b, int ox, int oz) {
        int[] counts = counts(biome);
        int trees = Math.min(MAX_TREES, counts[0]) + (random.nextInt(10) == 0 ? 1 : 0);
        for (int i = 0; i < trees; i++) {
            int x = ox + random.nextInt(16), z = oz + random.nextInt(16), y = b.plantY(x, z);
            if (y < 0) continue;
            WorldGenAbstractTree tree = biome.func_150567_a(random);
            tree.setScale(1, 1, 1);
            if (tree.generate(world, random, x, y, z)) tree.func_150524_b(world, random, x, y, z);
        }
        for (int i = 0; i < Math.min(MAX_FLOWERS, counts[1]); i++) {
            int x = ox + random.nextInt(16), z = oz + random.nextInt(16), y = b.plantY(x, z);
            if (y < 0) continue;
            String name = biome.func_150572_a(random, x, y, z);
            BlockFlower flower = BlockFlower.func_149857_e(name);
            if (flower.getMaterial() == Material.air) continue;
            WorldGenFlowers gen = new WorldGenFlowers(flower);
            gen.func_150550_a(flower, BlockFlower.func_149856_f(name));
            gen.generate(world, random, x, y, z);
        }
        for (int i = 0; i < Math.min(MAX_GRASS, counts[2]); i++) {
            int x = ox + random.nextInt(16), z = oz + random.nextInt(16), y = b.plantY(x, z);
            if (y < 0) continue;
            biome.getRandomWorldGenForGrass(random)
                .generate(world, random, x, y, z);
        }
    }

    /**
     * Trees, flowers and grass per chunk, as {trees, flowers, grass}. BoP hides the vanilla decorator behind one of
     * its own, and a biome it overrides keeps the counts in the vanilla biome it wraps, so the larger count wins.
     */
    private static int[] counts(BiomeGenBase biome) {
        return COUNTS.computeIfAbsent(biome, b -> {
            int[] out = new int[3];
            add(out, decorator(b));
            BiomeGenBase wrapped = ShadowFields.find(b, BiomeGenBase.class, BiomeGenBase.class);
            if (wrapped != null) add(out, decorator(wrapped));
            return out;
        });
    }

    private static void add(int[] counts, BiomeDecorator d) {
        if (d == null) return;
        counts[0] = Math.max(counts[0], d.treesPerChunk);
        counts[1] = Math.max(counts[1], d.flowersPerChunk);
        counts[2] = Math.max(counts[2], d.grassPerChunk);
    }

    private static BiomeDecorator decorator(BiomeGenBase biome) {
        BiomeDecorator hidden = ShadowFields.find(biome, BiomeGenBase.class, BiomeDecorator.class);
        return hidden != null ? hidden : biome.theBiomeDecorator;
    }
}
