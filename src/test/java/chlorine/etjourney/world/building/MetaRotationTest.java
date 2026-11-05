package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Metadata of facing blocks turned with the building, against the 1.7.10 data values. */
class MetaRotationTest {

    static Stream<Arguments> rotations() {
        return Stream.of(
            Arguments.of("STAIRS", MetaRotation.STAIRS),
            Arguments.of("LOG", MetaRotation.LOG),
            Arguments.of("FACING_2345", MetaRotation.FACING_2345),
            Arguments.of("DOOR", MetaRotation.DOOR),
            Arguments.of("TRAPDOOR", MetaRotation.TRAPDOOR),
            Arguments.of("TORCH", MetaRotation.TORCH),
            Arguments.of("LEVER", MetaRotation.LEVER),
            Arguments.of("BUTTON", MetaRotation.BUTTON),
            Arguments.of("DIRECTIONAL_0123", MetaRotation.DIRECTIONAL_0123),
            Arguments.of("SIGN_POST", MetaRotation.SIGN_POST),
            Arguments.of("PISTON_DISPENSER", MetaRotation.PISTON_DISPENSER));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rotations")
    void everyRotationReturnsAfterFourQuarterTurns(String name, MetaRotation rotation) {
        for (int meta = 0; meta < 16; meta++) {
            int m = meta;
            for (int i = 0; i < 4; i++) m = rotation.rotate(m, Rotation.R90);
            assertEquals(meta, m, name + " meta " + meta);
            assertEquals(
                rotation.rotate(rotation.rotate(meta, Rotation.R90), Rotation.R90),
                rotation.rotate(meta, Rotation.R180));
        }
    }

    @Test
    void stairsEastBecomesSouth() {
        assertEquals(2, MetaRotation.STAIRS.rotate(0, Rotation.R90));
        assertEquals(2 | 4, MetaRotation.STAIRS.rotate(0 | 4, Rotation.R90));
        assertEquals(1, MetaRotation.STAIRS.rotate(2, Rotation.R90));
        assertEquals(3, MetaRotation.STAIRS.rotate(1, Rotation.R90));
        assertEquals(0, MetaRotation.STAIRS.rotate(3, Rotation.R90));
    }

    @Test
    void logAxisSwapsOnQuarterTurnOnly() {
        assertEquals(8, MetaRotation.LOG.rotate(4, Rotation.R90));
        assertEquals(4, MetaRotation.LOG.rotate(4, Rotation.R180));
        assertEquals(1, MetaRotation.LOG.rotate(1, Rotation.R90));
        assertEquals(12 | 2, MetaRotation.LOG.rotate(12 | 2, Rotation.R90));
        assertEquals(4 | 3, MetaRotation.LOG.rotate(8 | 3, Rotation.R270));
    }

    @Test
    void facingNorthBecomesEast() {
        assertEquals(5, MetaRotation.FACING_2345.rotate(2, Rotation.R90));
        assertEquals(3, MetaRotation.FACING_2345.rotate(5, Rotation.R90));
        assertEquals(1, MetaRotation.PISTON_DISPENSER.rotate(1, Rotation.R90));
        assertEquals(5 | 8, MetaRotation.PISTON_DISPENSER.rotate(2 | 8, Rotation.R90));
    }

    @Test
    void torchesLeversAndButtonsPointClockwise() {
        // 1 east, 3 south, 2 west, 4 north.
        assertEquals(3, MetaRotation.TORCH.rotate(1, Rotation.R90));
        assertEquals(5, MetaRotation.TORCH.rotate(5, Rotation.R90));
        assertEquals(3 | 8, MetaRotation.LEVER.rotate(1 | 8, Rotation.R90));
        assertEquals(6, MetaRotation.LEVER.rotate(5, Rotation.R90));
        assertEquals(7, MetaRotation.LEVER.rotate(0, Rotation.R90));
        assertEquals(1, MetaRotation.BUTTON.rotate(4, Rotation.R90));
    }

    @Test
    void doorTurnsOnlyItsLowerHalf() {
        assertEquals(1 | 4, MetaRotation.DOOR.rotate(0 | 4, Rotation.R90));
        assertEquals(8 | 1, MetaRotation.DOOR.rotate(8 | 1, Rotation.R90));
    }

    @Test
    void bedsGatesAndPumpkinsStepByOne() {
        assertEquals(1 | 8, MetaRotation.DIRECTIONAL_0123.rotate(0 | 8, Rotation.R90));
        assertEquals(0, MetaRotation.DIRECTIONAL_0123.rotate(3, Rotation.R90));
    }

    @Test
    void signPostAddsFour() {
        assertEquals(4, MetaRotation.SIGN_POST.rotate(0, Rotation.R90));
        assertEquals(3, MetaRotation.SIGN_POST.rotate(15, Rotation.R90));
    }

    @Test
    void trapdoorTurnsItsHingeSide() {
        // 1 north, 2 east, 0 south, 3 west; open and top bits kept.
        assertEquals(2 | 4 | 8, MetaRotation.TRAPDOOR.rotate(1 | 4 | 8, Rotation.R90));
        assertEquals(3, MetaRotation.TRAPDOOR.rotate(0, Rotation.R90));
    }

    @Test
    void registeredNameWins() {
        MetaRotation r = (meta, rot) -> meta;
        assertNull(MetaRotations.forName("etjourney:test_unregistered"));
        MetaRotations.register("etjourney:test_registered", r);
        assertSame(r, MetaRotations.forName("etjourney:test_registered"));
    }
}
