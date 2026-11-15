package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.VeilAbility;

/** veil: a sword hit costs far more health than a fire hit of the same size. */
final class VeilTrial extends AbilityTrial {

    VeilTrial() {
        super(VeilAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(Rank.EPIC, 4, 0, VeilAbility.ID);
        float full = mob.getHealth();
        mob.attackEntityFrom(c.byPlayer(), 4);
        float lostToSword = full - mob.getHealth();
        mob.hurtResistantTime = 0;
        float before = mob.getHealth();
        mob.attackEntityFrom(ElementWardTrial.hit(c, Elements.FIRE), 4);
        float lostToFire = before - mob.getHealth();
        c.verdict(
            lostToSword > lostToFire * 2,
            name + ": lost " + lostToSword + " to the sword, " + lostToFire + " to fire");
        return true;
    }
}
