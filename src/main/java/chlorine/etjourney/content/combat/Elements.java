package chlorine.etjourney.content.combat;

import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;

/** The damage elements of ETJ; neutral comes from the damage core. */
public final class Elements {

    public static final Element FIRE = ElementRegistry.register("fire");
    public static final Element ICE = ElementRegistry.register("ice");
    public static final Element WIND = ElementRegistry.register("wind");
    public static final Element ELECTRIC = ElementRegistry.register("electric");
    public static final Element CHAOS = ElementRegistry.register("chaos");
    public static final Element ORDER = ElementRegistry.register("order");
    public static final Element LIGHT = ElementRegistry.register("light");

    private Elements() {}

    /** Loads the class, which registers every element above. */
    public static void register() {}
}
