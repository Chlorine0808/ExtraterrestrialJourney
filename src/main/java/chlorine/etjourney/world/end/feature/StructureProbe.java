package chlorine.etjourney.world.end.feature;

/** What placing a free-standing structure needs to know about the terrain, supplied by the engine. */
public interface StructureProbe {

    /** Weight of the named style at (x, z), base and overlay together. */
    double weight(String style, double x, double z);

    double land(double x, double z);

    /** Ground surface at (x, z), or a very low value over the void. */
    double ground(double x, double z);

    /** Centre of style region cell (cx, cz), whose cells are REGION_CELL wide; plain cell centres by default. */
    default double[] regionCentre(int cx, int cz) {
        return new double[] { (cx + 0.5) * REGION_CELL, (cz + 0.5) * REGION_CELL };
    }

    /** Width of a style region cell; the engine's region map uses the same. */
    int REGION_CELL = 720;
}
