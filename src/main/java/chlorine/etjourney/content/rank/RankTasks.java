package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Work an ability puts off for a few ticks, as a warned strike; lost when the server stops. */
public final class RankTasks {

    private static final class Task {

        int ticksLeft;
        final Runnable work;

        Task(int ticksLeft, Runnable work) {
            this.ticksLeft = ticksLeft;
            this.work = work;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private RankTasks() {}

    /** Runs the work at the end of the server tick that many ticks from now. */
    public static void later(int ticks, Runnable work) {
        TASKS.add(new Task(ticks, work));
    }

    /** Once per server tick; work added while running waits for the next tick. */
    static void tick() {
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext();) {
            Task t = it.next();
            if (--t.ticksLeft <= 0) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) t.work.run();
    }

    /** Drops work left from a server that stopped. */
    public static void clear() {
        TASKS.clear();
    }
}
