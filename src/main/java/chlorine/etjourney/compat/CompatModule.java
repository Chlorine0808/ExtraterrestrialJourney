package chlorine.etjourney.compat;

/** One optional integration; created and called only when its target mod is loaded. */
public interface CompatModule {

    default void preInit() {}

    default void init() {}

    default void postInit() {}
}
