package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Rank;

/** A size ability: the hitbox is that of a plain mob times the factor. */
abstract class SizeTrial extends AbilityTrial {

    private final float factor;

    SizeTrial(String id, float factor) {
        super(id);
        this.factor = factor;
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(Rank.RARE, 4, 0, name);
        EntityLiving plain = c.plain(-4, 0);
        c.verdict(
            Math.abs(mob.width - plain.width * factor) < 0.01 && Math.abs(mob.height - plain.height * factor) < 0.01,
            name + ": hitbox " + mob.width + "x" + mob.height + " against " + plain.width + "x" + plain.height);
        return true;
    }
}
