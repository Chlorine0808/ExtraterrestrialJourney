package chlorine.etjourney.world.end.shoot;

import java.util.List;

/** One parsed "shot" line of the shot list. */
public final class ShotSpec {

    public enum What {
        BIOME,
        BARE,
        BIOSPHERE,
        MOUNTAIN,
        ARC_TOUCH,
        GIANT_TREE,
        GLOWSHROOM,
        AMBER_TREE,
        LAKE,
        BUILDING,
        AT
    }

    public final What what;
    /** Biome or sphere class name, or null. */
    public final String arg;
    public final int count;
    public final List<View> views;
    public final int minTop;
    /** {x, y, z, yaw, pitch} for AT only, else null. */
    public final double[] at;
    public final String label;
    /** Terrain style the continent point or the tree's root must be dominated by (biome, bare, amber-tree), or null. */
    public final String style;
    /** 1-based line number in the list file. */
    public final int line;

    ShotSpec(What what, String arg, int count, List<View> views, int minTop, double[] at, String label, String style,
        int line) {
        this.what = what;
        this.arg = arg;
        this.count = count;
        this.views = views;
        this.minTop = minTop;
        this.at = at;
        this.label = label;
        this.style = style;
        this.line = line;
    }
}
