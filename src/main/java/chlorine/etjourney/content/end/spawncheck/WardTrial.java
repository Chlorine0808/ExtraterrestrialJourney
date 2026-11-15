package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;

/** A ward that heals: the hit it names does no harm and heals, an ordinary hit still hurts. */
abstract class WardTrial extends AbilityTrial {

    private final Rank rank;

    WardTrial(String id, Rank rank) {
        super(id);
        this.rank = rank;
    }

    /** The hit the ability wards off. */
    abstract DamageSource warded(Ctx c);

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(rank, 4, 0, name);
        mob.setHealth(mob.getMaxHealth() / 2);
        float before = mob.getHealth();
        mob.attackEntityFrom(warded(c), 4);
        float healed = mob.getHealth();
        c.verdict(healed > before, name + ": the warded hit healed (" + before + " -> " + healed + ")");
        mob.attackEntityFrom(c.byPlayer(), 4);
        c.verdict(
            mob.getHealth() < healed,
            name + ": a sword hit still hurts (" + healed + " -> " + mob.getHealth() + ")");
        return true;
    }
}
