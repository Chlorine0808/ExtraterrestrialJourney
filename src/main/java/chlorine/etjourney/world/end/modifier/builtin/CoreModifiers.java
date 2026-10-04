package chlorine.etjourney.world.end.modifier.builtin;

import java.util.Arrays;
import java.util.List;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Land;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Relief;
import chlorine.etjourney.world.end.feature.Underside;
import chlorine.etjourney.world.end.feature.Valleys;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.TerrainView;

/** Column modifiers every region runs at full weight; PLAINS is these alone. */
public final class CoreModifiers {

    public static final int CONTINENT = 100, LEVEL = 200, HILLS = 300, MOUNTAINS = 400, UNDERSIDE = 600, SURFACE = 610;
    /** Land removed at full suppression next to a reserved area. */
    private static final double SUPPRESSED_LAND = 120;
    /** Highest surface built; above the generator's 127, the tall pass writes from the same density. */
    public static final double MAX_TOP = 250;

    private CoreModifiers() {}

    public static List<Modifier> all() {
        return Arrays.asList(
            simple(CONTINENT, CoreModifiers::continent),
            simple(LEVEL, CoreModifiers::level),
            simple(HILLS, CoreModifiers::hills),
            simple(MOUNTAINS, CoreModifiers::mountains),
            simple(UNDERSIDE, CoreModifiers::underside),
            simple(SURFACE, CoreModifiers::surface));
    }

    interface ColumnStep {

        void apply(ChunkArea area, TerrainView view, ColumnState s, double weight);
    }

    static Modifier simple(int order, ColumnStep step) {
        return new Modifier() {

            @Override
            public int order() {
                return order;
            }

            @Override
            public void column(ChunkArea area, ColumnState state, double weight) {
                step.apply(area, area.view, state, weight);
            }
        };
    }

    private static void continent(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        s.land = Land.height(area.seed, view.seeds(), view.mountains(), s.x, s.z, view.voidShare(s.x, s.z))
            - s.suppression * SUPPRESSED_LAND;
    }

    private static void level(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        double[] w = Continent.warped(area.seed, s.x, s.z);
        s.level = Relief.level(area.seed, w[0], w[1]);
    }

    private static void hills(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        double[] w = Continent.warped(area.seed, s.x, s.z);
        s.hills = Relief.hills(area.seed, w[0], w[1], s.x, s.z) * s.interior * (1 - s.suppression) * s.hillScale;
    }

    private static void mountains(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        double[] w = Continent.warped(area.seed, s.x, s.z);
        double rise = Mountains.rise(view.mountains(), w[0], w[1]);
        if (rise <= 0) return;
        double interior = s.interior * s.interior * (3 - 2 * s.interior);
        s.rise += rise * (0.65 + 0.6 * Relief.ridges(area.seed, s.x, s.z))
            * interior
            * (1 - s.suppression)
            * view.mountainScale(s.x, s.z);
    }

    private static void underside(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        double body = s.level + s.hills * 0.5
            - Underside.depth(area.seed, s.land, s.x, s.z) * s.undersideScale
            - s.rise * 0.25;
        double shift = Underside.shift(body);
        body += shift;
        s.level += shift;
        // Relief hangs below the placed slab, only as far as the world floor; it never moves the surface.
        s.bottom = body - Math.min(s.hang, Math.max(0, body - Underside.MIN_BOTTOM));
    }

    /** Ground with valleys cut in and lakes shaped; valleys may cut through, lake beds keep MIN_FLOOR of rock. */
    private static void surface(ChunkArea area, TerrainView view, ColumnState s, double weight) {
        double uncut = s.level + s.hills + s.rise;
        double cut = uncut - Valleys.depth(area.seed, s.x, s.z) * s.interior * view.valleyScale(s.x, s.z);
        double top = Lakes.shape(view.lakes(), cut, s.x, s.z);
        if (top < cut) top = Math.max(top, Math.min(cut, s.bottom + Lakes.MIN_FLOOR));
        s.top = Math.min(MAX_TOP, top);
    }
}
