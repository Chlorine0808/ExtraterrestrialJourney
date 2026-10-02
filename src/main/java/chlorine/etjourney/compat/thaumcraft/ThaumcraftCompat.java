package chlorine.etjourney.compat.thaumcraft;

import chlorine.etjourney.compat.CompatModule;

/** Thaumcraft biome integration; Thaumcraft classes may be touched only from here. */
public final class ThaumcraftCompat implements CompatModule {

    @Override
    public String modId() {
        return "Thaumcraft";
    }
}
