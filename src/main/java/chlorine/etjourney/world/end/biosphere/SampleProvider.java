package chlorine.etjourney.world.end.biosphere;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;

/** A hidden dimension that generates like the overworld, with its seed and world type, for biosphere cut-outs. */
public final class SampleProvider extends WorldProvider {

    @Override
    public IChunkProvider createChunkGenerator() {
        return SampleStructures.building(super::createChunkGenerator);
    }

    @Override
    public String getDimensionName() {
        return "ETJ Overworld Sample";
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }
}
