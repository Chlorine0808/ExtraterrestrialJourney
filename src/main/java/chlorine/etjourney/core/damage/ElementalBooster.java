package chlorine.etjourney.core.damage;

import chlorine.etjourney.core.damage.model.Boosts;

/** Implemented by a creature that raises its own outgoing damage, such as a ranked enemy. */
public interface ElementalBooster {

    void addBoosts(Boosts.Builder out);
}
