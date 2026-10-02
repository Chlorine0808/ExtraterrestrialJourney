package chlorine.etjourney.compat;

/** One optional integration; its stage methods run only when modId() is loaded. */
public interface CompatModule {

    String modId();

    default void preInit() {}

    default void init() {}

    default void postInit() {}
}
