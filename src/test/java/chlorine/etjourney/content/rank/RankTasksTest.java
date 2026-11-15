package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Put-off work runs on its tick, once, and work it adds waits for a later tick. */
class RankTasksTest {

    @AfterEach
    void clear() {
        RankTasks.clear();
    }

    @Test
    void runsOnItsTickOnce() {
        List<String> ran = new ArrayList<>();
        RankTasks.later(2, () -> ran.add("a"));
        RankTasks.tick();
        assertEquals(0, ran.size());
        RankTasks.tick();
        assertEquals(Arrays.asList("a"), ran);
        RankTasks.tick();
        assertEquals(1, ran.size());
    }

    @Test
    void workAddedWhileRunningWaits() {
        List<String> ran = new ArrayList<>();
        RankTasks.later(1, () -> {
            ran.add("first");
            RankTasks.later(1, () -> ran.add("second"));
        });
        RankTasks.tick();
        assertEquals(Arrays.asList("first"), ran);
        RankTasks.tick();
        assertEquals(Arrays.asList("first", "second"), ran);
    }
}
