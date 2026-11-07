package chlorine.etjourney.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WindowTitleTest {

    private static final String BASE = "Minecraft 1.7.10";

    @Test
    void isTheBaseWithoutATagOrAShotList() {
        assertEquals(BASE, WindowTitle.compose(BASE, null, null));
        assertEquals(BASE, WindowTitle.compose(BASE, " ", ""));
    }

    @Test
    void putsTheCheckoutTagFirst() {
        assertEquals(
            "fix/building-sink @ ETJ-building-sink | Minecraft 1.7.10",
            WindowTitle.compose(BASE, " fix/building-sink @ ETJ-building-sink ", null));
    }

    @Test
    void namesOnlyTheShotListFile() {
        assertEquals(
            "master @ ExtraterrestrialJourney | shoot 2026-10-28-end.txt | Minecraft 1.7.10",
            WindowTitle.compose(BASE, "master @ ExtraterrestrialJourney", "../../shots\\lists/2026-10-28-end.txt"));
    }

    @Test
    void showsTheShotListEvenWithoutATag() {
        assertEquals("shoot a.txt | Minecraft 1.7.10", WindowTitle.compose(BASE, null, "a.txt"));
    }
}
