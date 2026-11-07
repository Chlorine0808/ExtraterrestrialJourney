package chlorine.etjourney.world.wind;

import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.wind.model.Wind;

/** The client's copy of the winds in the dimension it is in, as last sent by the server. */
final class WindView {

    private static final class Snapshot {

        final int dimension;
        final List<Wind> winds;

        Snapshot(int dimension, List<Wind> winds) {
            this.dimension = dimension;
            this.winds = winds;
        }
    }

    private static volatile Snapshot current = new Snapshot(0, Collections.<Wind>emptyList());

    private WindView() {}

    static void set(int dimension, List<Wind> winds) {
        current = new Snapshot(dimension, Collections.unmodifiableList(winds));
    }

    /** The winds if the copy is of this dimension, otherwise none. */
    static List<Wind> in(int dimension) {
        Snapshot s = current;
        return s.dimension == dimension ? s.winds : Collections.<Wind>emptyList();
    }
}
