package chlorine.etjourney.core.growth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import chlorine.etjourney.core.growth.model.AffixDraw;
import chlorine.etjourney.core.growth.model.GearSlot;

/** The registered affixes by id; content registers them. */
public final class Affixes {

    private static final Map<String, Affix> BY_ID = new ConcurrentHashMap<>();
    private static final List<Affix> ORDER = new ArrayList<>();

    private Affixes() {}

    public static synchronized void register(Affix affix) {
        if (BY_ID.putIfAbsent(affix.id, affix) != null)
            throw new IllegalArgumentException("Affix already registered: " + affix.id);
        ORDER.add(affix);
    }

    /** Null for an id no longer registered; such affixes do nothing. */
    public static Affix get(String id) {
        return BY_ID.get(id);
    }

    public static synchronized List<AffixDraw.Candidate> pool(GearSlot slot) {
        List<AffixDraw.Candidate> out = new ArrayList<>();
        for (Affix a : ORDER) if (a.slots.contains(slot)) out.add(new AffixDraw.Candidate(a.id, a.weight));
        return out;
    }
}
