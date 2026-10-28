package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Shot list expansion on the screenshot seed. The sample worlds are faked: a ball's site is picked from its key,
 * so every class turns up among the candidates, or (for the shortfall case) no ball has a site at all.
 */
class ShotPlannerTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(
        ShotList.DEFAULT_SEED,
        new RegionPicker(Styles.all()));

    private static final ShotPlanner.Sites BY_KEY = b -> {
        int k = (int) Math.floorMod(b.key(), 4L);
        BiosphereSource.Kind kind = k == 2 ? BiosphereSource.Kind.CAVE
            : k == 3 ? BiosphereSource.Kind.NETHER : BiosphereSource.Kind.SURFACE;
        return new ShotPlanner.Site(kind, 10 * k, -5 * k, k == 1);
    };

    private static ShotList list(String... lines) {
        return ShotList.parse(Arrays.asList(lines));
    }

    @Test
    void tonightsListExpandsToThirtySixNumberedShots() {
        ShotList l = list(
            "sphere-candidates 120",
            "shot biome GROVE count=2 views=aerial,ground",
            "shot biome HIGHLAND count=2 views=aerial,ground",
            "shot biome MYCELIA count=2 views=aerial,ground",
            "shot biome AMBER count=2 views=aerial,ground",
            "shot bare count=1 views=aerial,ground",
            "shot biosphere SURFACE count=2 views=outside,inside",
            "shot biosphere WATER count=2 views=outside,inside",
            "shot biosphere CAVE count=2 views=outside,inside",
            "shot biosphere NETHER count=2 views=outside,inside",
            "shot mountain min-top=150 count=1 views=aerial",
            "shot arc-touch count=1 views=aerial");
        List<PlannedShot> shots = ShotPlanner.expand(l, SAMPLER, BY_KEY);
        assertEquals(36, shots.size());
        for (int i = 0; i < shots.size(); i++) {
            PlannedShot s = shots.get(i);
            assertEquals(i + 1, s.index);
            assertNotNull(s.target, s.group);
            assertNull(s.missing, s.group);
        }
        assertEquals("GROVE 大陸 1", shots.get(0).group);
        assertEquals(View.AERIAL, shots.get(0).view);
        assertEquals("GROVE 大陸 1", shots.get(1).group);
        assertEquals(View.GROUND, shots.get(1).view);
        assertEquals("GROVE 大陸 2", shots.get(2).group);
        assertEquals("bare 大陸 1", shots.get(16).group);
        assertEquals("地表の球 1", shots.get(18).group);
        assertEquals("水の球 2", shots.get(24).group);
        assertTrue(shots.get(24).source.endsWith("（水）"), shots.get(24).source);
        assertEquals("ネザーの球 1", shots.get(30).group);
        assertTrue(shots.get(30).source.startsWith("NETHER from "), shots.get(30).source);
        assertEquals("山 1", shots.get(34).group);
        assertEquals("ARCS 接地 1", shots.get(35).group);
    }

    @Test
    void ballsWithoutSitesBecomeMissingShots() {
        ShotList l = list("sphere-candidates 5", "shot biosphere CAVE count=1 views=outside,inside");
        List<PlannedShot> shots = ShotPlanner.expand(l, SAMPLER, b -> null);
        assertEquals(2, shots.size());
        for (PlannedShot s : shots) {
            assertEquals("洞窟の球 1", s.group);
            assertNull(s.target);
            assertEquals("候補 5 件中に見つからず", s.missing);
        }
    }

    @Test
    void fixedShotKeepsItsCamera() {
        List<PlannedShot> shots = ShotPlanner
            .expand(list("shot at 3200 120 -40 yaw=45 pitch=30 label=example"), SAMPLER, BY_KEY);
        assertEquals(1, shots.size());
        PlannedShot s = shots.get(0);
        assertEquals("example", s.group);
        assertEquals(3200, s.fixed.x, 1e-9);
        assertEquals(120, s.fixed.y, 1e-9);
        assertEquals(-40, s.fixed.z, 1e-9);
        assertEquals(45, s.fixed.yaw, 1e-6);
        assertEquals(30, s.fixed.pitch, 1e-6);
        assertSame(s.fixed, ShotPlanner.firstCamera(s, (x, z) -> null));
    }

    @Test
    void sourceNamesTheKindPointAndWater() {
        assertEquals(
            "SURFACE from 10,-20（水）",
            ShotPlanner.source(new ShotPlanner.Site(BiosphereSource.Kind.SURFACE, 10, -20, true)));
        assertEquals("CAVE from 1,2", ShotPlanner.source(new ShotPlanner.Site(BiosphereSource.Kind.CAVE, 1, 2, false)));
    }

    @Test
    void firstCameraFollowsTheView() {
        Target t = new Target("t", 100, 80, 200, 40, 7);
        Camera aerial = ShotPlanner.firstCamera(new PlannedShot("g", 1, View.AERIAL, t, null, null, null), null);
        Camera expected = Framing.aerial(t, 0);
        assertEquals(expected.x, aerial.x, 1e-9);
        assertEquals(expected.yaw, aerial.yaw, 1e-6);
        Camera ground = ShotPlanner
            .firstCamera(new PlannedShot("g", 1, View.GROUND, t, null, null, null), (x, z) -> null);
        assertEquals(t.x, ground.x, 1e-9);
        assertEquals(t.y, ground.y, 1e-9);
        assertNull(ShotPlanner.firstCamera(new PlannedShot("g", 1, View.AERIAL, null, null, null, "none"), null));
    }
}
