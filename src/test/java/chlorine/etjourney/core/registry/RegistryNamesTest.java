package chlorine.etjourney.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistryNamesTest {

    @Test
    void textureIsPrefixedWithTheModid() {
        assertEquals("etjourney:sunken_sand", RegistryNames.texture("sunken_sand"));
    }

    @Test
    void unlocalizedNameIsPrefixedWithTheModid() {
        assertEquals("etjourney.sunken_sand", RegistryNames.unlocalized("sunken_sand"));
    }

    @Test
    void validNameIsReturnedUnchanged() {
        assertEquals("abyss_stone_2", RegistryNames.validate("abyss_stone_2"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "SunkenSand", "sunken sand", "etjourney:sand", "sand-block" })
    void invalidNamesAreRejected(String name) {
        assertThrows(IllegalArgumentException.class, () -> RegistryNames.validate(name));
    }

    @Test
    void textureRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> RegistryNames.texture("Bad"));
    }
}
