package chlorine.etjourney.core.status.model;

/** Finds a free potion ID in the potion table. */
public final class PotionSlots {

    private PotionSlots() {}

    /** The preferred ID when it is free and at or above the first allowed; else the first free one; else -1. */
    public static int claim(Object[] table, int preferred, int firstAllowed) {
        if (preferred >= firstAllowed && preferred < table.length && table[preferred] == null) return preferred;
        for (int id = firstAllowed; id < table.length; id++) if (table[id] == null) return id;
        return -1;
    }
}
