package chlorine.etjourney.world.end.shoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import chlorine.etjourney.world.end.feature.EndBiome;

/** A parsed screenshot shot list: settings plus shot specs. Bad lines go to {@link #errors}; the rest still parses. */
public final class ShotList {

    public static final long DEFAULT_SEED = 7037748451318983361L;
    private static final List<View> GROUND_VIEWS = Arrays.asList(View.AERIAL, View.GROUND);
    private static final List<View> SPHERE_VIEWS = Arrays.asList(View.OUTSIDE, View.INSIDE);
    private static final int DEFAULT_MOUNTAIN_TOP = 150;

    public long seed = DEFAULT_SEED;
    public String worldType = "DEFAULT";
    public double originX = 3000;
    public double originZ = 0;
    public int renderDistance = 24;
    public int sphereCandidates = 120;
    public int budgetMinutes = 480;
    public int settleSeconds = 240;
    public boolean dryRun = false;
    public final List<ShotSpec> specs = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();

    private ShotList() {}

    public static ShotList parse(List<String> lines) {
        ShotList list = new ShotList();
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            int hash = text.indexOf('#');
            if (hash >= 0) text = text.substring(0, hash);
            text = text.trim();
            if (text.isEmpty()) continue;
            try {
                list.parseLine(text.split("\\s+"), i + 1);
            } catch (IllegalArgumentException e) {
                list.errors.add("line " + (i + 1) + ": " + e.getMessage());
            }
        }
        return list;
    }

    public static ShotList read(Path file) throws IOException {
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    /** The list path from the system property value, or null when it is unset or blank. */
    public static String listPath(String property) {
        if (property == null) return null;
        String trimmed = property.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void parseLine(String[] t, int line) {
        if (t[0].equals("shot")) {
            parseShot(t, line);
            return;
        }
        switch (t[0]) {
            case "seed":
                seed = Long.parseLong(single(t));
                break;
            case "world-type":
                worldType = single(t);
                break;
            case "origin":
                if (t.length != 3) throw new IllegalArgumentException("origin needs x and z");
                originX = Double.parseDouble(t[1]);
                originZ = Double.parseDouble(t[2]);
                break;
            case "render-distance":
                renderDistance = Integer.parseInt(single(t));
                break;
            case "sphere-candidates":
                sphereCandidates = Integer.parseInt(single(t));
                break;
            case "budget-minutes":
                budgetMinutes = Integer.parseInt(single(t));
                break;
            case "settle-seconds":
                settleSeconds = Integer.parseInt(single(t));
                break;
            case "dry-run":
                dryRun = Boolean.parseBoolean(single(t));
                break;
            default:
                throw new IllegalArgumentException("unknown setting '" + t[0] + "'");
        }
    }

    private static String single(String[] t) {
        if (t.length != 2) throw new IllegalArgumentException(t[0] + " needs exactly one value");
        return t[1];
    }

    private void parseShot(String[] t, int line) {
        if (t.length < 2) throw new IllegalArgumentException("shot needs a kind");
        ShotSpec.What what;
        switch (t[1]) {
            case "biome":
                what = ShotSpec.What.BIOME;
                break;
            case "bare":
                what = ShotSpec.What.BARE;
                break;
            case "biosphere":
                what = ShotSpec.What.BIOSPHERE;
                break;
            case "mountain":
                what = ShotSpec.What.MOUNTAIN;
                break;
            case "arc-touch":
                what = ShotSpec.What.ARC_TOUCH;
                break;
            case "at":
                what = ShotSpec.What.AT;
                break;
            default:
                throw new IllegalArgumentException("unknown shot kind '" + t[1] + "'");
        }
        int next = 2;
        String arg = null;
        double[] at = null;
        if (what == ShotSpec.What.BIOME || what == ShotSpec.What.BIOSPHERE) {
            if (t.length <= next || t[next].contains("=")) throw new IllegalArgumentException(t[1] + " needs a name");
            arg = t[next++];
            if (what == ShotSpec.What.BIOME) {
                try {
                    EndBiome.valueOf(arg);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unknown biome '" + arg + "'");
                }
            } else {
                try {
                    SphereClass.valueOf(arg);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unknown sphere class '" + arg + "'");
                }
            }
        } else if (what == ShotSpec.What.AT) {
            if (t.length < next + 3) throw new IllegalArgumentException("at needs x y z");
            at = new double[] { Double.parseDouble(t[next]), Double.parseDouble(t[next + 1]),
                Double.parseDouble(t[next + 2]), 0, 0 };
            next += 3;
        }

        int count = 1;
        int minTop = what == ShotSpec.What.MOUNTAIN ? DEFAULT_MOUNTAIN_TOP : 0;
        String label = null;
        List<View> views = allowedViews(what);
        for (; next < t.length; next++) {
            int eq = t[next].indexOf('=');
            if (eq <= 0) throw new IllegalArgumentException("expected key=value, got '" + t[next] + "'");
            String key = t[next].substring(0, eq);
            String value = t[next].substring(eq + 1);
            switch (key) {
                case "count":
                    count = Integer.parseInt(value);
                    if (count < 1) throw new IllegalArgumentException("count must be at least 1");
                    break;
                case "views":
                    views = parseViews(value, allowedViews(what));
                    break;
                case "min-top":
                    if (what != ShotSpec.What.MOUNTAIN) throw new IllegalArgumentException("min-top is for mountain");
                    minTop = Integer.parseInt(value);
                    break;
                case "yaw":
                    at = withAngle(at, 3, value);
                    break;
                case "pitch":
                    at = withAngle(at, 4, value);
                    break;
                case "label":
                    label = value;
                    break;
                default:
                    throw new IllegalArgumentException("unknown key '" + key + "'");
            }
        }
        specs.add(new ShotSpec(what, arg, count, views, minTop, at, label, line));
    }

    private static double[] withAngle(double[] at, int index, String value) {
        if (at == null) throw new IllegalArgumentException("yaw and pitch are for 'at' only");
        at[index] = Double.parseDouble(value);
        return at;
    }

    private static List<View> allowedViews(ShotSpec.What what) {
        switch (what) {
            case BIOSPHERE:
                return SPHERE_VIEWS;
            case AT:
                return Collections.emptyList();
            default:
                return GROUND_VIEWS;
        }
    }

    private static List<View> parseViews(String value, List<View> allowed) {
        if (allowed.isEmpty()) throw new IllegalArgumentException("this shot takes no views");
        List<View> out = new ArrayList<>();
        for (String name : value.split(",")) {
            View view;
            try {
                view = View.valueOf(
                    name.trim()
                        .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown view '" + name + "'");
            }
            if (!allowed.contains(view)) throw new IllegalArgumentException("view '" + name + "' is not allowed here");
            if (!out.contains(view)) out.add(view);
        }
        return out;
    }
}
