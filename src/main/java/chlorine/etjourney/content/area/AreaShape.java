package chlorine.etjourney.content.area;

/** An upright cylinder: a floor rises from where it lies, a cloud spreads above and below its centre. */
public enum AreaShape {

    FLOOR,
    CLOUD;

    public double bottom(double y, double height) {
        return this == FLOOR ? y : y - height / 2;
    }

    public double top(double y, double height) {
        return bottom(y, height) + height;
    }

    /**
     * Whether a box, given by its centre, half width and vertical span, overlaps the area, which reaches headroom
     * further up.
     */
    public boolean overlaps(double radius, double height, double headroom, double ax, double ay, double az, double bx,
        double bz, double halfWidth, double minY, double maxY) {
        if (maxY < bottom(ay, height) || minY > top(ay, height) + headroom) return false;
        double dx = bx - ax, dz = bz - az, reach = radius + halfWidth;
        return dx * dx + dz * dz <= reach * reach;
    }
}
