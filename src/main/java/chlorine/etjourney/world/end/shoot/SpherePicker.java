package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Collects items per {@link SphereClass} up to a wanted count each; extra offers are ignored. */
public final class SpherePicker<T> {

    private final Map<SphereClass, Integer> wanted = new EnumMap<>(SphereClass.class);
    private final Map<SphereClass, List<T>> picked = new EnumMap<>(SphereClass.class);

    public SpherePicker(Map<SphereClass, Integer> wanted) {
        for (SphereClass c : SphereClass.values()) {
            Integer n = wanted.get(c);
            this.wanted.put(c, n == null ? 0 : Math.max(0, n));
            picked.put(c, new ArrayList<T>());
        }
    }

    public boolean wants(SphereClass c) {
        return picked.get(c)
            .size() < wanted.get(c);
    }

    public void offer(T item, SphereClass c) {
        if (wants(c)) picked.get(c)
            .add(item);
    }

    public boolean full() {
        for (SphereClass c : SphereClass.values()) {
            if (wants(c)) return false;
        }
        return true;
    }

    public List<T> picked(SphereClass c) {
        return new ArrayList<>(picked.get(c));
    }

    public int shortfall(SphereClass c) {
        return wanted.get(c) - picked.get(c)
            .size();
    }
}
