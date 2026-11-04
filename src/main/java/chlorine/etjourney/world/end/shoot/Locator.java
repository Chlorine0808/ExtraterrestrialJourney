package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chlorine.etjourney.world.end.ChunkPlan;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EndSearch;
import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.plant.GiantTrees;
import chlorine.etjourney.world.end.feature.plant.Glowshrooms;
import chlorine.etjourney.world.end.reserve.Area;

/** Finds screenshot targets from the seed alone, nearest first; a search that comes up short returns fewer. */
final class Locator {

    private static final double LAND = 10;
    /** Scene sizes; an arc touch is framed with the stretch of tube that comes down to the ground. */
    private static final double CONTINENT_SIZE = 96, MOUNTAIN_SIZE = 64, ARC_SCENE_SIZE = 64;
    /** A lake is framed with its shore, so the scene is this many times its radius. */
    private static final double LAKE_SCENE_RADII = 3;

    /** Style weight at a continent point from which the style counts as dominant there. */
    private static final double STYLE_DOMINANT = 0.6;
    /** A giant tree is 56 high with a 26-block reach, so this scene size puts the camera 200 blocks off. */
    private static final double TREE_SIZE = 80;

    /** Style weight below which the surface paint leaves a MYCELIA column to the glowshrooms. */
    private static final double LAYERED_LIMIT = 0.05;
    /** Normal-cell rings searched around a continent seed. */
    private static final int NORMAL_RINGS = 6;

    private final TerrainSampler sampler;
    /** Reserved areas by chunk, as generation sees them. */
    private final ChunkPlan.ReservedLookup reserved;

    Locator(TerrainSampler sampler, ChunkPlan.ReservedLookup reserved) {
        this.sampler = sampler;
        this.reserved = reserved;
    }

    List<Target> continents(EndBiome biome, double ox, double oz, int count) {
        return continents(biome, null, ox, oz, count);
    }

