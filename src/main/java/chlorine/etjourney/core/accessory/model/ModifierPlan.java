package chlorine.etjourney.core.accessory.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The attribute modifiers to take off and put on so the current ones match the wanted ones. */
public final class ModifierPlan {

    /** One modifier on one attribute, named by the attribute's unlocalized name. */
    public static final class Entry {

        public final UUID id;
        public final String attribute;
        public final double amount;
        public final int operation;

        public Entry(UUID id, String attribute, double amount, int operation) {
            this.id = id;
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry e = (Entry) o;
            return id.equals(e.id) && attribute.equals(e.attribute)
                && Double.compare(amount, e.amount) == 0
                && operation == e.operation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attribute, amount, operation);
        }

        @Override
        public String toString() {
            return attribute + " " + id + " " + amount + " op" + operation;
        }
    }

    public final List<Entry> removals;
    public final List<Entry> additions;

    private ModifierPlan(List<Entry> removals, List<Entry> additions) {
        this.removals = Collections.unmodifiableList(removals);
        this.additions = Collections.unmodifiableList(additions);
    }

    /** A changed modifier is removed and added again, since the game keeps one modifier per id. */
    public static ModifierPlan between(List<Entry> current, List<Entry> wanted) {
        List<Entry> removals = new ArrayList<>();
        for (Entry e : current) if (!wanted.contains(e)) removals.add(e);
        List<Entry> additions = new ArrayList<>();
        for (Entry e : wanted) if (!current.contains(e)) additions.add(e);
        return new ModifierPlan(removals, additions);
    }
}
