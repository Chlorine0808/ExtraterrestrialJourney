package chlorine.etjourney.world.end.modifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** A value bound to some EndBlock kinds; unbound kinds have none. */
public final class KindTable<V> {

    private final Map<EndBlock, V> values = new EnumMap<>(EndBlock.class);

    public void bind(EndBlock kind, V value) {
        values.put(Objects.requireNonNull(kind, "kind"), Objects.requireNonNull(value, "value"));
    }

    /** The bound value, or null. */
    public V get(EndBlock kind) {
        return values.get(kind);
    }
}
