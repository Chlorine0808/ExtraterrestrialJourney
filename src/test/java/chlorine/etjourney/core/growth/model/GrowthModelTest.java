package chlorine.etjourney.core.growth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GrowthModelTest {

    @Test
    void levelsFollowTheSquareCurve() {
        assertEquals(0, Leveling.level(0));
        assertEquals(0, Leveling.level(9));
        assertEquals(1, Leveling.level(10));
        // 10 + 40 to reach level 2.
        assertEquals(1, Leveling.level(49));
        assertEquals(2, Leveling.level(50));
        assertEquals(Leveling.MAX_LEVEL, Leveling.level(Leveling.totalFor(Leveling.MAX_LEVEL)));
        assertEquals(Leveling.MAX_LEVEL, Leveling.level(Integer.MAX_VALUE));
        assertEquals(3850, Leveling.totalFor(Leveling.MAX_LEVEL));
    }

    @Test
    void progressCountsWithinTheLevel() {
        int[] p = Leveling.progress(60);
        assertEquals(10, p[0]);
        assertEquals(90, p[1]);
    }

    @Test
    void levelBonusesStaySmall() {
        assertEquals(0.2f, GrowthBonus.weapon(10), 1e-6);
        assertEquals(0.1f, GrowthBonus.accessory(10), 1e-6);
        assertEquals(1f, GrowthBonus.armor(), 1e-6);
        assertEquals(0.95f * 0.95f, GrowthBonus.armor(10, 10), 1e-6);
    }

    @Test
    void aHitGivesItsRoundedAmountAtLeastOne() {
        assertEquals(1, GrowthBonus.xpForHit(0.2f));
        assertEquals(7, GrowthBonus.xpForHit(6.6f));
    }

    @Test
    void xpStopsAtTheMaximum() {
        GrowthData d = new GrowthData(3840, Rarity.COMMON, new ArrayList<>());
        assertEquals(3850, d.withXp(100).xp);
        assertEquals(
            3850,
            d.withXp(100)
                .withXp(5).xp);
        assertEquals(3840, d.withXp(-5).xp);
    }

    @Test
    void rarityPicksByCumulativeWeight() {
        // Weights 60, 25, 10, 4, 1 out of 100.
        assertEquals(Rarity.COMMON, RarityTable.pick(0));
        assertEquals(Rarity.COMMON, RarityTable.pick(0.599));
        assertEquals(Rarity.UNCOMMON, RarityTable.pick(0.6));
        assertEquals(Rarity.RARE, RarityTable.pick(0.85));
        assertEquals(Rarity.EPIC, RarityTable.pick(0.95));
        assertEquals(Rarity.LEGENDARY, RarityTable.pick(0.99));
        assertEquals(Rarity.LEGENDARY, RarityTable.pick(0.99999));
    }

    @Test
    void rarityNamesRoundTrip() {
        for (Rarity r : Rarity.values()) assertEquals(r, Rarity.byName(r.id));
        assertEquals(null, Rarity.byName("nonsense"));
    }

    private static List<AffixDraw.Candidate> pool(String... ids) {
        List<AffixDraw.Candidate> out = new ArrayList<>();
        for (String id : ids) out.add(new AffixDraw.Candidate(id, 10));
        return out;
    }

    @Test
    void drawPicksDistinctAffixesFromThePool() {
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            List<AffixRoll> rolls = AffixDraw.draw(pool("a", "b", "c", "d"), 3, 0, random);
            assertEquals(3, rolls.size());
            Set<String> ids = new HashSet<>();
            for (AffixRoll r : rolls) {
                ids.add(r.id);
                assertTrue(
                    Arrays.asList("a", "b", "c", "d")
                        .contains(r.id));
            }
            assertEquals(3, ids.size());
        }
    }

    @Test
    void drawStopsWhenThePoolRunsOut() {
        assertEquals(
            2,
            AffixDraw.draw(pool("a", "b"), 4, 0, new Random(1))
                .size());
        assertEquals(
            0,
            AffixDraw.draw(pool(), 2, 0, new Random(1))
                .size());
    }

    @Test
    void drawSkipsZeroWeights() {
        List<AffixDraw.Candidate> pool = Arrays
            .asList(new AffixDraw.Candidate("never", 0), new AffixDraw.Candidate("always", 5));
        for (int i = 0; i < 50; i++) {
            List<AffixRoll> rolls = AffixDraw.draw(pool, 2, 0, new Random(i));
            assertEquals(1, rolls.size());
            assertEquals("always", rolls.get(0).id);
        }
    }

    @Test
    void rollsStayAboveTheRarityFloor() {
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            for (AffixRoll r : AffixDraw.draw(pool("a", "b"), 2, 0.75f, random)) {
                assertTrue(r.roll >= 0.75f && r.roll < 1, "roll " + r.roll);
            }
        }
    }

    @Test
    void rollingGivesTheRarityItsAffixCount() {
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            GrowthData d = GrowthData.roll(pool("a", "b", "c", "d", "e"), random);
            assertEquals(0, d.xp);
            assertEquals(d.rarity.affixes, d.affixes.size());
        }
    }

    @Test
    void reforgeKeepsRarityAndXpAndRedrawsTheAffixes() {
        GrowthData before = new GrowthData(500, Rarity.EPIC, Arrays.asList(new AffixRoll("old", 0.9f)));
        Random random = new Random(11);
        GrowthData after = Reforge.reroll(before, pool("a", "b", "c", "d"), random);
        assertEquals(500, after.xp);
        assertEquals(Rarity.EPIC, after.rarity);
        assertEquals(3, after.affixes.size());
        for (AffixRoll r : after.affixes) assertTrue(r.roll >= Rarity.EPIC.floor);
    }

    @Test
    void reforgingAnUnrolledItemRollsItsRarityToo() {
        GrowthData after = Reforge.reroll(null, pool("a", "b", "c", "d", "e"), new Random(5));
        assertEquals(after.rarity.affixes, after.affixes.size());
    }
}
