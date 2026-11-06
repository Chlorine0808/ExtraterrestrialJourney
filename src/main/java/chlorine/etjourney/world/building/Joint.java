package chlorine.etjourney.world.building;

import java.util.Collections;
import java.util.List;

/** An opening of '@' cells on one side of a room, where another room may join; faces (dx, 0, dz) outward. */
public final class Joint {

    public final int dx, dz;
    /** Cells along the side and up. */
    public final int width, height;
    /** Template positions {x, y, z} of the cells. */
    public final List<int[]> cells;

    Joint(int dx, int dz, int width, int height, List<int[]> cells) {
        this.dx = dx;
        this.dz = dz;
        this.width = width;
        this.height = height;
        this.cells = Collections.unmodifiableList(cells);
    }

    /** Whether another joint can meet this one face to face. */
    public boolean fits(Joint other) {
        return width == other.width && height == other.height;
    }
}
