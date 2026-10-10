package chlorine.etjourney.world.end.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.region.Style;
import chlorine.etjourney.world.end.region.StyleKind;

/** Tab completion and the style listing of /etj end, kept apart from Minecraft so they can be tested. */
final class EtjArgs {

    static final List<String> SUBCOMMANDS = Arrays.asList(
        "here",
        "style",
        "styles",
        "mix",
        "mountain",
        "lake",
        "hole",
        "valley",
        "zone",
        "heeisland",
        "destitute");
    /** Heights /etj look keeps the player between. */
    static final double MIN_Y = 0, MAX_Y = 270;
    /** Most styles mix accepts: a base and two overlays. */
    private static final int MIX = 3;

    private EtjArgs() {}

    /** Candidates for the last word of args. */
    static List<String> complete(String[] args, List<String> styles, List<String> zones) {
        String last = args[args.length - 1];
        if (args.length == 1) return matching(Arrays.asList("end", "look"), last);
        if (!args[0].equals("end")) return Collections.emptyList();
        if (args.length == 2) return matching(SUBCOMMANDS, last);
        switch (args[1]) {
            case "style":
                if (args.length == 3) return matching(styles, last);
                if (args.length == 4) return matching(Collections.singletonList("pure"), last);
                return Collections.emptyList();
            case "mix":
                return args.length <= 2 + MIX ? matching(styles, last) : Collections.<String>emptyList();
            case "zone":
                return args.length == 3 ? matching(zones, last) : Collections.<String>emptyList();
            default:
                return Collections.emptyList();
        }
    }

    private static List<String> matching(List<String> words, String prefix) {
        List<String> out = new ArrayList<>();
        for (String word : words) {
            if (word.regionMatches(true, 0, prefix, 0, prefix.length())) out.add(word);
        }
        return out;
    }

    /** One line per style: its name and where it may stand, as a base or over which bases. */
    static List<String> styleLines(List<Style> styles) {
        List<String> out = new ArrayList<>();
        for (Style style : styles) out.add(style.name + ": " + role(style, styles));
        return out;
    }

    private static String role(Style style, List<Style> styles) {
        boolean overLand = false, overVoid = false;
        for (Style base : styles) {
            if (!base.isBase() || !style.canOverlay(base)) continue;
            if (base.hasLand()) overLand = true;
            else overVoid = true;
        }
        if (style.kind == StyleKind.OVERLAY) return overVoid ? "overlay on anything" : "overlay on land";
        String base = style.hasLand() ? "base with land" : "base without land";
        return overLand || overVoid ? base + ", or overlay" : base;
    }

    /** Unit vector of a view at yaw and pitch in degrees, as Minecraft measures them (yaw 0 faces +Z). */
    static double[] lookDirection(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[] { -Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p) };
    }

    /** The point range blocks from eye along dir, kept between MIN_Y and MAX_Y. */
    static double[] lookTarget(double[] eye, double[] dir, int range) {
        double y = Math.max(MIN_Y, Math.min(MAX_Y, eye[1] + dir[1] * range));
        return new double[] { eye[0] + dir[0] * range, y, eye[2] + dir[2] * range };
    }

    /** What /etj end here says about a biosphere: its biome, the biome ID and the class that tells its mod. */
    static String biosphereLine(String biome, int id, String className) {
        return "Biosphere: " + biome + " (ID " + id + ", " + className + ")";
    }
}
