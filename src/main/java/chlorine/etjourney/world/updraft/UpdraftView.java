package chlorine.etjourney.world.updraft;

import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.updraft.model.Updraft;

/** The client's copy of the updrafts in the dimension it is in, as last sent by the server. */
final class UpdraftView {

    private static final class Snapshot {

        final int dimension;
        final List<Updraft> updrafts;

        Snapshot(int dimension, List<Updraft> updrafts) {
            this.dimension = dimension;
            this.updrafts = updrafts;
        }
    }

    private static volatile Snapshot current = new Snapshot(0, Collections.<Updraft>emptyList());

    private UpdraftView() {}

    static void set(int dimension, List<Updraft> updrafts) {
        current = new Snapshot(dimension, Collections.unmodifiableList(updrafts));
    }

    /** The updrafts if the copy is of this dimension, otherwise none. */
    static List<Updraft> in(int dimension) {
        Snapshot s = current;
        return s.dimension == dimension ? s.updrafts : Collections.<Updraft>emptyList();
    }
}
