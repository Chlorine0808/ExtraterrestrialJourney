package chlorine.etjourney.core.combat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * A hit counts only for the health it really took, seen a tick later: target dummies take hits without losing
 * health, and some creatures heal right back, so the hurt event's amount alone over-rewards them.
 */
class PendingHitsTest {

    private final Map<String, Float> health = new HashMap<>();
    private final List<String> landed = new ArrayList<>();

    private void resolve(PendingHits<String> hits, long now) {
        hits.resolve(now, health::get, (hit, lost) -> landed.add(hit + "=" + lost));
    }

    @Test
    void aHitLandsWithTheHealthItTook() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("zombie", 20, 6, 100);
        health.put("zombie", 15f);
        resolve(hits, 101);
        assertEquals(Collections.singletonList("zombie=5.0"), landed);
    }

    @Test
    void aHitWaitsATickBeforeLanding() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("zombie", 20, 6, 100);
        health.put("zombie", 15f);
        resolve(hits, 100);
        assertTrue(landed.isEmpty());
        resolve(hits, 101);
        assertEquals(1, landed.size());
    }

    @Test
    void aTargetThatLosesNothingGivesNothing() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("dummy", 20, 6, 100);
        health.put("dummy", 20f);
        resolve(hits, 101);
        assertTrue(landed.isEmpty());
    }

    @Test
    void aTargetThatHealsBackWithinTheTickGivesNothing() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("regrower", 20, 6, 100);
        health.put("regrower", 21f);
        resolve(hits, 101);
        assertTrue(landed.isEmpty());
    }

    @Test
    void theLossNeverExceedsTheHitItself() {
        // A second hit in the same tick lowers health further; each hit is credited at most its own amount.
        PendingHits<String> hits = new PendingHits<>();
        hits.add("first", 20, 4, 100);
        health.put("first", 10f);
        resolve(hits, 101);
        assertEquals(Collections.singletonList("first=4.0"), landed);
    }

    @Test
    void aLandedHitIsGoneAfterwards() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("zombie", 20, 6, 100);
        health.put("zombie", 15f);
        resolve(hits, 101);
        resolve(hits, 102);
        assertEquals(1, landed.size());
    }

    @Test
    void flushingLandsEveryHitAtOnce() {
        PendingHits<String> hits = new PendingHits<>();
        hits.add("zombie", 20, 6, 100);
        health.put("zombie", 15f);
        hits.flush(health::get, (hit, lost) -> landed.add(hit));
        assertEquals(Collections.singletonList("zombie"), landed);
    }
}
