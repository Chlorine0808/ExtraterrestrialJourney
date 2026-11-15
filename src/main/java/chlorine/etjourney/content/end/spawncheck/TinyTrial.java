package chlorine.etjourney.content.end.spawncheck;

import chlorine.etjourney.content.rank.ability.TinyAbility;

/** tiny: the hitbox is 0.6 times that of a plain mob. */
final class TinyTrial extends SizeTrial {

    TinyTrial() {
        super(TinyAbility.ID, 0.6f);
    }
}
