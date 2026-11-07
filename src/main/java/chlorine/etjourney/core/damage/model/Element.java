package chlorine.etjourney.core.damage.model;

/** A damage element such as fire or chaos; two elements are the same when their names are. */
public final class Element {

    private final String name;

    Element(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Element && ((Element) o).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
