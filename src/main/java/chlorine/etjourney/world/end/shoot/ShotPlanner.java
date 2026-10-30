package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EndSearch;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.feature.EndBiome;

/** Expands a shot list into numbered planned shots, one per target and view. */
public final class ShotPlanner {

    /** Where a ball is cut from; null when it has no source and is left out. */
    public interface Sites {

        Site read(Biosphere b);
    }

    /** A resolved biosphere source: its kind, the point in the sample world, and whether that point is water. */
    public static final class Site {

        public final BiosphereSource.Kind kind;
        public final int x, z;
        public final boolean water;

        public Site(BiosphereSource.Kind kind, int x, int z, boolean water) {
            this.kind = kind;
            this.x = x;
            this.z = z;
            this.water = water;
        }

        SphereClass sphereClass() {
            return SphereClass.of(kind, kind == BiosphereSource.Kind.SURFACE && water);
        }
    }

    private static final String NOT_FOUND = "半径 " + EndSearch.RADIUS + " ブロック内に見つからず";

    private ShotPlanner() {}

    public static List<PlannedShot> expand(ShotList list, TerrainSampler sampler, Sites sites) {
        Locator locator = new Locator(sampler);
        SphereSearch spheres = searchSpheres(list, locator, sites);
        List<PlannedShot> out = new ArrayList<>();
        double ox = list.originX, oz = list.originZ;
        for (ShotSpec spec : list.specs) {
            switch (spec.what) {
                case BIOME:
                    EndBiome biome = EndBiome.valueOf(spec.arg);
                    add(out, spec, biome.name() + " 大陸", locator.continents(biome, spec.style, ox, oz, spec.count));
                    break;
                case BARE:
                    add(out, spec, "bare 大陸", locator.continents(null, spec.style, ox, oz, spec.count));
                    break;
                case MOUNTAIN:
                    add(out, spec, "山", locator.mountains(spec.minTop, ox, oz, spec.count));
                    break;
                case GIANT_TREE:
                    add(out, spec, "巨木", locator.giantTrees(ox, oz, spec.count));
                    break;
                case ARC_TOUCH:
                    add(out, spec, "ARCS 接地", locator.arcTouches(ox, oz, spec.count));
                    break;
                case BIOSPHERE:
                    addSpheres(out, spec, spheres);
                    break;
                case AT:
                    double[] a = spec.at;
                    String group = spec.label != null ? spec.label : "指定位置";
                    Target t = new Target(group, a[0], a[1], a[2], 0, 0);
                    Camera fixed = new Camera(a[0], a[1], a[2], (float) a[3], (float) a[4]);
                    out.add(new PlannedShot(group, out.size() + 1, View.AERIAL, t, fixed, null, null));
                    break;
            }
        }
        return out;
    }

    /** "SURFACE from x,z", with "（水）" when the point is water. */
    public static String source(Site s) {
        return s.kind + " from " + s.x + "," + s.z + (s.water ? "（水）" : "");
    }

    /** The camera of the first attempt, or null without a target; INSIDE stands at the ball's centre for now. */
    public static Camera firstCamera(PlannedShot s, Framing.BiomeAt biomes) {
        if (s.fixed != null) return s.fixed;
        Target t = s.target;
        if (t == null) return null;
        switch (s.view) {
            case OUTSIDE:
                return Framing.outside(t, 0);
            case INSIDE:
                return new Camera(t.x, t.y - Framing.EYE, t.z, 0, 0);
            case GROUND:
                float yaw = Framing.groundYaws(biomes, t.x, t.z, biomes.at(t.x, t.z))
                    .get(0);
                return Framing.ground(t.x, t.y, t.z, yaw);
            default:
                return Framing.aerial(t, 0);
        }
    }

    private static void add(List<PlannedShot> out, ShotSpec spec, String prefix, List<Target> found) {
        for (int n = 0; n < spec.count; n++) {
            Target t = n < found.size() ? found.get(n) : null;
            for (View v : spec.views) {
                out.add(
                    new PlannedShot(
                        prefix + " " + (n + 1),
                        out.size() + 1,
                        v,
                        t,
                        null,
                        null,
                        t == null ? NOT_FOUND : null));
            }
        }
    }

    private static void addSpheres(List<PlannedShot> out, ShotSpec spec, SphereSearch search) {
        SphereClass c = SphereClass.valueOf(spec.arg);
        List<Picked> mine = search.take(c, spec.count);
        String missing = "候補 " + search.candidates + " 件中に見つからず";
        for (int n = 0; n < spec.count; n++) {
            Picked p = n < mine.size() ? mine.get(n) : null;
            for (View v : spec.views) {
                out.add(
                    new PlannedShot(
                        sphereName(c) + " " + (n + 1),
                        out.size() + 1,
                        v,
                        p == null ? null : Locator.ofBall(p.ball),
                        null,
                        p == null ? null : source(p.site),
                        p == null ? missing : null));
            }
        }
    }

    private static String sphereName(SphereClass c) {
        switch (c) {
            case WATER:
                return "水の球";
            case CAVE:
                return "洞窟の球";
            case NETHER:
                return "ネザーの球";
            default:
                return "地表の球";
        }
    }

    /** Reads candidate balls nearest first until every biosphere line has its count. */
    private static SphereSearch searchSpheres(ShotList list, Locator locator, Sites sites) {
        Map<SphereClass, Integer> wanted = new EnumMap<>(SphereClass.class);
        for (ShotSpec spec : list.specs) {
            if (spec.what == ShotSpec.What.BIOSPHERE)
                wanted.merge(SphereClass.valueOf(spec.arg), spec.count, Integer::sum);
        }
        SpherePicker<Picked> picker = new SpherePicker<>(wanted);
        if (wanted.isEmpty()) return new SphereSearch(picker, 0);
        List<Biosphere> candidates = locator.biosphereCandidates(list.originX, list.originZ, list.sphereCandidates);
        for (Biosphere b : candidates) {
            if (picker.full()) break;
            Site site = sites.read(b);
            if (site == null) continue;
            SphereClass c = site.sphereClass();
            picker.offer(new Picked(b, site), c);
        }
        return new SphereSearch(picker, candidates.size());
    }

    private static final class Picked {

        final Biosphere ball;
        final Site site;

        Picked(Biosphere ball, Site site) {
            this.ball = ball;
            this.site = site;
        }
    }

    /** Picked balls per class, handed out to the biosphere lines in list order. */
    private static final class SphereSearch {

        final int candidates;
        private final Map<SphereClass, List<Picked>> left = new EnumMap<>(SphereClass.class);

        SphereSearch(SpherePicker<Picked> picker, int candidates) {
            this.candidates = candidates;
            for (SphereClass c : SphereClass.values()) left.put(c, picker.picked(c));
        }

        List<Picked> take(SphereClass c, int count) {
            List<Picked> all = left.get(c);
            if (all.isEmpty()) return Collections.emptyList();
            List<Picked> head = new ArrayList<>(all.subList(0, Math.min(count, all.size())));
            all.subList(0, head.size())
                .clear();
            return head;
        }
    }
}
