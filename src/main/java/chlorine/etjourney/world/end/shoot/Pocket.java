package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ranks standing spots inside a hollow by how far the air runs in 8 directions at eye height. */
public final class Pocket {

    public interface Grid {

        boolean air(int x, int y, int z);
    }

    static final int MIN_OPEN = 40, REACH = 24;

    private static final class Spot {

        Camera cam;
        double score;
    }

    private Pocket() {}

    public static List<Camera> rank(Grid g, int x0, int y0, int z0, int x1, int y1, int z1, double cx, double cz,
        int limit) {
        List<Spot> found = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0 + 1; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (!inside(x, y, z, x0, y0, z0, x1, y1, z1)) continue;
                    if (!standable(g, x, y, z, x0, y0, z0, x1, y1, z1)) continue;
                    int open = 0, bestLen = -1, bestDir = 0;
                    for (int k = 0; k < 8; k++) {
                        double a = Math.toRadians(k * 45.0);
                        double dx = -Math.sin(a), dz = Math.cos(a);
                        int len = 0;
                        while (len < REACH) {
                            int px = (int) Math.round(x + dx * (len + 1));
                            int pz = (int) Math.round(z + dz * (len + 1));
                            if (!inside(px, y + 1, pz, x0, y0, z0, x1, y1, z1) || !g.air(px, y + 1, pz)) break;
                            len++;
                        }
                        open += len;
                        if (len > bestLen) {
                            bestLen = len;
                            bestDir = k;
                        }
                    }
                    if (open < MIN_OPEN) continue;
                    Spot s = new Spot();
                    s.cam = new Camera(x + 0.5, y, z + 0.5, bestDir * 45f, 10f);
                    s.score = open - Math.hypot(x + 0.5 - cx, z + 0.5 - cz) / 2;
                    found.add(s);
                }
            }
        }
        found.sort(Comparator.comparingDouble((Spot s) -> -s.score));
        List<Camera> out = new ArrayList<>();
        for (int i = 0; i < found.size() && i < limit; i++) out.add(found.get(i).cam);
        return out;
    }

    /** Up to n of the ranked spots, best first, skipping any within gap blocks of one already taken. */
    public static List<Camera> spread(List<Camera> ranked, int n, double gap) {
        List<Camera> out = new ArrayList<>();
        for (Camera c : ranked) {
            if (out.size() >= n) break;
            boolean near = false;
            for (Camera o : out) near |= Math.sqrt(sq(c.x - o.x) + sq(c.y - o.y) + sq(c.z - o.z)) < gap;
            if (!near) out.add(c);
        }
        return out;
    }

    private static double sq(double v) {
        return v * v;
    }

    private static boolean inside(int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
    }

    private static boolean standable(Grid g, int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1) {
        return !g.air(x, y - 1, z) && g.air(x, y, z)
            && inside(x, y + 1, z, x0, y0, z0, x1, y1, z1)
            && g.air(x, y + 1, z);
    }
}
