package chlorine.etjourney.content.rank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** What the client knows of ranked mobs, by entity ID; filled from the network thread. */
public final class RankViews {

    public static final class View {

        public final Rank rank;
        public final boolean small;
        /** Client ticks the entity has been missing from the world. */
        public int missing;

        View(Rank rank, boolean small) {
            this.rank = rank;
            this.small = small;
        }
    }

    public static final Map<Integer, View> VIEWS = new ConcurrentHashMap<>();

    private RankViews() {}

    static void put(int entityId, Rank rank, boolean small) {
        VIEWS.put(entityId, new View(rank, small));
    }

    public static View get(int entityId) {
        return VIEWS.get(entityId);
    }
}
