package chlorine.etjourney.core.damage.model;

/**
 * A damage element such as fire or chaos; two elements are the same when their names are. An absolute element is
 * reduced only by resistance to itself and pierces death immunity.
 */
public final class Element {

    private final String name;
    private final boolean absolute;

    Element(String name, boolean absolute) {
        this.name = name;
        this.absolute = absolute;
    }

    public String name() {
        return name;
    }

    public boolean isAbsolute() {
        return absolute;
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
