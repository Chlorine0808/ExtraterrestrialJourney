package chlorine.etjourney.content.end.spawncheck;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.AbsorbAbility;

/** absorb: a hit of the set element does no harm, another element hurts. */
final class AbsorbTrial extends ElementWardTrial {

    AbsorbTrial() {
        super(AbsorbAbility.ID, Rank.EPIC);
    }
}
