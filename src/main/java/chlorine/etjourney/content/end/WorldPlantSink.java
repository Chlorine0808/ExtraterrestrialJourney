package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.plant.PlantSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Writes a plant into a live world, only where the block is air or replaceable, with block updates (flag 3). */
final class WorldPlantSink implements PlantSink {

    private final World world;

    WorldPlantSink(World world) {
        this.world = world;
    }

    @Override
    public void put(int x, int y, int z, EndBlock kind) {
        if (y < 0 || y > 255) return;
        Block at = world.getBlock(x, y, z);
        if (!world.isAirBlock(x, y, z) && !at.isReplaceable(world, x, y, z)) return;
        world.setBlock(x, y, z, EndPalette.block(kind), EndPalette.meta(kind), 3);
    }
}
