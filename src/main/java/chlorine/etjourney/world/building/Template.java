package chlorine.etjourney.world.building;

import java.util.Collections;
import java.util.List;

/** A building read from a file: its cells with y 0 at the bottom, and the anchor it is placed and turned by. */
public final class Template {

    public final String name;
    public final int sizeX, sizeY, sizeZ;
    public final int anchorX, anchorY, anchorZ;
    /** Where other rooms may join; empty for a building that stands alone. */
    public final List<Joint> joints;
    private final Cell[] cells;

    Template(String name, int sizeX, int sizeY, int sizeZ, Cell[] cells, int anchorX, int anchorY, int anchorZ) {
        this(name, sizeX, sizeY, sizeZ, cells, anchorX, anchorY, anchorZ, Collections.emptyList());
    }

    Template(String name, int sizeX, int sizeY, int sizeZ, Cell[] cells, int anchorX, int anchorY, int anchorZ,
        List<Joint> joints) {
        this.joints = Collections.unmodifiableList(joints);
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.cells = cells;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    static int index(int sizeX, int sizeZ, int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public Cell get(int x, int y, int z) {
        return cells[index(sizeX, sizeZ, x, y, z)];
    }
}
