package chlorine.etjourney.world.end.debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void completesTheTopWords() {
        assertEquals(Collections.singletonList("end"), complete("e"));
        assertEquals(Collections.singletonList("look"), complete("l"));
    }

    @Test
    void completesSubcommandsByPrefix() {
        assertEquals(Arrays.asList("style", "styles"), complete("end", "sty"));
        assertTrue(complete("end", "").contains("lake"));
        assertTrue(!complete("end", "").contains("look"), "look moved to /etj look");
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

    @Test
    void lookDirectionFollowsMinecraftAngles() {
        // Yaw 0 faces south (+Z), yaw 90 west (-X); pitch 90 looks straight down.
        assertArrayEquals(new double[] { 0, 0, 1 }, EtjArgs.lookDirection(0, 0), 1e-9);
        assertArrayEquals(new double[] { -1, 0, 0 }, EtjArgs.lookDirection(90, 0), 1e-9);
        assertArrayEquals(new double[] { 0, -1, 0 }, EtjArgs.lookDirection(0, 90), 1e-9);
    }

    @Test
    void lookGoesTheWholeRangeWhenNothingIsInTheWay() {
        double[] to = EtjArgs.lookTarget(new double[] { 0, 100, 0 }, new double[] { 0, 0, 1 }, 1024);
        assertArrayEquals(new double[] { 0, 100, 1024 }, to, 1e-9);
    }

    @Test
    void lookStaysBetweenY0AndY270() {
        assertEquals(270, EtjArgs.lookTarget(new double[] { 0, 100, 0 }, new double[] { 0, 1, 0 }, 1024)[1], 1e-9);
        assertEquals(0, EtjArgs.lookTarget(new double[] { 0, 100, 0 }, new double[] { 0, -1, 0 }, 1024)[1], 1e-9);
    }

    @Test
    void aBiosphereLineNamesTheBiomeItsIdAndClass() {
        assertEquals(
            "Biosphere: Crimson Forest (ID 176, DelirusCrux.Netherlicious.Biomes.CrimsonForest)",
            EtjArgs.biosphereLine("Crimson Forest", 176, "DelirusCrux.Netherlicious.Biomes.CrimsonForest"));
    }

    /** Vanilla ray tracing gives up after 200 block boundaries, so a long look is traced in short pieces. */
    @Test
    void aLongLookIsSplitIntoJoinedPiecesOfAtMostTheStep() {
        double[] eye = { 10, 70, -5 }, dir = { 0.6, -0.48, 0.64 };
        List<double[]> pieces = EtjArgs.lookSegments(eye, dir, 1000, 64);
        assertEquals(16, pieces.size());
        double[] from = eye;
        for (double[] p : pieces) {
            assertArrayEquals(from, Arrays.copyOfRange(p, 0, 3), 1e-9);
            double len = Math.sqrt(sq(p[3] - p[0]) + sq(p[4] - p[1]) + sq(p[5] - p[2]));
            assertTrue(len <= 64 + 1e-9, "piece of " + len);
            from = Arrays.copyOfRange(p, 3, 6);
        }
        assertArrayEquals(new double[] { 610, -410, 635 }, from, 1e-9);
    }

    private static double sq(double v) {
        return v * v;
    }
}
