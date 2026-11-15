package chlorine.etjourney.content.rank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** What the client knows of ranked mobs, by entity ID; filled from the network thread. */
public final class RankViews {

    public static final class View {

        public final Rank rank;
        public final float scale;
        /** Client ticks the entity has been missing from the world. */
        public int missing;

        View(Rank rank, float scale) {
            this.rank = rank;
            this.scale = scale;
        }
    }

    public static final Map<Integer, View> VIEWS = new ConcurrentHashMap<>();

    private RankViews() {}

    static void put(int entityId, Rank rank, float scale) {
        VIEWS.put(entityId, new View(rank, scale));
    }

    public static View get(int entityId) {
        return VIEWS.get(entityId);
    }
}
