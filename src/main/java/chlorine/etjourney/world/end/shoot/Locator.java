package chlorine.etjourney.world.end.shoot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EndSearch;
import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Mountains;

/** Finds screenshot targets from the seed alone, nearest first; a search that comes up short returns fewer. */
final class Locator {

    private static final double LAND = 10;
    /** Scene sizes; an arc touch is framed with the stretch of tube that comes down to the ground. */
    private static final double CONTINENT_SIZE = 96, MOUNTAIN_SIZE = 64, ARC_SCENE_SIZE = 64;

    private final TerrainSampler sampler;

    Locator(TerrainSampler sampler) {
        this.sampler = sampler;
    }

    /** Continents wearing the biome (null for bare), each from a different continent, nearest first. */
    List<Target> continents(EndBiome biome, double ox, double oz, int count) {
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
