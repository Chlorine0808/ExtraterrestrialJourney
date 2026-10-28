package chlorine.etjourney.world.end.shoot;

import java.util.Arrays;
import java.util.List;

/** What to do with the screenshot save before launching it: create it, reuse it with a fresh End, or rebuild it. */
public final class WorldPlan {

    public enum Action {
        CREATE,
        LOAD_WITH_FRESH_END,
        RECREATE
    }

    /** Folder and display name of the screenshot save. */
    public static final String SAVE = "etj-shoot";
    /** Save subfolders holding End chunks (vanilla and HEE), removed so the End is generated anew. */
    public static final List<String> END_DIRS = Arrays.asList("DIM1", "hee");

    private WorldPlan() {}

    /** The action for a save whose seed is savedSeed (null when there is no save) when wanted is the seed to shoot. */
    public static Action decide(Long savedSeed, long wanted) {
        if (savedSeed == null) return Action.CREATE;
        return savedSeed == wanted ? Action.LOAD_WITH_FRESH_END : Action.RECREATE;
    }
}
