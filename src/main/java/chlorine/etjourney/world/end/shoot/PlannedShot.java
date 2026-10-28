package chlorine.etjourney.world.end.shoot;

/** One shot the run intends to take; target is null when none was found (then missing says why). */
public final class PlannedShot {

    public final String group;
    public final int index;
    public final View view;
    public final Target target;
    /** Fixed camera for AT shots only, otherwise null. */
    public final Camera fixed;
    /** Biosphere site text, or null. */
    public final String source;
    /** Reason when target is null. */
    public final String missing;

    public PlannedShot(String group, int index, View view, Target target, Camera fixed, String source, String missing) {
        this.group = group;
        this.index = index;
        this.view = view;
        this.target = target;
        this.fixed = fixed;
        this.source = source;
        this.missing = missing;
    }
}
