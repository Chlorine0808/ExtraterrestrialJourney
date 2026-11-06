package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reading OKModular wand scan JSON into building templates. */
class TemplateParserTest {

    private static Template parse(String layers, String mappings, boolean blankIsAir) throws TemplateException {
        String json = "[{\"name\":\"t\",\"layers\":[" + layers + "],\"mappings\":{" + mappings + "}}]";
        return TemplateParser.parse("t", new StringReader(json), blankIsAir);
    }

    private static Template parse(String layers, String mappings) throws TemplateException {
        return parse(layers, mappings, true);
    }

    private static String only(Cell cell) {
        BlockRef ref = cell.pick(0);
        return ref.name + ":" + ref.meta;
    }

    @Test
    void layersRunTopToBottom() throws Exception {
        Template t = parse("{\"rows\":[\"A\"]},{\"rows\":[\"B\"]}", "\"A\":\"m:a\",\"B\":\"m:b\"");
        assertEquals(2, t.sizeY);
        assertEquals("m:b:0", only(t.get(0, 0, 0)));
        assertEquals("m:a:0", only(t.get(0, 1, 0)));
    }

    @Test
    void rowsAreZCharsAreX() throws Exception {
        Template t = parse("{\"rows\":[\"AB\",\"CD\"]}", "\"A\":\"m:a\",\"B\":\"m:b\",\"C\":\"m:c\",\"D\":\"m:d\"");
        assertEquals("m:b:0", only(t.get(1, 0, 0)));
        assertEquals("m:c:0", only(t.get(0, 0, 1)));
    }

    @Test
    void blankIsAirByDefaultAndKeepWhenOff() throws Exception {
        assertTrue(
            parse("{\"rows\":[\" \"]}", "", true).get(0, 0, 0)
                .isAir());
        assertTrue(
            parse("{\"rows\":[\" \"]}", "", false).get(0, 0, 0)
                .isKeep());
    }

    @Test
    void underscoreIsAlwaysAirDotIsAlwaysKeep() throws Exception {
        for (boolean blankIsAir : new boolean[] { true, false }) {
            Template t = parse("{\"rows\":[\"_.\"]}", "", blankIsAir);
            assertTrue(
                t.get(0, 0, 0)
                    .isAir());
            assertTrue(
                t.get(1, 0, 0)
                    .isKeep());
        }
    }

    @Test
    void shortRowsPadWithBlank() throws Exception {
        Template t = parse("{\"rows\":[\"AAA\",\"A\"]}", "\"A\":\"m:a\"", false);
        assertEquals(3, t.sizeX);
        assertTrue(
            t.get(2, 0, 1)
                .isKeep());
    }

    @Test
    void bracedIdIsOneCell() throws Exception {
        Template t = parse("{\"rows\":[\"A{minecraft:wool:3}A\"]}", "\"A\":\"m:a\"");
        assertEquals(3, t.sizeX);
        assertEquals("minecraft:wool:3", only(t.get(1, 0, 0)));
    }

    @Test
    void anchorIsQ() throws Exception {
        Template t = parse("{\"rows\":[\"AAA\",\"AQA\"]},{\"rows\":[\"AAA\",\"AAA\"]}", "\"A\":\"m:a\",\"Q\":\"m:q\"");
        assertArrayEquals(new int[] { 1, 1, 1 }, new int[] { t.anchorX, t.anchorY, t.anchorZ });
        assertEquals("m:q:0", only(t.get(1, 1, 1)));
    }

    @Test
    void unmappedQActsBlank() throws Exception {
        Template t = parse("{\"rows\":[\"Q\"]}", "", false);
        assertTrue(
            t.get(0, 0, 0)
                .isKeep());
    }

    @Test
    void anchorDefaultsToBottomCentre() throws Exception {
        Template t = parse("{\"rows\":[\"AAAAA\",\"AAAAA\",\"AAAAA\",\"AAAAA\"]}", "\"A\":\"m:a\"");
        assertArrayEquals(new int[] { 2, 0, 2 }, new int[] { t.anchorX, t.anchorY, t.anchorZ });
    }

    @Test
    void mappingForms() throws Exception {
        Template t = parse(
            "{\"rows\":[\"ABCDE\"]}",
            "\"A\":\"m:a:4\",\"B\":\"_\",\"C\":{\"blocks\":[\"m:c1\",\"m:c2\"]},"
                + "\"D\":{\"blocks\":[\"m:d1\",\"m:d2\"],\"weights\":[3,1]},"
                + "\"E\":{\"block\":\"minecraft:chest:2\",\"loot\":\"etjourney:end_ruins\"}");
        assertEquals("m:a:4", only(t.get(0, 0, 0)));
        assertTrue(
            t.get(1, 0, 0)
                .isAir());
        assertEquals(
            "m:c1",
            t.get(2, 0, 0)
                .pick(0.49).name);
        assertEquals(
            "m:c2",
            t.get(2, 0, 0)
                .pick(0.51).name);
        assertEquals(
            "m:d1",
            t.get(3, 0, 0)
                .pick(0.74).name);
        assertEquals(
            "m:d2",
            t.get(3, 0, 0)
                .pick(0.76).name);
        BlockRef chest = t.get(4, 0, 0)
            .pick(0);
        assertEquals("minecraft:chest", chest.name);
        assertEquals(2, chest.meta);
        assertEquals("etjourney:end_ruins", chest.loot);
        assertNull(
            t.get(0, 0, 0)
                .pick(0).loot);
    }

