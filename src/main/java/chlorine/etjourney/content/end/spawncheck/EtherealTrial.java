package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.EtherealAbility;

/** ethereal: a sword hit does nothing, a magic hit hurts. */
final class EtherealTrial extends AbilityTrial {

    EtherealTrial() {
        super(EtherealAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(Rank.LEGENDARY, 4, 0, EtherealAbility.ID);
        float before = mob.getHealth();
        mob.attackEntityFrom(c.byPlayer(), 4);
        c.verdict(
            mob.getHealth() == before,
            name + ": the sword hit did nothing (" + before + " -> " + mob.getHealth() + ")");
        mob.attackEntityFrom(DamageSource.causeIndirectMagicDamage(c.player, c.player), 4);
        c.verdict(mob.getHealth() < before, name + ": the magic hit hurt (" + before + " -> " + mob.getHealth() + ")");
        return true;
    }
}
