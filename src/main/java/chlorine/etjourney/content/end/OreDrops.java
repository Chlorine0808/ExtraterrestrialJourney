package chlorine.etjourney.content.end;

import java.util.Random;

/** How many items an ore drops, with Fortune applied the way vanilla applies it to diamond ore. */
final class OreDrops {

    /** Far above any enchantment level, and low enough that the random bound cannot overflow. */
    private static final int MAX_FORTUNE = 255;

    private OreDrops() {}

    static int count(int min, int max, int fortune, Random random) {
        int low = Math.max(1, min), high = Math.max(low, max);
        int base = low + random.nextInt(high - low + 1);
        int level = Math.max(0, Math.min(MAX_FORTUNE, fortune));
        if (level == 0) return base;
        // BlockOre.quantityDroppedWithBonus: a bonus multiplier of 0 to level, with 0 drawn twice as often.
        return base * (Math.max(0, random.nextInt(level + 2) - 1) + 1);
    }
}
