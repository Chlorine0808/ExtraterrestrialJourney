package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityPig;

import chlorine.etjourney.content.rank.ability.FrenzyAbility;

/** frenzy: one hit on a pig beside the mob becomes two within a few ticks. */
final class FrenzyTrial extends AbilityTrial {

    private EntityLiving mob;
    private EntityPig pig;
    private float afterFirst;

    FrenzyTrial() {
        super(FrenzyAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, FrenzyAbility.ID);
            pig = new EntityPig(c.end);
            c.place(pig, 4, 1);
            c.end.spawnEntityInWorld(pig);
            c.made.add(pig);
            return false;
        }
        if (tick == 5) {
            pig.setHealth(pig.getMaxHealth());
            pig.setLocationAndAngles(mob.posX + 0.8, mob.posY, mob.posZ, 0, 0);
            mob.attackEntityAsMob(pig);
            afterFirst = pig.getHealth();
            return false;
        }
        if (tick < 10) return false;
        c.verdict(
            pig.getHealth() < afterFirst,
            name + ": the pig took a second hit (" + afterFirst + " -> " + pig.getHealth() + ")");
        return true;
    }
}
