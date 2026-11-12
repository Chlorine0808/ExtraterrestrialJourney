package chlorine.etjourney.content.rank;

/** Strengthens a status effect that a ranked mob's hit gave or lengthened. */
public final class EffectBoost {

    /** The duration of an effect the target did not have before the hit. */
    public static final int ABSENT = -1;

    private EffectBoost() {}

    /** The boosted duration and level, or null when the hit did not touch the effect. */
    public static int[] boost(int durationBefore, int levelBefore, int durationAfter, int levelAfter, Rank rank) {
        if (rank == Rank.NORMAL) return null;
        boolean touched = durationBefore == ABSENT || levelAfter > levelBefore || durationAfter > durationBefore;
        if (!touched) return null;
        return new int[] { (int) Math.round(durationAfter * rank.effectLength), levelAfter + rank.effectLevels };
    }
}
