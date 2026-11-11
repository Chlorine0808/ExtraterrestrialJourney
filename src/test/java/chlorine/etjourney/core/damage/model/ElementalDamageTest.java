package chlorine.etjourney.core.damage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElementalDamageTest {

    private static final Element ICE = ElementRegistry.register("test_damage_ice");
    private static final Element VOID = ElementRegistry.registerAbsolute("test_damage_void");
    private static final Element WIND = ElementRegistry.register("test_damage_wind");

    @Test
    void addsUpTheSameElement() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.MAGIC)
            .add(ICE, 2)
            .add(ICE, 3)
            .build();
        assertEquals(
            5f,
            d.parts()
                .get(ICE),
            1e-6);
        assertEquals(5f, d.raw(), 1e-6);
        assertEquals(DamageKind.MAGIC, d.kind());
        assertFalse(d.isPiercing());
    }

    @Test
    void rejectsANegativeAmount() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ElementalDamage.builder(DamageKind.PHYSICAL)
                .add(ICE, -1));
    }

    @Test
    void piercesOnlyWhenEveryPartIsAbsolute() {
        assertTrue(
            ElementalDamage.builder(DamageKind.MAGIC)
                .add(VOID, 1)
                .build()
                .isPiercing());
        assertFalse(
            ElementalDamage.builder(DamageKind.MAGIC)
                .add(VOID, 1)
                .add(ICE, 1)
                .build()
                .isPiercing());
        assertFalse(
            ElementalDamage.builder(DamageKind.MAGIC)
                .build()
                .isPiercing());
    }

    @Test
    void splitsTheAbsolutePartsFromTheRest() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(VOID, 5)
            .add(ICE, 3)
            .build();
        assertEquals(
            5f,
            d.absolutePart()
                .raw(),
            1e-6);
        assertTrue(
            d.absolutePart()
                .isPiercing());
        assertEquals(
            3f,
            d.ordinaryPart()
                .raw(),
            1e-6);
        assertFalse(
            d.ordinaryPart()
                .isPiercing());
        assertEquals(
            DamageKind.PHYSICAL,
            d.absolutePart()
                .kind());
    }

    @Test
    void dominantIsTheLargestAndTheFirstOnATie() {
        ElementalDamage tie = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(WIND, 3)
            .add(ICE, 3)
            .build();
        assertSame(WIND, tie.dominant());
        ElementalDamage larger = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(WIND, 3)
            .add(ICE, 4)
            .build();
        assertSame(ICE, larger.dominant());
    }

    @Test
    void dominantOfAnEmptyDamageIsNeutral() {
        assertSame(
            ElementRegistry.NEUTRAL,
            ElementalDamage.builder(DamageKind.PHYSICAL)
                .build()
                .dominant());
    }

    @Test
    void partsCannotBeChanged() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(ICE, 1)
            .build();
        assertThrows(
            UnsupportedOperationException.class,
            () -> d.parts()
                .put(WIND, 1f));
    }
}
