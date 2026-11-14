package chlorine.etjourney.content.area;

import java.util.HashMap;
import java.util.Map;

/** When an area next works on each creature in it, keyed by entity ID. */
public final class PulseClock {

    private final int interval;
    private final Map<Integer, Long> next = new HashMap<>();

    public PulseClock(int interval) {
        this.interval = interval;
    }

    /** True, and starts the next interval, when the creature is due. */
    public boolean due(int id, long now) {
        Long at = next.get(id);
        if (at != null && now < at) return false;
        next.put(id, now + interval);
        return true;
    }
}
