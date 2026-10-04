package chlorine.etjourney.world.end.modifier;

/** A 3D part of the terrain: density is positive inside, roughly the distance to its surface. */
public interface Shape {

    double minX();

    double maxX();

    double minZ();

    double maxZ();

    double density(double x, double y, double z);
}
