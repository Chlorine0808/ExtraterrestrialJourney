package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders the run result as one self-contained HTML page; images are referenced by bare file name. */
public final class Report {

    private Report() {}

    public static String html(String title, List<String> errors, List<ShotRecord> records, String settings) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!DOCTYPE html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n<title>")
            .append(esc(title))
            .append("</title>\n<style>")
            .append("body{font-family:sans-serif;margin:1em 2em}")
            .append("figure{margin:1em 0;max-width:960px}")
            .append("img{max-width:100%;height:auto}")
            .append("figcaption{font-size:.9em}")
            .append("code{background:#eee;padding:0 .3em}")
            .append(".ng{color:#b00}")
            .append("</style>\n</head>\n<body>\n<h1>")
            .append(esc(title))
            .append("</h1>\n");
        if (settings != null && !settings.isEmpty()) {
            sb.append("<p>")
                .append(esc(settings))
                .append("</p>\n");
        }
        if (errors != null && !errors.isEmpty()) {
            sb.append("<h2>エラー</h2>\n<ul>\n");
            for (String e : errors) {
                sb.append("<li class=\"ng\">")
                    .append(esc(e))
                    .append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        String group = null;
        for (ShotRecord r : records) {
            if (!r.shot.group.equals(group)) {
                group = r.shot.group;
                sb.append("<h2>")
                    .append(esc(group))
                    .append("</h2>\n");
            }
            figure(sb, r);
        }
        return sb.append("</body>\n</html>\n")
            .toString();
    }

    private static void figure(StringBuilder sb, ShotRecord r) {
        PlannedShot s = r.shot;
        sb.append("<figure>\n");
        if (r.image != null && !r.image.isEmpty()) {
            sb.append("<img src=\"")
                .append(esc(r.image))
                .append("\" alt=\"")
                .append(esc(s.group + " " + s.index))
                .append("\">\n");
        }
        sb.append("<figcaption>\n");
        line(
            sb,
            "#" + s.index + " " + s.view + " : " + label(r.status),
            r.status == ShotRecord.Status.OK ? null : "ng");
        if (s.target != null) line(sb, "対象: " + s.target.label, null);
        if (s.source != null) line(sb, "出所: " + s.source, null);
        if (r.reason != null) line(sb, "理由: " + r.reason, "ng");
        if (s.missing != null) line(sb, "理由: " + s.missing, "ng");
        if (r.biome != null) line(sb, "バイオーム: " + r.biome, null);
        if (r.styles != null) line(sb, "スタイル: " + r.styles, null);
        if (r.attempts > 0) line(sb, "試行: " + r.attempts + " 回", null);
        if (r.width > 0) line(sb, "画像: " + r.width + "x" + r.height, null);
        if (r.camera != null) {
            sb.append("<div><code>")
                .append(esc(tpCommand(r.camera)))
                .append("</code> <code>")
                .append(esc(camCommand(r.camera)))
                .append("</code></div>\n");
        }
        sb.append("</figcaption>\n</figure>\n");
    }

    private static void line(StringBuilder sb, String text, String cls) {
        sb.append(cls == null ? "<div>" : "<div class=\"" + cls + "\">")
            .append(esc(text))
            .append("</div>\n");
    }

    private static String label(ShotRecord.Status st) {
        switch (st) {
            case OK:
                return "撮れた";
            case FAILED:
                return "撮れなかった";
            case MISSING:
                return "対象なし";
            case TIMED_OUT:
                return "時間切れ";
            default:
                return "未撮影";
        }
    }

    public static String tpCommand(Camera c) {
        return String.format(Locale.ROOT, "/tp Developer %.1f %.1f %.1f", c.x, c.y, c.z);
    }

    public static String camCommand(Camera c) {
        return String.format(Locale.ROOT, "/etj end cam %.1f %.1f %.1f %.1f %.1f", c.x, c.y, c.z, c.yaw, c.pitch);
    }

    /** Up to three styles with at least 1% weight, heaviest first, as "ARCS 60% / PLAINS 40%". */
    public static String styles(Map<String, Double> weights) {
        List<Map.Entry<String, Double>> es = new ArrayList<>();
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            if (e.getValue() >= 0.01) es.add(e);
        }
        es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, es.size()); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(
                es.get(i)
                    .getKey())
                .append(' ')
                .append(
                    Math.round(
                        es.get(i)
                            .getValue() * 100))
                .append('%');
        }
        return sb.toString();
    }

    public static String reason(Inspection.Fault f) {
        switch (f) {
            case SMALL:
                return "解像度不足";
            case UNIFORM:
                return "画面の大半が一色";
            case EMPTY_CENTRE:
                return "中央に何も写っていない";
            case DARK:
                return "暗すぎる";
            default:
                return "";
        }
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
