package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.RangedWardAbility;

/** ranged_ward: a thrown-object hit is warded and half healed. */
final class RangedWardTrial extends WardTrial {

    RangedWardTrial() {
        super(RangedWardAbility.ID, Rank.EPIC);
    }

    @Override
    DamageSource warded(Ctx c) {
        return DamageSource.causeThrownDamage(c.player, c.player);
    }
}
