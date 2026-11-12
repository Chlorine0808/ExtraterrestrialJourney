package chlorine.etjourney.content.rank;

import java.util.Collection;

/** Which mobs may roll a rank, by their EntityList name. */
public final class RankEligibility {

    private RankEligibility() {}

    /** An empty allow list takes every monster; the deny list and bosses always win. */
    public static boolean allows(String name, boolean monster, boolean boss, Collection<String> ranked,
        Collection<String> unranked) {
        if (name == null || boss || unranked.contains(name)) return false;
        return ranked.isEmpty() ? monster : ranked.contains(name);
    }
}
