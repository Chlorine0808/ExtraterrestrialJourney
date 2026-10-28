package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportTest {

    private static final Camera CAM = new Camera(3201.5, 140.0, -12.3, 90f, 35f);

    private static ShotRecord record(ShotRecord.Status s, String image, String reason, String missing) {
        Target t = missing == null ? new Target("grove", 3200, 100, -12, 40, 1L) : null;
        PlannedShot p = new PlannedShot("Groves", 1, View.AERIAL, t, null, "site <A>", missing);
        ShotRecord r = new ShotRecord(p);
        r.status = s;
        r.image = image;
        r.reason = reason;
        r.camera = CAM;
        r.biome = "End";
        r.styles = "ARCS 60%";
        r.attempts = 1;
        r.width = 1920;
        r.height = 1080;
        return r;
    }

    private static String html(ShotRecord r) {
        return Report.html("t", Collections.<String>emptyList(), Collections.singletonList(r), "");
    }

    @Test
    void okRecordShowsImageCaptionAndCommands() {
        String html = html(record(ShotRecord.Status.OK, "01-grove-aerial.png", null, null));
        assertTrue(html.contains("<meta charset=\"utf-8\">"));
        assertTrue(html.contains("<img src=\"01-grove-aerial.png\""));
        assertTrue(html.contains("/tp Developer 3201.5 140.0 -12.3"));
        assertTrue(html.contains("/etj cam 3201.5 140.0 -12.3 90.0 35.0"));
        assertTrue(html.contains("撮れた"));
        assertTrue(html.contains("<h2>Groves</h2>"));
        assertTrue(html.contains("site &lt;A&gt;"));
        assertFalse(html.contains(":\\") || html.contains("file:"));
    }

    @Test
    void failedRecordShowsReasonAndLastImage() {
        String html = html(record(ShotRecord.Status.FAILED, "02-x.png", "画面の大半が一色", null));
        assertTrue(html.contains("撮れなかった"));
        assertTrue(html.contains("画面の大半が一色"));
        assertTrue(html.contains("<img src=\"02-x.png\""));
    }

    @Test
    void missingTargetHasNoImageButAReason() {
        String html = html(record(ShotRecord.Status.MISSING, null, null, "候補 120 件中に見つからず"));
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("対象なし"));
        assertTrue(html.contains("候補 120 件中に見つからず"));
    }

    @Test
    void pendingAndTimedOutRecordsAreListed() {
        List<ShotRecord> rs = Arrays.asList(
            record(ShotRecord.Status.PENDING, null, null, null),
            record(ShotRecord.Status.TIMED_OUT, null, null, null));
        String html = Report.html("t", Collections.<String>emptyList(), rs, "");
        assertTrue(html.contains("未撮影"));
        assertTrue(html.contains("時間切れ"));
        assertFalse(html.contains("<img"));
    }

    @Test
    void textIsEscaped() {
        String html = Report
            .html("<b>&", Collections.singletonList("line 1: <x>"), Collections.<ShotRecord>emptyList(), "s<1>");
        assertTrue(html.contains("&lt;b&gt;&amp;"));
        assertTrue(html.contains("line 1: &lt;x&gt;"));
        assertTrue(html.contains("s&lt;1&gt;"));
    }

    @Test
    void stylesKeepTheThreeHeaviest() {
        Map<String, Double> w = new LinkedHashMap<>();
        w.put("PLAINS", 0.2);
        w.put("ARCS", 0.6);
        w.put("RIDGES", 0.15);
        w.put("SPIRES", 0.04);
        w.put("DUST", 0.005);
        assertEquals("ARCS 60% / PLAINS 20% / RIDGES 15%", Report.styles(w));
    }

    @Test
    void reasonsAreJapanese() {
        assertEquals("解像度不足", Report.reason(Inspection.Fault.SMALL));
        assertEquals("画面の大半が一色", Report.reason(Inspection.Fault.UNIFORM));
        assertEquals("中央に何も写っていない", Report.reason(Inspection.Fault.EMPTY_CENTRE));
        assertEquals("暗すぎる", Report.reason(Inspection.Fault.DARK));
    }
}
