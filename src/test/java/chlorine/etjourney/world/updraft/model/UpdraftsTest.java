package chlorine.etjourney.world.updraft.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Sources register and remove updrafts per dimension, and each change is reported for that dimension. */
class UpdraftsTest {

    private static final Updraft A = new Updraft(0, 0, 0, 1, 1, 1, 0.1, 0.5);
    private static final Updraft B = new Updraft(5, 0, 5, 6, 1, 6, 0.1, 0.5);

    @Test
    void keepsEachDimensionApart() {
        Updrafts all = new Updrafts();
        int a = all.add(1, A);
        all.add(0, B);
        assertEquals(Collections.singletonMap(a, A), all.in(1));
        assertEquals(Collections.singletonList(B), new ArrayList<>(all.in(0).values()));
        assertTrue(all.in(-1).isEmpty());
    }

    @Test
    void removesOnlyTheNamedOneInItsDimension() {
        Updrafts all = new Updrafts();
        int a = all.add(1, A);
        int b = all.add(1, B);
        assertNotEquals(a, b);
        assertFalse(all.remove(0, a));
        assertTrue(all.remove(1, a));
        assertFalse(all.remove(1, a));
        assertEquals(Collections.singletonMap(b, B), all.in(1));
    }

    @Test
    void reportsTheDimensionOfEachChange() {
        List<Integer> changed = new ArrayList<>();
        Updrafts all = new Updrafts();
        all.onChange(changed::add);
        int a = all.add(1, A);
        all.add(-1, B);
        all.remove(1, a);
        all.remove(1, a);
        all.clear(-1);
        all.clear(-1);
        assertEquals(Arrays.asList(1, -1, 1, -1), changed);
    }
}
