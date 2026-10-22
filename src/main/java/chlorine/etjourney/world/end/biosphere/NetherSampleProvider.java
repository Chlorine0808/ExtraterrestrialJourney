package chlorine.etjourney.world.end.biosphere;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.DimensionManager;

/**
 * A hidden dimension that generates like the Nether, borrowing the biome map and generator of the Nether's own
 * provider; that provider sets its ID to -1, which would save this world over the real Nether.
 */
public final class NetherSampleProvider extends WorldProviderHell {

    private WorldProvider nether;

    @Override
    public void registerWorldChunkManager() {
        nether = DimensionManager.createProviderFor(-1);
        nether.registerWorld(worldObj);
        worldChunkMgr = nether.worldChunkMgr;
        isHellWorld = true;
        hasNoSky = true;
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        return SampleStructures.building(nether::createChunkGenerator);
    }

    @Override
    public String getDimensionName() {
        return "ETJ Nether Sample";
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }
}
