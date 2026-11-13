package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

/** An area's row in the table refuses values that would make it misbehave, and fills in shape-wise defaults. */
class AreaSpecTest {

    private static void rejects(UnaryOperator<AreaSpec.Builder> change) {
        assertThrows(
            IllegalArgumentException.class,
            () -> change.apply(AreaSpec.builder("area"))
                .build());
    }

    @Test
    void theDefaultsMakeAnArea() {
        assertDoesNotThrow(
            () -> AreaSpec.builder("area")
                .build());
    }

    @Test
    void aFloorIsThinAndACloudTallUnlessTold() {
        assertEquals(
            0.5,
            AreaSpec.builder("a")
                .build().height);
        assertEquals(
            2,
            AreaSpec.builder("a")
                .shape(AreaShape.CLOUD)
                .build().height);
        assertEquals(
            3,
            AreaSpec.builder("a")
                .shape(AreaShape.CLOUD)
                .height(3)
                .build().height);
    }

    @Test
    void itStartsAtItsFullRadiusUnlessTold() {
        assertEquals(
            3,
            AreaSpec.builder("a")
                .radius(3)
                .build().size.startRadius);
    }

    @Test
    void rejectsBadSizes() {
        rejects(b -> b.radius(0));
        rejects(b -> b.height(0));
        rejects(b -> b.duration(0));
        rejects(b -> b.growTicks(-1));
        rejects(b -> b.shrinkTicks(-1));
        rejects(b -> b.radiusPerUse(-0.1));
        rejects(b -> b.minRadius(-0.1));
        // Born smaller than it may be, it would vanish on its first tick.
        rejects(
            b -> b.radius(2)
                .startRadius(0.2)
                .minRadius(0.5));
    }

    @Test
    void rejectsBadEffects() {
        rejects(b -> b.interval(0));
        rejects(b -> b.slow(-0.1));
        rejects(b -> b.slow(1.1));
    }

    @Test
    void rejectsABadName() {
        assertThrows(IllegalArgumentException.class, () -> AreaSpec.builder("Bad Name"));
    }
}
