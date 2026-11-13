package chlorine.etjourney.core.status;

import chlorine.etjourney.core.status.model.StatusBoosts;

/** Implemented by a creature whose status effects last longer or hit harder. */
public interface StatusBooster {

    void addStatusBoosts(StatusBoosts.Builder out);
}
