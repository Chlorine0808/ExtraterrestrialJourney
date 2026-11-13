package chlorine.etjourney.core.growth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** What a piece of gear has grown into: experience, rarity and affixes. */
public final class GrowthData {

    public final int xp;
    public final Rarity rarity;
    public final List<AffixRoll> affixes;

    public GrowthData(int xp, Rarity rarity, List<AffixRoll> affixes) {
        this.xp = xp;
        this.rarity = rarity;
        this.affixes = Collections.unmodifiableList(new ArrayList<>(affixes));
    }

    /** A fresh piece: a drawn rarity and as many affixes as it allows. */
    public static GrowthData roll(List<AffixDraw.Candidate> pool, Random random) {
        Rarity rarity = RarityTable.roll(random);
        return new GrowthData(0, rarity, AffixDraw.draw(pool, rarity.affixes, rarity.floor, random));
    }

    public int level() {
        return Leveling.level(xp);
    }

    /** Adds experience up to the maximum level; nothing below 1 adds. */
    public GrowthData withXp(int gained) {
        if (gained <= 0) return this;
        long sum = Math.min((long) xp + gained, Leveling.totalFor(Leveling.MAX_LEVEL));
        return new GrowthData((int) Math.max(xp, sum), rarity, affixes);
    }

    public GrowthData withAffixes(List<AffixRoll> next) {
        return new GrowthData(xp, rarity, next);
    }
}
