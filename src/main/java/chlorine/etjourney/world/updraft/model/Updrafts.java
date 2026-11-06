package chlorine.etjourney.world.updraft.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.IntConsumer;

/** The updrafts of every dimension, keyed by an id their source keeps to remove them later. */
public final class Updrafts {

    private final Map<Integer, SortedMap<Integer, Updraft>> byDimension = new HashMap<>();
    private int nextId = 1;
    private IntConsumer listener = dimension -> {};

    /** Called with the dimension after each change there. */
    public void onChange(IntConsumer listener) {
        this.listener = listener;
    }

    public int add(int dimension, Updraft updraft) {
        int id = nextId++;
        byDimension.computeIfAbsent(dimension, d -> new TreeMap<>())
            .put(id, updraft);
        listener.accept(dimension);
        return id;
    }

    public boolean remove(int dimension, int id) {
        SortedMap<Integer, Updraft> here = byDimension.get(dimension);
        if (here == null || here.remove(id) == null) return false;
        listener.accept(dimension);
        return true;
    }

    public void clear(int dimension) {
        SortedMap<Integer, Updraft> here = byDimension.remove(dimension);
        if (here != null && !here.isEmpty()) listener.accept(dimension);
    }

    /** Every dimension at once, without reporting; for a server that stops. */
    public void clearAll() {
        byDimension.clear();
    }

    /** The updrafts of the dimension by id, in the order they were added. */
    public SortedMap<Integer, Updraft> in(int dimension) {
        SortedMap<Integer, Updraft> here = byDimension.get(dimension);
        return here == null ? Collections.<Integer, Updraft>emptySortedMap() : Collections.unmodifiableSortedMap(here);
    }
}
