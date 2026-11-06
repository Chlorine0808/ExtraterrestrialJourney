package chlorine.etjourney.content.craft;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One placed item as the recipes see it: its registry name, metadata and ore dictionary names. */
public final class ItemKey {

    final String id;
    final int meta;
    final List<String> oreNames;

    public ItemKey(String id, int meta, String... oreNames) {
        this(id, meta, Arrays.asList(oreNames));
    }

    public ItemKey(String id, int meta, List<String> oreNames) {
        this.id = id;
        this.meta = meta;
        this.oreNames = Collections.unmodifiableList(oreNames);
    }
}
