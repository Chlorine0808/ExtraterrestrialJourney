package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Draws distinct abilities, all equally likely, from those that fit the mob. */
public final class AbilityPicker {

    private AbilityPicker() {}

    public static <T> List<T> pick(List<T> all, Predicate<T> fits, int count, Random random) {
        List<T> pool = new ArrayList<>();
        for (T a : all) if (fits.test(a)) pool.add(a);
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }
}
