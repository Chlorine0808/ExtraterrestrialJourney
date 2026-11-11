package chlorine.etjourney.core.accessory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ModifierPlanTest {

    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    private static ModifierPlan.Entry entry(UUID id, double amount) {
        return new ModifierPlan.Entry(id, "generic.attackDamage", amount, 0);
    }

    @Test
    void addsWhatIsMissing() {
        ModifierPlan plan = ModifierPlan.between(Collections.emptyList(), Collections.singletonList(entry(A, 1)));
        assertTrue(plan.removals.isEmpty());
        assertEquals(Collections.singletonList(entry(A, 1)), plan.additions);
    }

    @Test
    void removesWhatIsNoLongerWorn() {
        ModifierPlan plan = ModifierPlan.between(Collections.singletonList(entry(A, 1)), Collections.emptyList());
        assertEquals(Collections.singletonList(entry(A, 1)), plan.removals);
        assertTrue(plan.additions.isEmpty());
    }

    @Test
    void replacesAModifierWhoseAmountChanged() {
        ModifierPlan plan = ModifierPlan
            .between(Collections.singletonList(entry(A, 1)), Collections.singletonList(entry(A, 2)));
        assertEquals(Collections.singletonList(entry(A, 1)), plan.removals);
        assertEquals(Collections.singletonList(entry(A, 2)), plan.additions);
    }

    @Test
    void leavesAnUnchangedModifierAlone() {
        ModifierPlan plan = ModifierPlan
            .between(Arrays.asList(entry(A, 1), entry(B, 3)), Arrays.asList(entry(A, 1), entry(B, 3)));
        assertTrue(plan.removals.isEmpty());
        assertTrue(plan.additions.isEmpty());
    }

    @Test
    void sameIdOnAnotherOperationIsAChange() {
        ModifierPlan.Entry multiply = new ModifierPlan.Entry(A, "generic.attackDamage", 1, 1);
        ModifierPlan plan = ModifierPlan
            .between(Collections.singletonList(entry(A, 1)), Collections.singletonList(multiply));
        assertEquals(Collections.singletonList(entry(A, 1)), plan.removals);
        assertEquals(Collections.singletonList(multiply), plan.additions);
    }
}
