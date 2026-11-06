package chlorine.etjourney.world.building;

import java.util.Collections;
import java.util.List;

/** A building grown from a start room by joining rooms from a pool, within a depth and a count. */
public final class RoomSet {

    public final String id;
    public final String start;
    public final List<String> pool;
    /** Joins counted from the start room. */
    public final int maxDepth;
    /** Rooms in all, the start included. */
    public final int maxRooms;

    public RoomSet(String id, String start, List<String> pool, int maxDepth, int maxRooms) {
        this.id = id;
        this.start = start;
        this.pool = Collections.unmodifiableList(pool);
        this.maxDepth = maxDepth;
        this.maxRooms = maxRooms;
    }
}
