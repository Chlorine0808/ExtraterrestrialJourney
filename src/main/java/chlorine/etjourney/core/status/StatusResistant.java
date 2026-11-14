package chlorine.etjourney.core.status;

import chlorine.etjourney.core.status.model.StatusResistance;

/** Implemented by a creature that shrugs off status effects. */
public interface StatusResistant {

    void addStatusResistance(StatusResistance.Builder out);
}
