package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chlorine.etjourney.core.ModInfo;

/** What the hydrofluoric acid does to the item entities and creatures in it. Items go by their registry id. */
final class HydrofluoricAcid {

    enum Outcome {
        /** Each item turns into one of each product. */
        SPLIT,
        KEEP,
        DISSOLVE
    }

    static final String NAME = "hydrofluoric_acid";
    static final String RAW_TANTALUM = "raw_tantalum";
    static final String RAW_NIOBIUM = "raw_niobium";
    static final String EMPTY_BUCKET = "tantalum_bucket";
    static final String FULL_BUCKET = "tantalum_bucket_" + NAME;

    /** One heart, once a second. */
    static final float DAMAGE = 2.0F;
    private static final int HURT_INTERVAL = 20;

    private static final Set<String> COLTAN = ids("raw_coltan", "coltan_ore");
    private static final List<String> COLTAN_PRODUCTS = Collections
        .unmodifiableList(Arrays.asList(id(RAW_TANTALUM), id(RAW_NIOBIUM)));
    private static final Set<String> KEPT = ids(RAW_TANTALUM, RAW_NIOBIUM, EMPTY_BUCKET, FULL_BUCKET);

    private HydrofluoricAcid() {}

    static Outcome outcome(String item) {
        if (COLTAN.contains(item)) return Outcome.SPLIT;
        return KEPT.contains(item) ? Outcome.KEEP : Outcome.DISSOLVE;
    }

    /** The ids of what one of the item splits into; empty unless it splits. */
    static List<String> products(String item) {
        return COLTAN.contains(item) ? COLTAN_PRODUCTS : Collections.emptyList();
    }

    /** Every creature in the acid is hurt on the same ticks. */
    static boolean hurtsOn(long worldTime) {
        return worldTime % HURT_INTERVAL == 0;
    }

    private static String id(String name) {
        return ModInfo.MODID + ":" + name;
    }

    private static Set<String> ids(String... names) {
        Set<String> ids = new HashSet<>();
        for (String name : names) ids.add(id(name));
        return Collections.unmodifiableSet(ids);
    }
}
