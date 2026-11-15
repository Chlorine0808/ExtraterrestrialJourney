package chlorine.etjourney.content.end.spawncheck;

import chlorine.etjourney.content.rank.ability.GiantAbility;

/** giant: the hitbox is 1.5 times that of a plain mob. */
final class GiantTrial extends SizeTrial {

    GiantTrial() {
        super(GiantAbility.ID, 1.5f);
    }
}
