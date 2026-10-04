package chlorine.etjourney.world.end.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ModifierChainTest {

    private static final ChunkArea AREA = new ChunkArea(1L, 0, 0, Collections.emptyList());

    private static Modifier recorder(int order, List<String> log, String name) {
        return new Modifier() {

            @Override
            public int order() {
                return order;
            }

            @Override
            public void column(ChunkArea area, ColumnState state, double weight) {
                log.add(name + "@" + weight);
                state.top += 10 * weight;
            }
        };
    }

    @Test
    void appliesInDeclaredOrderWithWeightsAndSkipsZero() {
        List<String> log = new ArrayList<>();
        Modifier late = recorder(20, log, "late"), early = recorder(10, log, "early"), off = recorder(5, log, "off");
        ModifierChain chain = new ModifierChain(Arrays.asList(late, off, early));
        ColumnState state = new ColumnState(0, 0);
        chain.column(AREA, state, m -> m == off ? 0 : m == early ? 1 : 0.5);
        assertEquals(Arrays.asList("early@1.0", "late@0.5"), log);
        assertEquals(15, state.top, 1e-9);
    }

    @Test
    void interiorFollowsLand() {
        ColumnState state = new ColumnState(0, 0);
        state.land = 30;
        List<Double> seen = new ArrayList<>();
        Modifier probe = new Modifier() {

            @Override
            public int order() {
                return 0;
            }

            @Override
            public void column(ChunkArea area, ColumnState s, double weight) {
                seen.add(s.interior);
            }
        };
        new ModifierChain(Collections.singletonList(probe)).column(AREA, state, m -> 1);
        assertEquals(0.5, seen.get(0), 1e-9);
    }
}
