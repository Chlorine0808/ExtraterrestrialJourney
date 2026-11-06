package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.List;

/** Whether a pile of single items fills a list of inputs exactly, each item serving one unit of one input. */
final class Pile {

    private Pile() {}

    static boolean fills(List<ItemInput> inputs, List<ItemKey> items) {
        List<ItemInput> units = new ArrayList<>();
        for (ItemInput input : inputs) for (int i = 0; i < input.amount; i++) units.add(input);
        if (units.size() != items.size()) return false;
        return assign(units, items, 0, new boolean[items.size()]);
    }

    // Backtracks, since an item an ore input accepts may be the only one another input can take; piles hold a few
    // items at most.
    private static boolean assign(List<ItemInput> units, List<ItemKey> items, int unit, boolean[] used) {
        if (unit == units.size()) return true;
        for (int i = 0; i < items.size(); i++) {
            if (used[i] || !units.get(unit)
                .accepts(items.get(i))) continue;
            used[i] = true;
            if (assign(units, items, unit + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }
}
