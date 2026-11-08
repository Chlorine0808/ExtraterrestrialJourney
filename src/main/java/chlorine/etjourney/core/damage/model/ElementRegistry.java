package chlorine.etjourney.core.damage.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Every element by name, in registration order; starts with {@link #NEUTRAL}. */
public final class ElementRegistry {

    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, Element> BY_NAME = new LinkedHashMap<>();

    /** The part of an attack that has no element. */
    public static final Element NEUTRAL = register("neutral");

    private ElementRegistry() {}

    public static Element register(String name) {
        return add(name, false);
    }

    /** Registers an element reduced only by resistance to itself, which pierces death immunity. */
    public static Element registerAbsolute(String name) {
        return add(name, true);
    }

    private static synchronized Element add(String name, boolean absolute) {
        if (name == null || !VALID.matcher(name)
            .matches())
            throw new IllegalArgumentException("Element name must be snake_case [a-z0-9_]+: '" + name + "'");
        if (BY_NAME.containsKey(name)) throw new IllegalArgumentException("Element already registered: " + name);
        Element element = new Element(name, absolute);
        BY_NAME.put(name, element);
        return element;
    }

    /** The element of that name, or null. */
    public static synchronized Element get(String name) {
        return BY_NAME.get(name);
    }

    public static synchronized Collection<Element> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(BY_NAME.values()));
    }
}
