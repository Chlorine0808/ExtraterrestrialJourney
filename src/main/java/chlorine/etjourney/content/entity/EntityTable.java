package chlorine.etjourney.content.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chlorine.etjourney.core.registry.RegistryNames;

/** Mod entities in registration order, each with its network ID and optional spawn rule. */
public final class EntityTable {

    public static final class Entry {

        public final String name;
        public final int id;
        public final Class<?> type;
        /** Null for an entity that never spawns naturally. */
        public final SpawnRule rule;

        Entry(String name, int id, Class<?> type, SpawnRule rule) {
            this.name = name;
            this.id = id;
            this.type = type;
            this.rule = rule;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<Class<?>, Entry> byType = new HashMap<>();

    public Entry add(String name, Class<?> type, SpawnRule rule) {
        RegistryNames.validate(name);
        for (Entry e : entries) {
            if (e.name.equals(name)) throw new IllegalArgumentException("Entity name already registered: " + name);
        }
        if (byType.containsKey(type)) throw new IllegalArgumentException("Entity class already registered: " + type);
        Entry entry = new Entry(name, entries.size(), type, rule);
        entries.add(entry);
        byType.put(type, entry);
        return entry;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public Entry byType(Class<?> type) {
        return byType.get(type);
    }

    public List<String> withoutRenderer(Set<Class<?>> rendered) {
        List<String> out = new ArrayList<>();
        for (Entry e : entries) {
            if (!rendered.contains(e.type)) out.add(e.name);
        }
        return out;
    }
}
