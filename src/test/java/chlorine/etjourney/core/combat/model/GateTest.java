package chlorine.etjourney.core.combat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GateTest {

    @Test
    void aFreshGateIsReady() {
        assertTrue(
            Gate.every(200)
                .ready(GateState.FRESH, 0));
    }

    @Test
    void oneChargeComesBackAfterTheRecharge() {
        Gate gate = Gate.every(200);
        GateState s = gate.fire(GateState.FRESH, 1000);
        assertFalse(gate.ready(s, 1000));
        assertFalse(gate.ready(s, 1199));
        assertTrue(gate.ready(s, 1200));
    }

    @Test
    void theCooldownHoldsBackSpareCharges() {
        Gate gate = new Gate(3, 20, 100);
        GateState s = gate.fire(GateState.FRESH, 0);
        assertFalse(gate.ready(s, 19));
        assertTrue(gate.ready(s, 20));
        s = gate.fire(s, 20);
        s = gate.fire(s, 40);
        assertEquals(0, gate.left(s, 40));
        assertFalse(gate.ready(s, 60));
    }

    @Test
    void chargesComeBackOneAtATimeFromTheFirstSpent() {
        Gate gate = new Gate(3, 0, 100);
        GateState s = gate.fire(GateState.FRESH, 0);
        s = gate.fire(s, 10);
        s = gate.fire(s, 20);
        assertEquals(0, gate.left(s, 99));
        assertEquals(1, gate.left(s, 100));
        assertEquals(2, gate.left(s, 200));
        assertEquals(3, gate.left(s, 300));
    }

    @Test
    void firingWhenNotReadyChangesNothing() {
        Gate gate = Gate.every(200);
        GateState s = gate.fire(GateState.FRESH, 0);
        GateState again = gate.fire(s, 50);
        assertEquals(s.spent, again.spent);
        assertEquals(s.lastFired, again.lastFired);
    }

    @Test
    void usesNeverComeBack() {
        Gate gate = Gate.uses(2);
        GateState s = gate.fire(GateState.FRESH, 0);
        assertFalse(gate.exhausted(s, 0));
        s = gate.fire(s, 1);
        assertTrue(gate.exhausted(s, 1_000_000));
        assertFalse(gate.ready(s, 1_000_000));
    }

    @Test
    void aClockThatWentBackForgets() {
        Gate gate = Gate.uses(1);
        GateState s = gate.fire(GateState.FRESH, 5000);
        assertTrue(gate.ready(s, 10));
    }
}
