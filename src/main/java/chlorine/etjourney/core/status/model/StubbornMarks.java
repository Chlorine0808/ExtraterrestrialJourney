package chlorine.etjourney.core.status.model;

import java.util.HashMap;
import java.util.Map;

/** World times at which each stubborn effect, by potion ID, stops being stubborn. */
public final class StubbornMarks {

    private StubbornMarks() {}

    /** Keeps the later end when the effect is already marked. */
    public static void mark(Map<Integer, Long> marks, int potion, long until) {
        marks.merge(potion, until, Math::max);
    }

    /** The marks still running at the given time. */
    public static Map<Integer, Long> live(Map<Integer, Long> marks, long now) {
        Map<Integer, Long> out = new HashMap<>();
        for (Map.Entry<Integer, Long> e : marks.entrySet()) if (e.getValue() > now) out.put(e.getKey(), e.getValue());
        return out;
    }
}
