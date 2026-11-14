package chlorine.etjourney.content.rank.ability;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.content.rank.RankTasks;

/** A warned strike: particles at a spot for a while, then the strike. */
final class Warning {

    private static final int PUFF_EVERY = 4;

    private Warning() {}

    /** Puffs at the spot every few ticks, then runs the strike once the ticks are up. */
    static void at(World world, String particle, double x, double y, double z, int ticks, Runnable strike) {
        puff(world, particle, x, y, z);
        for (int t = PUFF_EVERY; t < ticks; t += PUFF_EVERY) RankTasks.later(t, () -> puff(world, particle, x, y, z));
        RankTasks.later(ticks, strike);
    }

    static void puff(World world, String particle, double x, double y, double z) {
        if (world instanceof WorldServer)
            ((WorldServer) world).func_147487_a(particle, x, y, z, 8, 0.5, 0.2, 0.5, 0.02);
    }
}
