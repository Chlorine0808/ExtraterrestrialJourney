package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Biomes kept out of biospheres, by ID, name or owning mod, as the config lists them. */
public final class BiomeBlacklist {

    private final Set<Integer> ids = new LinkedHashSet<>();
    private final Set<String> names = new LinkedHashSet<>(), mods = new LinkedHashSet<>();
    /** Biome entries in config order, so unmatched ones are reported as written. */
    private final List<String> biomeEntries = new ArrayList<>();

    public BiomeBlacklist(String[] biomes, String[] modIds) {
        for (String entry : biomes) {
            String e = entry.trim();
            if (e.isEmpty()) continue;
            biomeEntries.add(e);
            if (isId(e)) ids.add(Integer.valueOf(e));
            else names.add(e.toLowerCase(Locale.ROOT));
        }
        for (String entry : modIds) {
            String e = entry.trim();
            if (!e.isEmpty()) mods.add(e.toLowerCase(Locale.ROOT));
        }
    }

    /** Digits short enough to be an int; longer runs are taken as a name, which then matches nothing. */
    private static boolean isId(String e) {
        return e.matches("\\d{1,9}");
    }

    public boolean isEmpty() {
        return ids.isEmpty() && names.isEmpty() && mods.isEmpty();
    }

    /** Whether the biome is kept out; name and mod may be null when unknown. */
    public boolean excludes(int id, String name, String modId) {
        return ids.contains(id) || name != null && names.contains(name.toLowerCase(Locale.ROOT))
            || modId != null && mods.contains(modId.toLowerCase(Locale.ROOT));
    }

    /** The entries that match no registered biome or loaded mod, as "biome X" or "mod X". */
    public List<String> unmatched(Collection<Integer> biomeIds, Collection<String> biomeNames,
        Collection<String> loadedMods) {
        Set<String> knownNames = new HashSet<>(), knownMods = new HashSet<>();
        for (String n : biomeNames) if (n != null) knownNames.add(n.toLowerCase(Locale.ROOT));
        for (String m : loadedMods) knownMods.add(m.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>();
        for (String e : biomeEntries) {
            boolean known = isId(e) ? biomeIds.contains(Integer.valueOf(e))
                : knownNames.contains(e.toLowerCase(Locale.ROOT));
            if (!known) out.add("biome " + e);
        }
        for (String m : mods) if (!knownMods.contains(m)) out.add("mod " + m);
        return out;
    }
}
