package chlorine.etjourney.content.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

/** Entities are numbered from 0 in registration order, once per name and class. */
class EntityTableTest {

    @Test
    void idsCountUpFromZero() {
        EntityTable table = new EntityTable();
        assertEquals(0, table.add("a", String.class, null).id);
        assertEquals(1, table.add("b", Integer.class, null).id);
        assertEquals(2, table.add("c", Long.class, null).id);
    }

    @Test
    void duplicateNameIsRejected() {
        EntityTable table = new EntityTable();
        table.add("a", String.class, null);
        assertThrows(IllegalArgumentException.class, () -> table.add("a", Integer.class, null));
    }

    @Test
    void duplicateTypeIsRejected() {
        EntityTable table = new EntityTable();
        table.add("a", String.class, null);
        assertThrows(IllegalArgumentException.class, () -> table.add("b", String.class, null));
    }

    @Test
    void nonSnakeCaseNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EntityTable().add("Bad", String.class, null));
    }

    @Test
    void byTypeFindsTheEntry() {
        EntityTable table = new EntityTable();
        EntityTable.Entry a = table.add("a", String.class, null);
        assertSame(a, table.byType(String.class));
        assertNull(table.byType(Integer.class));
    }

    @Test
    void entriesWithoutRendererAreListedInOrder() {
        EntityTable table = new EntityTable();
        table.add("a", String.class, null);
        table.add("b", Integer.class, null);
        table.add("c", Long.class, null);
        assertEquals(
            Arrays.asList("a", "c"),
            table.withoutRenderer(new HashSet<Class<?>>(Collections.singleton(Integer.class))));
    }
}
