package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** A school's bounds hold its platforms tightly, and whether it forms follows its rule. */
class ShoalsTest {

    private static Shoals.School school(Random r) {
        double pitch = r.nextDouble() < 0.35 ? (r.nextBoolean() ? 1 : -1) * Math.toRadians(50 + 35 * r.nextDouble())
            : 0;
        return new Shoals.School(
            2000 + r.nextDouble() * 500,
            -1500 + r.nextDouble() * 500,
            30 + 170 * r.nextDouble(),
            r.nextDouble() * Math.PI * 2,
            pitch,
            50 + r.nextInt(70),
            r.nextDouble() < 0.3,
            r.nextLong());
    }

    @Test
    void boundsHoldThePlatformsTightly() {
        Random r = new Random(7);
        for (int n = 0; n < 300; n++) {
            Shoals.School s = school(r);
            int[] b = { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE };
            for (int[] p : Shoals.blocks(s)) {
                for (int i = 0; i < 3; i++) {
                    b[2 * i] = Math.min(b[2 * i], p[i]);
                    b[2 * i + 1] = Math.max(b[2 * i + 1], p[i]);
                }
            }
            assertArrayEquals(b, Shoals.bounds(s));
        }
    }

    /** The rule as it was first written, building every platform to find the lowest. */
    private static boolean formsByTheFullRule(Shoals.School school, List<Area> reserved) {
        int lowest = Integer.MAX_VALUE;
        for (int[] b : Shoals.blocks(school)) lowest = Math.min(lowest, b[1]);
        boolean high = school.y - 4 * 8 >= Reservations.CLEAR_Y && lowest >= Reservations.CLEAR_Y;
        if (!high && Reservations.insideFootprint(reserved, school.x, school.z, Shoals.EXTENT + 4 * 10)) return false;
        if (school.always) return true;
        for (Area area : reserved) {
            if (Distance.planar(school.x - area.x, school.z - area.z) < area.radius * 1.15 + Reservations.FADE + 30) {
                return true;
            }
        }
        return false;
    }

    @Test
    void formsFollowsTheFullRule() {
        Random r = new Random(8);
        for (int n = 0; n < 2000; n++) {
            Shoals.School s = school(r);
            List<Area> reserved = new ArrayList<>();
            if (r.nextBoolean()) {
                reserved.add(
                    new Area(
                        "test",
                        s.x + (r.nextDouble() - 0.5) * 600,
                        s.z + (r.nextDouble() - 0.5) * 600,
                        40 + 80 * r.nextDouble()));
            }
            assertEquals(formsByTheFullRule(s, reserved), Shoals.forms(s, reserved));
            assertEquals(formsByTheFullRule(s, Collections.emptyList()), Shoals.forms(s, Collections.emptyList()));
        }
    }
}
