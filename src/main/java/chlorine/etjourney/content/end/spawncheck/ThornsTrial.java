package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.ThornsAbility;

/** thorns: a melee hit on the mob hurts the player who made it. */
final class ThornsTrial extends AbilityTrial {

    ThornsTrial() {
        super(ThornsAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(4, 0, ThornsAbility.ID);
            float before = c.player.getHealth();
            mob.attackEntityFrom(c.byPlayer(), 10);
            c.verdict(
                c.player.getHealth() < before,
                name + ": the attacker lost health (" + before + " -> " + c.player.getHealth() + ")");
            return true;
        }
        return false;
    }
}
