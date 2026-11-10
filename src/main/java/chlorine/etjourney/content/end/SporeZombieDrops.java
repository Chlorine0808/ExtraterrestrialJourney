package chlorine.etjourney.content.end;

import java.util.Random;

/** How many of each drop a spore zombie leaves. */
final class SporeZombieDrops {

    private SporeZombieDrops() {}

    /** Zero to max, with each looting level adding one to the most. */
    static int count(int max, int looting, Random random) {
        return random.nextInt(max + looting + 1);
    }
}