    @Test
    void starMetaIsZero() throws Exception {
        assertEquals("m:a:0", only(parse("{\"rows\":[\"A\"]}", "\"A\":\"m:a:*\"").get(0, 0, 0)));
    }

    @Test
    void jointTakesItsFacingFromItsFace() throws Exception {
        // 3 wide, 1 high, 3 deep: one joint on each side.
        Template t = parse("{\"rows\":[\"A@A\",\"@A@\",\"A@A\"]}", "\"A\":\"m:a\",\"@\":\"m:seal\"");
        assertEquals(4, t.joints.size());
        java.util.Set<String> facings = new java.util.HashSet<>();
        for (Joint j : t.joints) facings.add(j.dx + "," + j.dz);
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("0,-1", "0,1", "-1,0", "1,0")), facings);
    }

    @Test
    void jointCellIsTheSealBlock() throws Exception {
        Template t = parse("{\"rows\":[\"AA\",\"@A\",\"AA\"]}", "\"A\":\"m:a\",\"@\":\"m:seal\"");
        assertEquals("m:seal:0", only(t.get(0, 0, 1)));
    }

    @Test
    void separateOpeningsOnOneFaceAreSeparateJoints() throws Exception {
        Template t = parse("{\"rows\":[\"AAA\",\"@AA\",\"AAA\",\"@AA\",\"AAA\"]}", "\"A\":\"m:a\",\"@\":\"m:seal\"");
        assertEquals(2, t.joints.size());
    }

    @Test
    void jointIsARectangle() throws Exception {
        Template t = parse(
            "{\"rows\":[\"AAA\",\"@AA\",\"@AA\",\"AAA\"]},{\"rows\":[\"AAA\",\"@AA\",\"@AA\",\"AAA\"]}",
            "\"A\":\"m:a\",\"@\":\"m:seal\"");
        assertEquals(1, t.joints.size());
        Joint j = t.joints.get(0);
        assertEquals(-1, j.dx);
        assertEquals(2, j.width);
        assertEquals(2, j.height);
        assertThrows(
            TemplateException.class,
            () -> parse(
                "{\"rows\":[\"AAA\",\"@AA\",\"AAA\"]},{\"rows\":[\"AAA\",\"@AA\",\"@AA\"]}",
                "\"A\":\"m:a\",\"@\":\"m:seal\""));
    }

    @Test
    void jointOffAFaceOrOnAnEdgeFails() {
        assertThrows(
            TemplateException.class,
            () -> parse("{\"rows\":[\"AAA\",\"A@A\",\"AAA\"]}", "\"A\":\"m:a\",\"@\":\"m:seal\""));
        assertThrows(
            TemplateException.class,
            () -> parse("{\"rows\":[\"@AA\",\"AAA\",\"AAA\"]}", "\"A\":\"m:a\",\"@\":\"m:seal\""));
    }

    @Test
    void jointNeedsASealMapping() {
        assertThrows(TemplateException.class, () -> parse("{\"rows\":[\"A@A\",\"AAA\"]}", "\"A\":\"m:a\""));
    }

    @Test
    void ignoresUnknownTopLevelKeys() throws Exception {
        String json = "[{\"name\":\"t\",\"displayName\":\"T\",\"requirements\":[],"
            + "\"layers\":[{\"name\":\"y1\",\"rows\":[\"A\"]}],\"mappings\":{\"A\":\"m:a\"}}]";
        assertEquals(1, TemplateParser.parse("t", new StringReader(json), true).sizeX);
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":\"m:a\"}",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":\"m:a\"}},{\"layers\":[],\"mappings\":{}}]",
            "[{\"layers\":[{\"rows\":[\"Z\"]}],\"mappings\":{}}]",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":{\"tiers\":{\"m:a\":1}}}}]",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":{\"component\":\"casing\",\"blocks\":[\"m:a\"]}}}]",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":{\"blocks\":[\"m:a\"],\"weights\":[1,2]}}}]",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":{\"blocks\":[\"m:a\"],\"weights\":[0]}}}]",
            "[{\"layers\":[{\"rows\":[\"A{m:b\"]}],\"mappings\":{\"A\":\"m:a\"}}]", "[]",
            "[{\"layers\":[{\"name\":\"y1\"}],\"mappings\":{}}]",
            "[{\"layers\":[{\"rows\":[\"A\"]}],\"mappings\":{\"A\":{\"blocks\":[\"m:a\"],\"weights\":[\"x\"]}}}]" })
    void rejects(String json) {
        assertThrows(TemplateException.class, () -> TemplateParser.parse("t", new StringReader(json), true));
    }
}
