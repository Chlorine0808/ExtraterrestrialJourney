package chlorine.etjourney.world.end.modifier;

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
}
