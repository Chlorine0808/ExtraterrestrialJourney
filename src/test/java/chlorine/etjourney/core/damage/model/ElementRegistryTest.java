package chlorine.etjourney.core.damage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElementRegistryTest {

    @Test
    void startsWithNeutral() {
        assertSame(ElementRegistry.NEUTRAL, ElementRegistry.get("neutral"));
        assertEquals("neutral", ElementRegistry.NEUTRAL.name());
        assertTrue(
            ElementRegistry.all()
                .contains(ElementRegistry.NEUTRAL));
    }

    @Test
    void looksUpARegisteredElement() {
        Element e = ElementRegistry.register("test_registry_light");
        assertSame(e, ElementRegistry.get("test_registry_light"));
        assertNull(ElementRegistry.get("test_registry_missing"));
    }

    @Test
    void rejectsTheSameNameTwice() {
        ElementRegistry.register("test_registry_twice");
        assertThrows(IllegalArgumentException.class, () -> ElementRegistry.register("test_registry_twice"));
    }

    @Test
    void rejectsANameThatIsNotSnakeCase() {
        assertThrows(IllegalArgumentException.class, () -> ElementRegistry.register("Fire"));
    }
}
