package chlorine.etjourney.world.end.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KindTableTest {

    @Test
    void unboundKindHasNoEntry() {
        KindTable<String> table = new KindTable<>();
        assertNull(table.get(EndBlock.SOLAR_TOP));
    }

    @Test
    void bindOverridesOnlyThatKind() {
        KindTable<String> table = new KindTable<>();
        table.bind(EndBlock.SOLAR_TOP, "crust");
        assertEquals("crust", table.get(EndBlock.SOLAR_TOP));
        assertNull(table.get(EndBlock.SOLAR_FILL));
    }

    @Test
    void laterBindWins() {
        KindTable<String> table = new KindTable<>();
        table.bind(EndBlock.VORTEX_TOP, "a");
        table.bind(EndBlock.VORTEX_TOP, "b");
        assertEquals("b", table.get(EndBlock.VORTEX_TOP));
    }

    @Test
    void nullIsRejected() {
        KindTable<String> table = new KindTable<>();
        assertThrows(NullPointerException.class, () -> table.bind(EndBlock.NEBULA_TOP, null));
        assertThrows(NullPointerException.class, () -> table.bind(null, "x"));
    }
}
