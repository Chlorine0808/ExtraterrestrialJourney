package chlorine.etjourney.world.end.modifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Modifiers sorted by order (stable for equal orders), applied with a weight each. */
public final class ModifierChain {

    private final List<Modifier> modifiers;

    public ModifierChain(List<Modifier> modifiers) {
        List<Modifier> sorted = new ArrayList<>(modifiers);
        sorted.sort(Comparator.comparingInt(Modifier::order));
        this.modifiers = sorted;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    public void column(ChunkArea area, ColumnState state, ToDoubleFunction<Modifier> weightAt) {
        for (Modifier modifier : modifiers) {
            double weight = weightAt.applyAsDouble(modifier);
            if (weight <= 0) continue;
            state.refreshInterior();
            modifier.column(area, state, weight);
        }
        state.refreshInterior();
    }

    public List<Shape> shapes(ChunkArea area, ToDoubleFunction<Modifier> weightOf) {
        List<Shape> out = new ArrayList<>();
        for (Modifier modifier : modifiers) {
            double weight = weightOf.applyAsDouble(modifier);
            if (weight > 0) modifier.shapes(area, out, weight);
        }
        return out;
    }

    public void blocks(ChunkArea area, BlockSink sink, ToDoubleFunction<Modifier> weightOf) {
        for (Modifier modifier : modifiers) {
            double weight = weightOf.applyAsDouble(modifier);
            if (weight > 0) modifier.blocks(area, sink, weight);
        }
    }
}
