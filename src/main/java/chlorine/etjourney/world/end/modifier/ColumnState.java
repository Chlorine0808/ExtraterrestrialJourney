package chlorine.etjourney.world.end.modifier;

import java.util.ArrayList;
import java.util.List;

/** Mutable shape of one terrain column, rewritten by the modifiers in order. */
public final class ColumnState {

    /** Land height at which hills and mountains reach full size. */
    public static final double FULL_INTERIOR = 60;

    public final double x, z;
    /** Continent height: 0 at the rim, up to 80 inside, negative over the void. */
    public double land;
    /** 0 to 1: how much an area another mod builds on suppresses our terrain here. */
    public double suppression;
    /** clamp(land / FULL_INTERIOR, 0, 1); refreshed by the chain before each modifier. */
    public double interior;
    public double level, top, bottom;
    /** Hills and mountain rise, kept apart because the underside follows half the hills and a quarter of the rise. */
    public double hills, rise;
    /** Scales other modifiers apply to hills and the underside depth (LOWLANDS shrinks both). */
    public double hillScale = 1, undersideScale = 1;
    /** Positive where a pillar joins the ground to the stacked slabs. */
    public double pillar;
    public final List<Layer> layers = new ArrayList<>();

    public ColumnState(double x, double z) {
        this.x = x;
        this.z = z;
    }

    void refreshInterior() {
        interior = Math.min(1, Math.max(0, land / FULL_INTERIOR));
    }
}
