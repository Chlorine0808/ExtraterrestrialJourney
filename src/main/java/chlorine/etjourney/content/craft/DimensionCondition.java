package chlorine.etjourney.content.craft;

import java.util.HashSet;
import java.util.Set;

/** The device stands in one of the listed dimensions. */
public final class DimensionCondition implements ICondition {

    private final Set<Integer> dimensions = new HashSet<>();

    public DimensionCondition(int... dimensions) {
        for (int dimension : dimensions) this.dimensions.add(dimension);
    }

    @Override
    public boolean isMet(ConditionContext context) {
        return dimensions.contains(context.dimension);
    }
}
