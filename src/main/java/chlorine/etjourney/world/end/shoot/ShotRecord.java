package chlorine.etjourney.world.end.shoot;

/** Mutable outcome of one planned shot, filled in by the harness as the run proceeds. */
public final class ShotRecord {

    public enum Status {
        PENDING,
        OK,
        FAILED,
        MISSING,
        TIMED_OUT
    }

    public final PlannedShot shot;
    public Status status = Status.PENDING;
    /** File name inside the run folder, or null. */
    public String image;
    public String reason;
    public Camera camera;
    public String biome;
    public String styles;
    public int attempts;
    public int width, height;

    public ShotRecord(PlannedShot shot) {
        this.shot = shot;
    }
}
