package chlorine.etjourney.core.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.potion.Potion;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** Looks potions up by name and tells harmful ones apart on either side. */
public final class Potions {

    private Potions() {}

    /** Potion.isBadEffect() is client-only, so the field is read instead. */
    public static boolean harmful(Potion potion) {
        if (potion instanceof StatusPotion) return ((StatusPotion) potion).harmful;
        Boolean bad = ReflectionHelper.getPrivateValue(Potion.class, potion, "isBadEffect", "field_76418_K");
        return bad;
    }

    /** By full name ("potion.poison") or its last part ("poison"), ignoring case; null if none. */
    public static Potion byName(String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        for (Potion potion : Potion.potionTypes) {
            if (potion == null) continue;
            String full = potion.getName()
                .toLowerCase(Locale.ROOT);
            if (full.equals(wanted) || shortName(full).equals(wanted)) return potion;
        }
        return null;
    }

    /** The last part of every registered potion's name, for tab completion. */
    public static List<String> shortNames() {
        List<String> out = new ArrayList<>();
        for (Potion potion : Potion.potionTypes) if (potion != null) out.add(shortName(potion.getName()));
        return out;
    }

    private static String shortName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
