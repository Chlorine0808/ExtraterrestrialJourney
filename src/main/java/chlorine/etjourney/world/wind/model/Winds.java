package chlorine.etjourney.world.wind.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.IntConsumer;

/** The winds of every dimension, keyed by an id their source keeps to remove them later. */
public final class Winds {

    private final Map<Integer, SortedMap<Integer, Wind>> byDimension = new HashMap<>();
    private int nextId = 1;
    private IntConsumer listener = dimension -> {};

    /** Called with the dimension after each change there. */
    public void onChange(IntConsumer listener) {
        this.listener = listener;
    }

    public int add(int dimension, Wind wind) {
        int id = nextId++;
        byDimension.computeIfAbsent(dimension, d -> new TreeMap<>())
            .put(id, wind);
        listener.accept(dimension);
        return id;
    }

    public boolean remove(int dimension, int id) {
        SortedMap<Integer, Wind> here = byDimension.get(dimension);
        if (here == null || here.remove(id) == null) return false;
        listener.accept(dimension);
        return true;
    }

    public void clear(int dimension) {
        SortedMap<Integer, Wind> here = byDimension.remove(dimension);
        if (here != null && !here.isEmpty()) listener.accept(dimension);
    }

    /** Every dimension at once, without reporting; for a server that stops. */
    public void clearAll() {
        byDimension.clear();
    }

    /** The winds of the dimension by id, in the order they were added. */
    public SortedMap<Integer, Wind> in(int dimension) {
        SortedMap<Integer, Wind> here = byDimension.get(dimension);
        return here == null ? Collections.<Integer, Wind>emptySortedMap() : Collections.unmodifiableSortedMap(here);
    }
}
