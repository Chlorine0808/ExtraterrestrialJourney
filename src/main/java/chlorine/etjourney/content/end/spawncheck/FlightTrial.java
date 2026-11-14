package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.FlightAbility;

/** flight: a mob whose target is above it rises. */
final class FlightTrial extends AbilityTrial {

    private EntityLiving mob;
    private double start;

    FlightTrial() {
        super(FlightAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, FlightAbility.ID);
            mob.setAttackTarget(c.player);
            start = mob.posY;
            return false;
        }
        if (tick < 60) return false;
        c.verdict(mob.posY > start + 0.5, name + ": the mob rose from " + start + " to " + mob.posY);
        return true;
    }
}
