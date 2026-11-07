package chlorine.etjourney.core.util;

import java.util.ArrayList;
import java.util.List;

/** The game window's title, led by the checkout and shot list so parallel dev runs can be told apart. */
public final class WindowTitle {

    private WindowTitle() {}

    /** The tag, then the shot list's file name, then the base; blank parts are left out. */
    public static String compose(String base, String tag, String shotList) {
        List<String> parts = new ArrayList<>();
        if (tag != null && !tag.trim()
            .isEmpty()) parts.add(tag.trim());
        if (shotList != null && !shotList.trim()
            .isEmpty()) {
            String path = shotList.trim();
            parts.add("shoot " + path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1));
        }
        parts.add(base);
        return String.join(" | ", parts);
    }
}