    /**
     * Continents wearing the biome (null for bare), each from a different continent, nearest first. With a style,
     * only those whose chosen point is at least STYLE_DOMINANT that style.
     */
    List<Target> continents(EndBiome biome, String style, double ox, double oz, int count) {
        long seed = sampler.seed();
        Set<Long> chosen = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] hit = EndSearch.nearestInCells(Continent.SEED_CELL, ox, oz, (cx, cz) -> {
                Continent.Seed s = Continent.seedAt(seed, cx, cz);
                // Cheap checks first; the continent id is a flood fill.
                if (s == null || EndBiome.ofContinentOf(seed, s) != biome || sampler.land(s.x, s.z) <= LAND)
                    return null;
                if (sampler.biome(s.x, s.z) != biome) return null;
                if (style != null && sampler.styleWeight(style, s.x, s.z) < STYLE_DOMINANT) return null;
                long id = ContinentIds.of(seed, s);
                return chosen.contains(id) ? null : new double[] { s.x, s.z, Double.longBitsToDouble(id) };
            });
            if (hit == null) break;
            long id = Double.doubleToRawLongBits(hit[2]);
            chosen.add(id);
            out.add(
                new Target(
                    biome == null ? "bare" : biome.name(),
                    hit[0],
                    sampler.bareColumn(hit[0], hit[1], true).top,
                    hit[1],
                    CONTINENT_SIZE,
                    id));
        }
        return out;
    }

    /** The giant tree nearest the origin of each grove continent, continents nearest first. */
    List<Target> giantTrees(double ox, double oz, int count) {
        long seed = sampler.seed();
        Set<Long> chosen = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] hit = EndSearch.nearestInCells(Continent.SEED_CELL, ox, oz, (cx, cz) -> {
                Continent.Seed s = Continent.seedAt(seed, cx, cz);
                // Only groves grow trees; this is cheap, the continent id is a flood fill.
                if (s == null || EndBiome.ofContinentOf(seed, s) != EndBiome.GROVE) return null;
                long id = ContinentIds.of(seed, s);
                if (chosen.contains(id)) return null;
                double[] best = null;
                for (double[] a : GiantTrees.anchors(seed, s, sampler.standingProbe()))
                    if (best == null || Math.hypot(a[0] - ox, a[2] - oz) < Math.hypot(best[0] - ox, best[2] - oz))
                        best = a;
                return best == null ? null : new double[] { best[0], best[1], best[2], Double.longBitsToDouble(id) };
            });
            if (hit == null) break;
            long id = Double.doubleToRawLongBits(hit[3]);
            chosen.add(id);
            // y is the ground at the root: the ground view looks for a floor near it, the aerial one centres on it.
            out.add(new Target("巨木", hit[0], hit[1], hit[2], TREE_SIZE, id));
        }
        return out;
    }

    /**
     * The glowshroom nearest the seed of each MYCELIA continent, continents nearest first. Cells around the seed
     * are searched, so the shroom may stand a little off the continent's centre.
     */
    List<Target> glowshrooms(boolean giant, double ox, double oz, int count) {
        long seed = sampler.seed();
        Glowshrooms.Terrain terrain = glowshroomTerrain();
        Set<Long> chosen = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] hit = EndSearch.nearestInCells(Continent.SEED_CELL, ox, oz, (cx, cz) -> {
                Continent.Seed s = Continent.seedAt(seed, cx, cz);
                if (s == null || EndBiome.ofContinentOf(seed, s) != EndBiome.MYCELIA) return null;
                long id = ContinentIds.of(seed, s);
                if (chosen.contains(id)) return null;
                Glowshrooms.Anchor best = nearestShroom(giant, seed, s, terrain);
                if (best == null) return null;
                double size = (best.shape.maxX() - best.shape.minX()) * 2;
                return new double[] { best.x, best.z, best.y, size, Double.longBitsToDouble(id) };
            });
            if (hit == null) break;
            long id = Double.doubleToRawLongBits(hit[4]);
            chosen.add(id);
            out.add(new Target(giant ? "Giant キノコ" : "通常のキノコ", hit[0], hit[2], hit[1], hit[3], id));
        }
        return out;
    }

    private static Glowshrooms.Anchor nearestShroom(boolean giant, long seed, Continent.Seed s,
        Glowshrooms.Terrain terrain) {
        int cell = giant ? Glowshrooms.GIANT_CELL : Glowshrooms.NORMAL_CELL;
        int rings = giant ? 1 : NORMAL_RINGS;
        int scx = (int) Math.floor((double) s.x / cell), scz = (int) Math.floor((double) s.z / cell);
        Glowshrooms.Anchor best = null;
        for (int dx = -rings; dx <= rings; dx++) {
            for (int dz = -rings; dz <= rings; dz++) {
                List<Glowshrooms.Anchor> found;
                if (giant) {
                    Glowshrooms.Anchor a = Glowshrooms.giantIn(seed, scx + dx, scz + dz, terrain);
                    found = a == null ? new ArrayList<>() : Collections.singletonList(a);
                } else {
                    found = Glowshrooms.normalIn(seed, scx + dx, scz + dz, terrain);
                }
                for (Glowshrooms.Anchor a : found)
                    if (best == null || Math.hypot(a.x - s.x, a.z - s.z) < Math.hypot(best.x - s.x, best.z - s.z))
                        best = a;
            }
        }
        return best;
    }

    /** The terrain the glowshroom placement sees, built from the sampler as the production modifier builds it. */
    private Glowshrooms.Terrain glowshroomTerrain() {
        StructureProbe probe = sampler.standingProbe();
        return new Glowshrooms.Terrain() {

            @Override
            public double ground(double x, double z) {
                return probe.ground(x, z);
            }

            @Override
            public boolean mycelia(double x, double z) {
                int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
                return sampler.biome(bx, bz) == EndBiome.MYCELIA
                    && sampler.styleWeight("LAYERED", bx, bz) < LAYERED_LIMIT;
            }

            @Override
            public boolean wet(double x, double z, double range) {
                return probe.wet(x, z, range);
            }
        };
    }

    /** Mountains whose top reaches minTop, nearest first. */
    List<Target> mountains(int minTop, double ox, double oz, int count) {
        long seed = sampler.seed();
        Set<Long> chosen = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] hit = EndSearch.nearestInCells(Mountains.CELL, ox, oz, (cx, cz) -> {
                Mountains.Mountain m = Mountains.inCell(seed, cx, cz);
                if (m == null || chosen.contains(cellKey(cx, cz))) return null;
                double top = sampler.bareColumn(m.x, m.z, true).top;
                return top < minTop ? null : new double[] { m.x, m.z, top, cx, cz };
            });
            if (hit == null) break;
            long key = cellKey((int) hit[3], (int) hit[4]);
            chosen.add(key);
            out.add(new Target("mountain", hit[0], hit[2], hit[1], MOUNTAIN_SIZE, key));
        }
        return out;
    }

    /** Lakes nearest first, the target on the liquid surface at the centre. */
    List<Target> lakes(double ox, double oz, int count) {
        long seed = sampler.seed();
        Lakes.Probe probe = sampler.lakeProbe();
        Set<Long> chosen = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double[] hit = EndSearch.nearestInCells(Lakes.CELL, ox, oz, (cx, cz) -> {
                if (chosen.contains(cellKey(cx, cz))) return null;
                Lakes.Lake l = Lakes.inCell(seed, cx, cz, probe);
                if (l == null || !generated(l)) return null;
                return new double[] { l.x, l.z, l.waterLevel, l.radius, cx, cz };
            });
            if (hit == null) break;
            long key = cellKey((int) hit[4], (int) hit[5]);
            chosen.add(key);
            out.add(new Target("lake", hit[0], hit[2], hit[1], hit[3] * LAKE_SCENE_RADII, key));
        }
        return out;
    }

    /** Whether generation keeps the lake; ChunkPlan drops lakes that touch a reserved area. */
    private boolean generated(Lakes.Lake lake) {
        List<Area> areas = reserved.at((int) Math.floor(lake.x) >> 4, (int) Math.floor(lake.z) >> 4);
        return !Lakes.withoutReserved(Collections.singletonList(lake), areas)
            .isEmpty();
    }

    /** Points where an ARCS tube meets the ground, one per path, looking outward ring by ring. */
    List<Target> arcTouches(double ox, double oz, int count) {
        long seed = sampler.seed();
        List<Target> out = new ArrayList<>();
        int ocx = (int) Math.floor(ox / ArcPaths.CELL), ocz = (int) Math.floor(oz / ArcPaths.CELL);
        for (int ring = 0; ring <= EndSearch.RADIUS / ArcPaths.CELL + 1 && out.size() < count; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.abs(i) != ring && Math.abs(j) != ring) continue;
                    int cx = ocx + i, cz = ocz + j;
                    List<ArcPaths.Path> paths = ArcPaths.pathsInCell(seed, cx, cz, sampler.arcProbe());
                    for (int k = 0; k < paths.size() && out.size() < count; k++) {
                        ArcPaths.Path path = paths.get(k);
                        double[] p = ArcPaths.touchPoint(path, sampler.arcLand(), sampler.arcProbe());
                        if (p != null)
                            out.add(new Target("arc", p[0], p[1], p[2], ARC_SCENE_SIZE, cellKey(cx, cz) + k));
                    }
                }
            }
        }
        return out;
    }

    /** Biospheres found ring by ring (Chebyshev distance in cells) around the origin, at most cap of them. */
    List<Biosphere> biosphereCandidates(double ox, double oz, int cap) {
        long seed = sampler.seed();
        int cell = Biosphere.KIND.cell;
        List<Biosphere> out = new ArrayList<>();
        int ocx = (int) Math.floor(ox / cell), ocz = (int) Math.floor(oz / cell);
        for (int ring = 0; ring <= EndSearch.RADIUS / cell + 1 && out.size() < cap; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.abs(i) != ring && Math.abs(j) != ring) continue;
                    Biosphere b = Biosphere.KIND.inCell(seed, ocx + i, ocz + j, sampler.structureProbe());
                    if (b != null && out.size() < cap) out.add(b);
                }
            }
        }
        return out;
    }

    static Target ofBall(Biosphere b) {
        return new Target("biosphere", b.centreX, b.y, b.centreZ, b.radius, b.key());
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) ^ ((long) cz & 0xFFFFFFFFL);
    }
}
