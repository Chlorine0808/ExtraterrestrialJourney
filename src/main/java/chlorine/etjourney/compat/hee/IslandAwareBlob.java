package chlorine.etjourney.compat.hee;

import java.util.Random;

import net.minecraft.world.World;

import chylex.hee.world.feature.WorldGenBlob;

/** HEE's floating sphere, skipped wherever it would touch our terrain. */
final class IslandAwareBlob extends WorldGenBlob {

    @Override
    public boolean generate(World world, Random rand, int x, int y, int z) {
        return !IslandClearance.blocked(world, x, y, z) && super.generate(world, rand, x, y, z);
    }
}
