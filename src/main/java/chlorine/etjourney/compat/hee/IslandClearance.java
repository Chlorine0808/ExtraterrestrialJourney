package chlorine.etjourney.compat.hee;

import net.minecraft.world.World;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.ZoneIslands;

/** Decides whether an HEE sphere or meteoroid at (x, y, z) would touch our End terrain. */
final class IslandClearance {

    /** Space kept clear around an island's footprint and above its top. */
    static final double MARGIN = 24;
    /** Spheres stay off continents at any height while land within MARGIN is above this. */
    private static final double LAND = -40;

    private IslandClearance() {}

    static boolean blocked(World world, int x, int y, int z) {
        if (world.provider.dimensionId != 1) return false;
        Long seed = EndTerrain.seed();
        return seed != null && blocked(EndTerrain.sampler(seed), x, y, z);
    }

    static boolean blocked(TerrainSampler sampler, double x, double y, double z) {
        long seed = sampler.seed();
        for (ZoneIslands.Island island : ZoneIslands.near(seed, x, z, ZoneIslands.CELL, sampler.landProbe())) {
            if (Math.hypot(x - island.x, z - island.z) > island.radius * 1.2 + MARGIN) continue;
            if (y >= island.y - island.down - MARGIN && y <= island.y + island.up + MARGIN) return true;
        }
        for (Islets.Islet islet : Islets.near(seed, x, z, MARGIN, sampler.isletProbe())) {
            if (Math.hypot(x - islet.x, z - islet.z) > islet.radius + MARGIN) continue;
            if (y >= islet.y - islet.flat - islet.depth - MARGIN && y <= islet.y + MARGIN) return true;
        }
        // Keep spheres off continents at any height, so nothing hangs above them.
        double land = -100;
        for (int a = 0; a < 4 && land <= LAND; a++) {
            double angle = a * Math.PI / 2;
            land = Math.max(land, sampler.land(x + Math.cos(angle) * MARGIN, z + Math.sin(angle) * MARGIN));
        }
        return Math.max(land, sampler.land(x, z)) > LAND;
    }
}
