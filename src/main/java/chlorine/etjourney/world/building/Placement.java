package chlorine.etjourney.world.building;

/** A template set in the world: its turn, where its anchor lands, and what fills under it. */
public final class Placement {

    public final Template template;
    public final Rotation rotation;
    public final int anchorX, anchorY, anchorZ;
    /** Fills down from under the bottom layer's blocks; null for none. */
    public final BlockRef foundation;
    public final int foundationDepth;
    /** Seeds the choice between a cell's blocks. */
    public final long salt;
    private final int[] bounds;

    public Placement(Template template, Rotation rotation, int anchorX, int anchorY, int anchorZ, BlockRef foundation,
        int foundationDepth, long salt) {
        this.template = template;
        this.rotation = rotation;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.foundation = foundation;
        this.foundationDepth = foundation == null ? 0 : foundationDepth;
        this.salt = salt;
        this.bounds = rotation.rotatedBounds(template);
    }

    public int minX() {
        return anchorX + bounds[0];
    }

    public int maxX() {
        return anchorX + bounds[2];
    }

    public int minZ() {
        return anchorZ + bounds[1];
    }

    public int maxZ() {
        return anchorZ + bounds[3];
    }

    /** World y of the bottom layer. */
    public int bottomY() {
        return anchorY - template.anchorY;
    }

    public int minY() {
        return bottomY() - foundationDepth;
    }

    public int maxY() {
        return bottomY() + template.sizeY - 1;
    }
}
