package chlorine.etjourney.core.status.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StubbornMarksTest {

    @Test
    void markingAgainKeepsTheLaterEnd() {
        Map<Integer, Long> marks = new HashMap<>();
        StubbornMarks.mark(marks, 19, 500);
        StubbornMarks.mark(marks, 19, 300);
        assertEquals(500L, marks.get(19));
        StubbornMarks.mark(marks, 19, 800);
        assertEquals(800L, marks.get(19));
    }

    @Test
    void endedMarksAreDropped() {
        Map<Integer, Long> marks = new HashMap<>();
        StubbornMarks.mark(marks, 19, 500);
        StubbornMarks.mark(marks, 2, 1000);
        Map<Integer, Long> live = StubbornMarks.live(marks, 500);
        assertEquals(1, live.size());
        assertTrue(live.containsKey(2));
    }
}
