package chlorine.etjourney.core.registry;

import java.util.regex.Pattern;

import chlorine.etjourney.core.ModInfo;

/** Builds texture and translation names from one snake_case registry name. */
public final class RegistryNames {

    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+");

    private RegistryNames() {}

    public static String validate(String name) {
        if (name == null || !VALID.matcher(name)
            .matches()) {
            throw new IllegalArgumentException("Registry name must be snake_case [a-z0-9_]+: '" + name + "'");
        }
        return name;
    }

    public static String texture(String name) {
        return ModInfo.MODID + ":" + validate(name);
    }

    public static String unlocalized(String name) {
        return ModInfo.MODID + "." + validate(name);
    }
}
