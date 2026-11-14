package chlorine.etjourney.content.area;

/** How an area's radius changes: grows in, shrinks out at the end, wears down with each use. */
public final class AreaSize {

    public final double startRadius, radius;
    public final int growTicks, shrinkTicks;
    public final double radiusPerUse, minRadius;
    public final int duration;

    public AreaSize(double startRadius, double radius, int growTicks, int shrinkTicks, double radiusPerUse,
        double minRadius, int duration) {
        this.startRadius = startRadius;
        this.radius = radius;
        this.growTicks = growTicks;
        this.shrinkTicks = shrinkTicks;
        this.radiusPerUse = radiusPerUse;
        this.minRadius = minRadius;
        this.duration = duration;
    }

    public double radius(int age, int uses) {
        double r = radius;
        if (growTicks > 0 && age < growTicks) r = startRadius + (radius - startRadius) * age / growTicks;
        int left = duration - age;
        if (shrinkTicks > 0 && left < shrinkTicks) r = Math.min(r, radius * Math.max(0, left) / shrinkTicks);
        return Math.max(0, r - uses * radiusPerUse);
    }

    public boolean alive(int age, int uses) {
        return age < duration && radius(age, uses) >= minRadius;
    }
}
