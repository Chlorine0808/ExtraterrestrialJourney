package chlorine.etjourney.world.end.shoot;

import java.util.List;

/** One shot the run intends to take; target is null when none was found (then missing says why). */
public final class PlannedShot {

    public final String group;
    public final int index;
    public final View view;
    public final Target target;
    /** Fixed camera, taken unchecked (AT shots, glowshrooms), otherwise null. */
    public final Camera fixed;
    /** Cameras to try in order, each checked like a searched one, instead of the view's own search; or null. */
    public final List<Camera> tries;
    /** How far in front of the target a hit still counts as the subject, such as a tree's crown; 0 otherwise. */
    public final double depth;
    /** Biosphere site text, or null. */
    public final String source;
    /** Reason when target is null. */
    public final String missing;

    public PlannedShot(String group, int index, View view, Target target, Camera fixed, String source, String missing) {
        this(group, index, view, target, fixed, null, 0, source, missing);
    }

    public PlannedShot(String group, int index, View view, Target target, Camera fixed, List<Camera> tries,
        double depth, String source, String missing) {
        this.group = group;
        this.index = index;
        this.view = view;
        this.target = target;
        this.fixed = fixed;
        this.tries = tries;
        this.depth = depth;
        this.source = source;
        this.missing = missing;
    }
}
