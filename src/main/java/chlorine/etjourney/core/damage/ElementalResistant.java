package chlorine.etjourney.core.damage;

import chlorine.etjourney.core.damage.model.Resistance;

/** Implemented by a creature that resists or is weak to elements. */
public interface ElementalResistant {

    void addResistance(Resistance.Builder out);
}
