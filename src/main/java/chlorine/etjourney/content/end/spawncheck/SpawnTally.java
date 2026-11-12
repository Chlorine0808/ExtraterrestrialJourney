package chlorine.etjourney.content.end.spawncheck;

/** Verdicts of the spawn check. */
final class SpawnTally {

    /** Endermen with the new enemy listed must keep this part of their number without it. */
    static final double KEPT = 0.7;

    private SpawnTally() {}

    static boolean endermenKept(int with, int without) {
        return without > 0 && with >= KEPT * without;
    }

    /** The enemy's part of the enemy and the endermen counted together; 0 when none were counted. */
    static double share(int enemy, int endermen) {
        int all = enemy + endermen;
        return all == 0 ? 0 : (double) enemy / all;
    }

    /** Whether ranked out of total lies within four standard deviations (plus one) of the chance p. */
    static boolean rankShareFits(int ranked, int total, double p) {
        if (total == 0) return false;
        double mean = total * p, spread = 4 * Math.sqrt(total * p * (1 - p)) + 1;
        return Math.abs(ranked - mean) <= spread;
    }
}
