package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.List;

/** Multiplies a ranked mob's drops. */
public final class RankLoot {

    private RankLoot() {}

    /** The stack sizes a drop of count becomes when multiplied by factor. */
    public static List<Integer> stacks(int count, int factor, int maxStack) {
        List<Integer> out = new ArrayList<>();
        int left = count * factor, max = Math.max(1, maxStack);
        while (left > 0) {
            int n = Math.min(left, max);
            out.add(n);
            left -= n;
        }
        return out;
    }
}
