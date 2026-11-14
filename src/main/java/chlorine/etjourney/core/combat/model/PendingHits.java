package chlorine.etjourney.core.combat.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Hits waiting a tick to see how much health they really took. */
public final class PendingHits<T> {

    private static final class Entry<T> {

        final T hit;
        final float healthBefore;
        final float amount;
        final long tick;

        Entry(T hit, float healthBefore, float amount, long tick) {
            this.hit = hit;
            this.healthBefore = healthBefore;
            this.amount = amount;
            this.tick = tick;
        }
    }

    private final List<Entry<T>> entries = new ArrayList<>();

    public void add(T hit, float healthBefore, float amount, long tick) {
        entries.add(new Entry<>(hit, healthBefore, amount, tick));
    }

    /** Lands the hits made before now; a hit that took no health is dropped silently. */
    public void resolve(long now, Function<T, Float> healthNow, BiConsumer<T, Float> landed) {
        land(now, healthNow, landed);
    }

    /** Lands every hit at once, for in-game checks that look right after a hit. */
    public void flush(Function<T, Float> healthNow, BiConsumer<T, Float> landed) {
        land(Long.MAX_VALUE, healthNow, landed);
    }

    private void land(long now, Function<T, Float> healthNow, BiConsumer<T, Float> landed) {
        // Copied first: a landed hit may cause another hit, which must wait for the next round.
        List<Entry<T>> due = new ArrayList<>();
        for (Iterator<Entry<T>> it = entries.iterator(); it.hasNext();) {
            Entry<T> entry = it.next();
            if (entry.tick >= now) continue;
            due.add(entry);
            it.remove();
        }
        for (Entry<T> entry : due) {
            float lost = Math.min(entry.amount, entry.healthBefore - healthNow.apply(entry.hit));
            if (lost > 0) landed.accept(entry.hit, lost);
        }
    }
}
