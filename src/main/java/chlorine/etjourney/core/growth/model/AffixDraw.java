package chlorine.etjourney.core.growth.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Draws distinct affixes in proportion to their weights, each with a roll at or above a floor. */
public final class AffixDraw {

    /** An affix that may be drawn and its weight; 0 never draws. */
    public static final class Candidate {

        public final String id;
        public final int weight;

        public Candidate(String id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    private AffixDraw() {}

    /** Fewer than count when the pool runs out. */
    public static List<AffixRoll> draw(List<Candidate> pool, int count, float floor, Random random) {
        List<Candidate> left = new ArrayList<>();
        for (Candidate c : pool) if (c.weight > 0) left.add(c);
        List<AffixRoll> out = new ArrayList<>();
        while (out.size() < count && !left.isEmpty()) {
            int total = 0;
            for (Candidate c : left) total += c.weight;
            int at = random.nextInt(total);
            int i = 0;
            while (at >= left.get(i).weight) at -= left.get(i++).weight;
            Candidate picked = left.remove(i);
            out.add(new AffixRoll(picked.id, floor + (1 - floor) * random.nextFloat()));
        }
        return out;
    }
}
