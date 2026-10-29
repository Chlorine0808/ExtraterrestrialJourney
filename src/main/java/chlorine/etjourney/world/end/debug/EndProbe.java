package chlorine.etjourney.world.end.debug;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndFlora;
import chlorine.etjourney.world.end.EndLargeFlora;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.biosphere.SampleWorlds;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Flora;
import chlorine.etjourney.world.end.feature.plant.GiantTrees;
import chlorine.etjourney.world.end.region.Style;

/**
 * Headless measurements of the End terrain, run at server start only when a file named etj-endscan exists in the
 * server directory: generation time, unpopulated chunks, land coverage, surface heights, plants and style shares.
 */
public final class EndProbe {

    private static final int CX0 = 40, CX1 = 112, CZ0 = -56, CZ1 = 16;

    private EndProbe() {}

    public static void runIfRequested() {
        if (new File("etj-biospheres").exists()) {
            biospheres();
            return;
        }
        if (!new File("etj-endscan").exists()) return;
        DimensionManager.initDimension(1);
        WorldServer end = DimensionManager.getWorld(1);
        Long seed = EndTerrain.seed();
        if (end == null || seed == null) return;
        long start = System.nanoTime();
        // Load in rings around the centre, like a player arriving there.
        int ccx = (CX0 + CX1) / 2, ccz = (CZ0 + CZ1) / 2;
        for (int r = 0; r <= (CX1 - CX0) / 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r)
                        end.theChunkProviderServer.loadChunk(ccx + dx, ccz + dz);
                }
            }
        }
        int unpopulated = 0;
        for (int cx = CX0 + 1; cx < CX1; cx++) {
            for (int cz = CZ0 + 1; cz < CZ1; cz++) {
                if (!end.getChunkFromChunkCoords(cx, cz).isTerrainPopulated) unpopulated++;
            }
        }
        long[] density = EndTerrain.densityStats();
        ModLog.LOG.info(
            "[probe] loaded in {} ms; interior unpopulated {}; density field avg {} us over {} fields",
            (System.nanoTime() - start) / 1_000_000,
            unpopulated,
            density[0],
            density[1]);
        surfaces(end);
        plants(end);
        giantTrees(seed);
        styles(EndTerrain.sampler(seed));
    }

    private static void surfaces(WorldServer end) {
        int columns = 0, solid = 0, highest = 0;
        int[] tops = new int[16];
        for (int x = (CX0 + 1) * 16; x < CX1 * 16; x += 8) {
            for (int z = (CZ0 + 1) * 16; z < CZ1 * 16; z += 8) {
                columns++;
                for (int y = 255; y > 0; y--) {
                    if (end.getBlock(x, y, z) != Blocks.air) {
                        solid++;
                        tops[y >> 4]++;
                        highest = Math.max(highest, y);
                        break;
                    }
                }
            }
        }
        StringBuilder histogram = new StringBuilder();
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] > 0) histogram.append(' ')
                .append(i * 16)
                .append("+:")
                .append(Math.round(1000.0 * tops[i] / Math.max(1, solid)) / 10.0)
                .append('%');
        }
        ModLog.LOG.info(
            "[probe] land coverage {}% of {} columns; surface Y:{}; highest {}",
            Math.round(1000.0 * solid / columns) / 10.0,
            columns,
            histogram,
            highest);
    }

    /** Plants per open biome surface over every fourth column, plants off their own biome, and populate time. */
    private static void plants(WorldServer end) {
        Map<Block, Flora> plants = new HashMap<>();
        for (Flora f : Flora.values()) if (EndPalette.isBound(f.block)) plants.put(EndPalette.block(f.block), f);
        Map<Block, EndBiome> tops = new HashMap<>();
        for (EndBiome b : EndBiome.values()) if (EndPalette.isBound(b.top)) tops.put(EndPalette.block(b.top), b);
        int[] open = new int[EndBiome.values().length], planted = new int[open.length];
        int[] counts = new int[Flora.values().length];
        int stray = 0;
        for (int x = (CX0 + 1) * 16; x < CX1 * 16; x += 4) {
            for (int z = (CZ0 + 1) * 16; z < CZ1 * 16; z += 4) {
                Block above = end.getBlock(x, 255, z);
                for (int y = 254; y > 0; y--) {
                    Block block = end.getBlock(x, y, z);
                    Flora plant = plants.get(above);
                    EndBiome biome = tops.get(block);
                    if (biome != null && (above == Blocks.air || plant != null)) open[biome.ordinal()]++;
                    if (plant != null) {
                        counts[plant.ordinal()]++;
                        if (biome == plant.biome) planted[biome.ordinal()]++;
                        else stray++;
                    }
                    above = block;
                }
            }
        }
        StringBuilder out = new StringBuilder();
        for (EndBiome b : EndBiome.values()) {
            out.append(' ')
                .append(b)
                .append(':')
                .append(planted[b.ordinal()])
                .append('/')
                .append(open[b.ordinal()]);
        }
        for (Flora f : Flora.values()) out.append(' ')
            .append(f)
            .append('=')
            .append(counts[f.ordinal()]);
        long[] stats = EndFlora.stats();
        ModLog.LOG.info(
            "[probe] plants per open surface:{}; off their biome {}; populate avg {} us over {} windows",
            out,
            stray,
            stats[0],
            stats[1]);
        long[] large = EndLargeFlora.stats();
        ModLog.LOG
            .info("[probe] large plants populate avg {} us over {} windows; {} placed", large[0], large[1], large[2]);
    }

    /** Counts the giant trees rooted inside the scanned chunk area, found from the continent seeds there. */
    private static void giantTrees(long seed) {
        double x0 = CX0 * 16, x1 = CX1 * 16, z0 = CZ0 * 16, z1 = CZ1 * 16;
        Set<Long> continents = new HashSet<>();
        Set<String> trees = new HashSet<>();
        for (int i = (int) Math.floor(x0 / Continent.SEED_CELL); i <= (int) Math.floor(x1 / Continent.SEED_CELL); i++) {
            for (int j = (int) Math.floor(z0 / Continent.SEED_CELL); j
                <= (int) Math.floor(z1 / Continent.SEED_CELL); j++) {
                Continent.Seed s = Continent.seedAt(seed, i, j);
                if (s == null || !continents.add(ContinentIds.of(seed, s))) continue;
                for (double[] a : GiantTrees.anchors(
                    seed,
                    s,
                    EndTerrain.sampler(seed)
                        .structureProbe())) {
                    if (a[0] >= x0 && a[0] < x1 && a[2] >= z0 && a[2] < z1) trees.add((int) a[0] + "," + (int) a[2]);
                }
            }
        }
        ModLog.LOG.info(
            "[probe] giant trees rooted in the scanned area (chunks {}..{} x {}..{}): {} from {} continents",
            CX0,
            CX1,
            CZ0,
            CZ1,
            trees.size(),
            continents.size());
    }

    private static void styles(TerrainSampler sampler) {
        Map<Style, Double> share = new LinkedHashMap<>();
        int samples = 0;
        for (int x = -8000; x <= 8000; x += 64) {
            for (int z = -8000; z <= 8000; z += 64) {
                sampler.weights(x, z)
                    .asMap()
                    .forEach((style, w) -> share.merge(style, w, Double::sum));
                samples++;
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Style, Double> e : share.entrySet()) {
            out.append(' ')
                .append(e.getKey())
                .append(':')
                .append(Math.round(1000.0 * e.getValue() / samples) / 10.0)
                .append('%');
        }
        ModLog.LOG.info("[probe] style weights (overlays add on top):{}", out);
    }

    /** Fills biospheres of each kind and logs, per ball, the time taken and what lies inside. */
    private static void biospheres() {
        DimensionManager.initDimension(1);
        WorldServer end = DimensionManager.getWorld(1);
        Long seed = EndTerrain.seed();
        if (end == null || seed == null) return;
        TerrainSampler sampler = EndTerrain.sampler(seed);
        // How many balls of each kind to fill: SURFACE, CAVE, NETHER.
        int[] wanted = { 3, 2, 3 };
        int left = 8;
        for (int r = 8; r < 400 && left > 0; r++) {
            for (int i = -r; i <= r && left > 0; i++) {
                for (int j = -r; j <= r && left > 0; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != r) continue;
                    Biosphere b = Biosphere.KIND.inCell(seed, i, j, sampler.structureProbe());
                    if (b == null) continue;
                    BiosphereSource.Kind kind = BiosphereSource.kind(seed, b);
                    if (wanted[kind.ordinal()] == 0) continue;
                    wanted[kind.ordinal()]--;
                    left--;
                    long start = System.nanoTime();
                    int c0x = ((int) Math.floor(b.minX()) >> 4) - 2, c1x = ((int) Math.floor(b.maxX()) >> 4) + 2;
                    int c0z = ((int) Math.floor(b.minZ()) >> 4) - 2, c1z = ((int) Math.floor(b.maxZ()) >> 4) + 2;
                    for (int cx = c0x; cx <= c1x; cx++) {
                        for (int cz = c0z; cz <= c1z; cz++) end.theChunkProviderServer.loadChunk(cx, cz);
                    }
                    long took = System.nanoTime() - start;
                    int glass = 0, solid = 0, inside = 0;
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    for (int x = (int) Math.floor(b.minX()); x <= (int) Math.ceil(b.maxX()); x++) {
                        for (int z = (int) Math.floor(b.minZ()); z <= (int) Math.ceil(b.maxZ()); z++) {
                            for (int y = Math.max(0, (int) b.minY()); y <= Math.min(255, (int) b.maxY()); y++) {
                                Biosphere.Part part = b.part(x, y, z);
                                Block k = end.getBlock(x, y, z);
                                if (part == Biosphere.Part.GLASS && k == Blocks.glass) glass++;
                                if (part != Biosphere.Part.INSIDE) continue;
                                inside++;
                                if (k == Blocks.air) continue;
                                solid++;
                                counts.merge(k.getUnlocalizedName(), 1, Integer::sum);
                            }
                        }
                    }
                    ModLog.LOG.info(
                        "[probe] biosphere {},{},{} r={} {}: {} ms, glass {}, filled {} of {} -> {}; {}",
                        (int) b.centreX,
                        (int) b.y,
                        (int) b.centreZ,
                        (int) b.radius,
                        kind,
                        took / 1_000_000,
                        glass,
                        solid,
                        inside,
                        counts,
                        biomes(end, seed, b));
                }
            }
        }
        // Chunks of the sample worlds are thrown away; nothing they held may stay in the worlds' lists.
        for (BiosphereSource.Kind kind : new BiosphereSource.Kind[] { BiosphereSource.Kind.SURFACE,
            BiosphereSource.Kind.NETHER }) {
            WorldServer w = SampleWorlds.world(kind);
            if (w == null) {
                ModLog.LOG.info("[probe] {} sample world could not be loaded", kind);
                continue;
            }
            ModLog.LOG.info(
                "[probe] {} sample world keeps {} chunks, {} entities, {} tile entities",
                kind,
                w.theChunkProviderServer.loadedChunks.size(),
                w.loadedEntityList.size(),
                w.loadedTileEntityList.size());
        }
        ModLog.LOG.info("[probe] End rain strength {}", end.getRainStrength(1));
        ModLog.LOG.info("[probe] biospheres done");
    }

    /**
     * How the ball's columns match its sample's biome map: the site, the covered columns holding the sample's
     * biome, and the columns around the ball holding something other than the End's own biome.
     */
    private static String biomes(WorldServer end, long seed, Biosphere b) {
        SampleWorlds.Site site = SampleWorlds.site(seed, b);
        if (site == null) return "no site";
        int cx = (int) Math.floor(b.centreX), cz = (int) Math.floor(b.centreZ);
        int covered = 0, matched = 0, stray = 0, excluded = 0;
        for (int x = (int) Math.floor(b.minX()) - 2; x <= (int) Math.ceil(b.maxX()) + 2; x++) {
            for (int z = (int) Math.floor(b.minZ()) - 2; z <= (int) Math.ceil(b.maxZ()) + 2; z++) {
                int here = end.getBiomeGenForCoords(x, z).biomeID;
                if (!b.coversColumn(x, z)) {
                    if (here != end.getWorldChunkManager()
                        .getBiomeGenAt(x, z).biomeID) stray++;
                    continue;
                }
                covered++;
                if (SampleWorlds.excluded(here)) excluded++;
                if (here == site.world.getWorldChunkManager()
                    .getBiomeGenAt(site.point[0] + x - cx, site.point[1] + z - cz).biomeID) matched++;
            }
        }
        return "site " + site.kind
            + " "
            + site.point[0]
            + ","
            + site.point[1]
            + ", biomes "
            + matched
            + " of "
            + covered
            + ", stray "
            + stray
            + ", excluded "
            + excluded;
    }
}
