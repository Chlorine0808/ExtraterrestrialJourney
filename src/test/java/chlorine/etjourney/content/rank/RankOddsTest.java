package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The roll walks the stage's chances from the top rank down; whatever is left over stays normal. */
class RankOddsTest {

    @Test
    void earlyStageRollsRareBelowItsChanceAndNormalAbove() {
        double rare = RankOdds.chance(RankStage.EARLY, Rank.RARE);
        assertEquals(Rank.RARE, RankOdds.pick(RankStage.EARLY, 0));
        assertEquals(Rank.RARE, RankOdds.pick(RankStage.EARLY, rare - 1e-9));
        assertEquals(Rank.NORMAL, RankOdds.pick(RankStage.EARLY, rare));
        assertEquals(Rank.NORMAL, RankOdds.pick(RankStage.EARLY, 0.999));
    }

    @Test
    void earlyStageNeverRollsAboveRare() {
        for (double u = 0; u < 1; u += 0.001) {
            assertEquals(
                true,
                RankOdds.pick(RankStage.EARLY, u)
                    .ordinal() <= Rank.RARE.ordinal());
        }
    }

    @Test
    void laterStagesRollHigherRanksFirst() {
        double legendary = RankOdds.chance(RankStage.LATE, Rank.LEGENDARY);
        double epic = RankOdds.chance(RankStage.LATE, Rank.EPIC);
        assertEquals(Rank.LEGENDARY, RankOdds.pick(RankStage.LATE, legendary / 2));
        assertEquals(Rank.EPIC, RankOdds.pick(RankStage.LATE, legendary + epic / 2));
        assertEquals(Rank.RARE, RankOdds.pick(RankStage.LATE, legendary + epic + 1e-9));
        assertEquals(Rank.EPIC, RankOdds.pick(RankStage.MID, 0));
    }

    @Test
    void laterStagesRankMoreOften() {
        assertEquals(true, ranked(RankStage.MID) > ranked(RankStage.EARLY));
        assertEquals(true, ranked(RankStage.LATE) > ranked(RankStage.MID));
    }

    private static double ranked(RankStage stage) {
        double sum = 0;
        for (Rank r : Rank.values()) if (r != Rank.NORMAL) sum += RankOdds.chance(stage, r);
        return sum;
    }
}
