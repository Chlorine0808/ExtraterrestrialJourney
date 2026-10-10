package chlorine.etjourney.core.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** A subclass may hide a base field behind one of its own; the most derived value of the type wins. */
class ShadowFieldsTest {

    static class Part {
    }

    static class SpecialPart extends Part {
    }

    static class Base {

        Part part = new Part();
    }

    static class Plain extends Base {
    }

    static class Hiding extends Base {

        static final Part SHARED = new Part();
        private final SpecialPart part = new SpecialPart();
    }

    static class HidingNull extends Base {

        SpecialPart part;
    }

    @Test
    void aHidingFieldInASubclassWins() {
        Hiding h = new Hiding();
        assertSame(h.part, ShadowFields.find(h, Base.class, Part.class));
    }

    @Test
    void withoutOneThereIsNothingBelowTheBase() {
        assertNull(ShadowFields.find(new Plain(), Base.class, Part.class));
    }

    @Test
    void anEmptyHidingFieldCountsAsNone() {
        assertNull(ShadowFields.find(new HidingNull(), Base.class, Part.class));
    }
}
