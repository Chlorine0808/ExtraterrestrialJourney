package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** Parsing of the screenshot shot list: defaults, per-line errors that do not stop the rest, and view rules. */
class ShotListTest {

    @Test
    void tonightsListParsesWithoutErrors() throws IOException {
        ShotList list = ShotList.read(Paths.get("src/test/resources/shots/2026-10-28-end.txt"));
        assertEquals(Collections.emptyList(), list.errors);
        assertEquals(7037748451318983361L, list.seed);
        assertEquals(4 + 1 + 4 + 1 + 1, list.specs.size()); // biomes, bare, sphere classes, mountain, arc
    }

    @Test
    void defaultsApplyWhenSettingsAreMissing() {
        ShotList list = ShotList.parse(Collections.singletonList("shot bare"));
        assertEquals(ShotList.DEFAULT_SEED, list.seed);
        assertEquals("DEFAULT", list.worldType);
        assertEquals(3000, list.originX, 0);
        assertEquals(24, list.renderDistance);
        assertEquals(120, list.sphereCandidates);
        assertEquals(480, list.budgetMinutes);
        assertEquals(240, list.settleSeconds);
        assertEquals(1, list.specs.get(0).count);
        assertEquals(Arrays.asList(View.AERIAL, View.GROUND), list.specs.get(0).views); // all allowed views by default
    }

    @Test
    void badLinesAreReportedAndTheRestStillParses() {
        ShotList list = ShotList
            .parse(Arrays.asList("shot biome PURPLE", "shot biome GROVE count=x", "shot biome GROVE count=2"));
        assertEquals(2, list.errors.size());
        assertTrue(
            list.errors.get(0)
                .startsWith("line 1:"));
        assertEquals(1, list.specs.size());
        assertEquals(2, list.specs.get(0).count);
        assertEquals(3, list.specs.get(0).line);
    }

    @Test
    void viewNotAllowedForTheTargetIsAnError() {
        ShotList list = ShotList.parse(Collections.singletonList("shot biosphere CAVE views=ground"));
        assertEquals(1, list.errors.size());
        assertTrue(list.specs.isEmpty());
    }

    @Test
    void atLineKeepsTheCamera() {
        ShotSpec s = ShotList.parse(Collections.singletonList("shot at 3200 120 -40 yaw=45 pitch=30 label=gate")).specs
            .get(0);
        assertArrayEquals(new double[] { 3200, 120, -40, 45, 30 }, s.at, 0);
        assertEquals("gate", s.label);
    }

    @Test
    void settingsAndCommentsAreRead() {
        ShotList list = ShotList.parse(
            Arrays.asList(
                "# comment",
                "seed 5  # trailing",
                "origin 10 -20",
                "dry-run true",
                "budget-minutes 30",
                "shot mountain min-top=170"));
        assertEquals(Collections.emptyList(), list.errors);
        assertEquals(5L, list.seed);
        assertEquals(10, list.originX, 0);
        assertEquals(-20, list.originZ, 0);
        assertTrue(list.dryRun);
        assertEquals(30, list.budgetMinutes);
        assertEquals(170, list.specs.get(0).minTop);
        assertEquals(ShotSpec.What.MOUNTAIN, list.specs.get(0).what);
    }

    @Test
    void leadingByteOrderMarkIsIgnored() {
        ShotList list = ShotList.parse(Arrays.asList("﻿seed 5", "shot bare"));
        assertEquals(Collections.emptyList(), list.errors);
        assertEquals(5L, list.seed);
    }

    @Test
    void dryRunTakesOnlyTrueOrFalse() {
        ShotList list = ShotList.parse(Arrays.asList("dry-run yes", "dry-run false"));
        assertEquals(1, list.errors.size());
        assertTrue(
            list.errors.get(0)
                .startsWith("line 1:"));
        assertEquals(false, list.dryRun);
    }

    @Test
    void unknownSettingsAndShotKindsAreErrors() {
        ShotList list = ShotList.parse(Arrays.asList("bogus 1", "shot nothing", "shot biosphere LAVA"));
        assertEquals(3, list.errors.size());
        assertTrue(list.specs.isEmpty());
    }

    @Test
    void noPropertyMeansNoRun() {
        assertNull(ShotList.listPath(null));
        assertNull(ShotList.listPath("  "));
        assertEquals("a.txt", ShotList.listPath(" a.txt "));
    }

    @Test
    void giantTreeTakesCountAndViewsButNoName() {
        ShotList list = ShotList.parse(Collections.singletonList("shot giant-tree count=3 views=aerial,ground"));
        assertEquals(Collections.emptyList(), list.errors);
        ShotSpec spec = list.specs.get(0);
        assertEquals(ShotSpec.What.GIANT_TREE, spec.what);
        assertEquals(3, spec.count);
        assertEquals(Arrays.asList(View.AERIAL, View.GROUND), spec.views);
        assertNull(spec.style);
    }

    @Test
    void parsesGlowshroom() {
        ShotList list = ShotList.parse(
            Arrays.asList(
                "shot glowshroom NORMAL count=2 views=aerial,ground",
                "shot glowshroom",
                "shot glowshroom BIG"));
        assertEquals(1, list.specs.size());
        ShotSpec spec = list.specs.get(0);
        assertEquals(ShotSpec.What.GLOWSHROOM, spec.what);
        assertEquals("NORMAL", spec.arg);
        assertEquals(2, spec.count);
        assertEquals(2, list.errors.size());
    }

    @Test
    void styleKeyIsKnownForBiomeAndBareOnly() {
        ShotList list = ShotList.parse(
            Arrays.asList(
                "shot biome GROVE style=LAYERED",
                "shot bare style=RANGES",
                "shot biome GROVE style=NOPE",
                "shot mountain style=LAYERED",
                "shot giant-tree style=LAYERED"));
        assertEquals("LAYERED", list.specs.get(0).style);
        assertEquals("RANGES", list.specs.get(1).style);
        assertEquals(2, list.specs.size());
        assertEquals(3, list.errors.size());
        assertTrue(
            list.errors.get(0)
                .contains("unknown style 'NOPE'"),
            list.errors.get(0));
        assertTrue(
            list.errors.get(1)
                .contains("style"),
            list.errors.get(1));
    }
}
