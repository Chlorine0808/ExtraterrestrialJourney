package chlorine.etjourney.world.end.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.region.Styles;

/** Tab completion follows the /etj end grammar, and the style listing names every style with its kind. */
class EtjArgsTest {

    private static final List<String> STYLES = Arrays.asList("PLAINS", "SPIRES", "SLOT_CANYONS");
    private static final List<String> ZONES = Arrays.asList("SOLAR", "VORTEX");

    private static List<String> complete(String... args) {
        return EtjArgs.complete(args, STYLES, ZONES);
    }

    @Test
    void completesTheWordEnd() {
        assertEquals(Collections.singletonList("end"), complete("e"));
    }

    @Test
    void completesSubcommandsByPrefix() {
        assertEquals(Arrays.asList("style", "styles"), complete("end", "sty"));
        assertTrue(complete("end", "").contains("look"));
    }

    @Test
    void completesStyleNamesIgnoringCase() {
        assertEquals(Arrays.asList("SPIRES", "SLOT_CANYONS"), complete("end", "style", "s"));
    }

    @Test
    void offersPureAfterAStyle() {
        assertEquals(Collections.singletonList("pure"), complete("end", "style", "PLAINS", "p"));
    }

    @Test
    void mixTakesUpToThreeStyles() {
        assertEquals(STYLES, complete("end", "mix", "PLAINS", "SPIRES", ""));
        assertEquals(Collections.emptyList(), complete("end", "mix", "PLAINS", "SPIRES", "SLOT_CANYONS", ""));
    }

    @Test
    void completesZones() {
        assertEquals(Collections.singletonList("VORTEX"), complete("end", "zone", "v"));
    }

    @Test
    void nothingForOtherWords() {
        assertEquals(Collections.emptyList(), complete("end", "lake", ""));
        assertEquals(Collections.emptyList(), complete("nether", ""));
    }

    @Test
    void theListingNamesEveryStyleWithItsKind() {
        List<String> lines = EtjArgs.styleLines(Styles.all());
        assertEquals(
            Styles.all()
                .size(),
            lines.size());
        assertTrue(lines.contains("PLAINS: base with land"), lines.toString());
        assertTrue(lines.contains("SPIRES: overlay on land"), lines.toString());
        assertTrue(lines.contains("ISLETS: base without land"), lines.toString());
        assertTrue(lines.contains("LAYERED: base with land, or overlay"), lines.toString());
        assertTrue(lines.contains("ARCS: base without land, or overlay"), lines.toString());
        assertTrue(lines.contains("CUMULUS: overlay on anything"), lines.toString());
    }
}
