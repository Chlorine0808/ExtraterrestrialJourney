package chlorine.etjourney.world.building;

import java.util.BitSet;

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
    /** Template cells of the joints another room has joined; they are written as air. */
    private final BitSet open = new BitSet();

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

    /** Marks a joint as joined, so its cells are written as air instead of sealed. */
    public void open(Joint joint) {
        for (int[] c : joint.cells) open.set(Template.index(template.sizeX, template.sizeZ, c[0], c[1], c[2]));
    }

    public boolean isOpen(Joint joint) {
        int[] c = joint.cells.get(0);
        return isOpen(c[0], c[1], c[2]);
    }

    boolean isOpen(int x, int y, int z) {
        return open.get(Template.index(template.sizeX, template.sizeZ, x, y, z));
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
