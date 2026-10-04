package chlorine.etjourney.world.end.reserve;

/** A disc of the outer End that another mod builds on (block coordinates). */
public final class Area {

    public final String label;
    public final double x, z, radius;

    public Area(String label, double x, double z, double radius) {
        this.label = label;
        this.x = x;
        this.z = z;
        this.radius = radius;
    }
}
