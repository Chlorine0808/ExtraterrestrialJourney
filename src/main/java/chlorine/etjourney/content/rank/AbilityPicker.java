package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Draws distinct abilities, all equally likely, from those that fit the mob. */
public final class AbilityPicker {

    private AbilityPicker() {}

    public static <T> List<T> pick(List<T> all, Predicate<T> fits, int count, Random random) {
        return pick(all, fits, (a, b) -> false, count, random);
    }

    /** As above, never keeping two abilities that clash, whichever way round the clash is declared. */
    public static <T> List<T> pick(List<T> all, Predicate<T> fits, BiPredicate<T, T> clash, int count, Random random) {
        List<T> pool = new ArrayList<>();
        for (T a : all) if (fits.test(a)) pool.add(a);
        Collections.shuffle(pool, random);
        List<T> out = new ArrayList<>();
        for (T a : pool) {
            if (out.size() >= count) break;
            boolean ok = true;
            for (T b : out) if (clash.test(a, b) || clash.test(b, a)) ok = false;
            if (ok) out.add(a);
        }
        return out;
    }
}
