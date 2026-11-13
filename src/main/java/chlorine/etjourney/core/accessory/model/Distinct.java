package chlorine.etjourney.core.accessory.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Which worn accessories count: copies of one accessory count once unless it declares that it stacks. */
public final class Distinct {

    private Distinct() {}

    public static <T> List<T> of(List<T> worn, Function<T, Object> identity, Predicate<T> stacks) {
        List<T> counted = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (T item : worn) {
            if (stacks.test(item) || seen.add(identity.apply(item))) counted.add(item);
        }
        return counted;
    }
}
