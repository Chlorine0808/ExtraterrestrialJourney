package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.MagicWardAbility;

/** magic_ward: a magic hit is warded and half healed. */
final class MagicWardTrial extends WardTrial {

    MagicWardTrial() {
        super(MagicWardAbility.ID, Rank.EPIC);
    }

    @Override
    DamageSource warded(Ctx c) {
        return DamageSource.causeIndirectMagicDamage(c.player, c.player);
    }
}
