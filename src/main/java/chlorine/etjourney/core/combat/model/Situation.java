package chlorine.etjourney.core.combat.model;

import chlorine.etjourney.core.damage.model.AttackProfile;

/** How things stand for the one whose hook or boost is asked, read by conditions. */
public interface Situation {

    /** Health over maximum health, from 0 to 1. */
    float healthFraction();

    /** Night by the world's clock, only where the sky turns from day to night. */
    boolean isNight();

    /** The attack in question, or {@link AttackProfile#UNKNOWN} outside a hit. */
    AttackProfile attack();
}
