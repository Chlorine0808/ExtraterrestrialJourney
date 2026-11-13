package chlorine.etjourney.content.rank;

/** The chance of each rank per stage (provisional) and the roll over them. */
public final class RankOdds {

    /** Chances of RARE, EPIC and LEGENDARY, by stage. */
    private static final double[][] CHANCES = { { 0.04, 0, 0 }, { 0.08, 0.02, 0 }, { 0.12, 0.05, 0.01 } };

    private RankOdds() {}

    public static double chance(RankStage stage, Rank rank) {
        return rank == Rank.NORMAL ? 0 : CHANCES[stage.ordinal()][rank.ordinal() - 1];
    }

    /** Picks a rank from a uniform draw in [0, 1), highest rank first. */
    public static Rank pick(RankStage stage, double u) {
        Rank[] all = Rank.values();
        double edge = 0;
        for (int i = all.length - 1; i > 0; i--) {
            edge += chance(stage, all[i]);
            if (u < edge) return all[i];
        }
        return Rank.NORMAL;
    }
}
