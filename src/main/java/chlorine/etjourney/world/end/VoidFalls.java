package chlorine.etjourney.world.end;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.world.end.feature.Falls;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Sets flowing water springs into the rim cliffs of VOID_FALLS land at populate time. The water is left to flow on
 * scheduled ticks, so it pours off the edge into the void.
 */
public final class VoidFalls implements IWorldGenerator {

    private static final int END = 1;
    private static final double MIN_WEIGHT = 0.5;

    public static void register() {
        GameRegistry.registerWorldGenerator(new VoidFalls(), 0);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        // The populate area is offset by 8 so springs and their neighbours stay in loaded chunks.
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        if (Math.hypot(ox + 8, oz + 8) < EndTerrain.TERRAIN_START + 64) return;
        Long seed = EndTerrain.seed();
        if (seed == null || EndTerrain.sampler(seed)
            .styleWeight("VOID_FALLS", ox + 8, oz + 8) < MIN_WEIGHT) return;
        Falls.Ground ground = (x, z) -> world.getHeightValue(x, z) - 1;
        for (int attempt = 0; attempt < Falls.ATTEMPTS; attempt++) {
            int[] s = Falls.spring(seed, ox, oz, attempt, ground);
            if (s != null) world.setBlock(s[0], s[1], s[2], Blocks.flowing_water, 0, 2);
        }
    }
}
