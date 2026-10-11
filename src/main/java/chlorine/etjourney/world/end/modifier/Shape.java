package chlorine.etjourney.world.end.modifier;

import java.util.function.DoublePredicate;

/** A 3D part of the terrain: density is positive inside, roughly the distance to its surface. */
public interface Shape {

    double minX();

    double maxX();

    double minZ();

    double maxZ();

    /** Lowest Y the shape can be solid at; the whole world by default. */
    default double minY() {
        return 0;
    }

    /** Highest Y the shape can be solid at; the tall pass runs when a shape reaches above the generator's 127. */
    default double maxY() {
        return 256;
    }

    double density(double x, double y, double z);

    /** The shape along the vertical line through (x, z), for drawing it block by block. */
    default Line line(double x, double z) {
        return Line.of(minY(), maxY(), y -> density(x, y, z) >= 0);
    }

    /**
     * One vertical line of a shape: solid at y exactly where the shape's density there is not negative, and never
     * outside minY to maxY.
     */
    interface Line {

        Line EMPTY = of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, y -> false);

        double minY();

        double maxY();

        /** Only asked within minY to maxY. */
        boolean solid(double y);

        static Line of(double minY, double maxY, DoublePredicate solid) {
            return new Line() {

                @Override
                public double minY() {
                    return minY;
                }

                @Override
                public double maxY() {
                    return maxY;
                }

                @Override
                public boolean solid(double y) {
                    return solid.test(y);
                }
            };
        }
    }
}
