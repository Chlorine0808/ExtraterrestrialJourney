package chlorine.etjourney.content.end.spawncheck;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.BarrierAbility;

/** barrier: a hit of the set element does no harm, another element hurts. */
final class BarrierTrial extends ElementWardTrial {

    BarrierTrial() {
        super(BarrierAbility.ID, Rank.RARE);
    }
}
