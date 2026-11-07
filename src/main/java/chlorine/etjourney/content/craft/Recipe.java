package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One recipe shared by every device. A device with a centre slot, such as the Attune altar, matches the centre
 * separately from the rest; the duration is hits for the Forge and ticks for the Attune altar.
 */
public final class Recipe {

    /** Null when the recipe takes no centre item. */
    final ItemInput center;
    final List<ItemInput> inputs;
    final List<ICondition> conditions;
    public final ItemOutput output;
    public final int duration;

    private Recipe(Builder b) {
        this.center = b.center;
        this.inputs = Collections.unmodifiableList(new ArrayList<>(b.inputs));
        this.conditions = Collections.unmodifiableList(new ArrayList<>(b.conditions));
        this.output = b.output;
        this.duration = b.duration;
    }

    public static Builder builder(ItemOutput output, int duration) {
        return new Builder(output, duration);
    }

    boolean matches(ItemKey centerItem, List<ItemKey> items, ConditionContext context) {
        if (center == null ? centerItem != null : centerItem == null || !center.accepts(centerItem)) return false;
        if (!Pile.fills(inputs, items)) return false;
        for (ICondition condition : conditions) if (!condition.isMet(context)) return false;
        return true;
    }

    public static final class Builder {

        private final ItemOutput output;
        private final int duration;
        private ItemInput center;
        private final List<ItemInput> inputs = new ArrayList<>();
        private final List<ICondition> conditions = new ArrayList<>();

        private Builder(ItemOutput output, int duration) {
            if (duration < 1) throw new IllegalArgumentException("duration must be positive: " + duration);
            this.output = output;
            this.duration = duration;
        }

        public Builder center(ItemInput input) {
            if (input.amount != 1) throw new IllegalArgumentException("the centre holds one item");
            this.center = input;
            return this;
        }

        public Builder input(ItemInput input) {
            inputs.add(input);
            return this;
        }

        public Builder condition(ICondition condition) {
            conditions.add(condition);
            return this;
        }

        public Recipe build() {
            return new Recipe(this);
        }
    }
}
