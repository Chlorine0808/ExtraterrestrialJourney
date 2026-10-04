package chlorine.etjourney.world.end;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.ModifierChain;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.modifier.builtin.FeatureModifiers;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Everything one chunk needs, gathered once and shared by the density, block and tall passes: the features near the
 * chunk, the modifier chain, and column states computed on demand.
 */
public final class ChunkPlan implements TerrainView {

    /** Reserved areas as seen from a chunk; the engine supplies the provider-backed lookup. */
    public interface ReservedLookup {

        List<Area> at(int chunkX, int chunkZ);
    }

    private static final double RANGE = 16;
    private static final double ARC_PAD = 64;
    /** How far below a segment's midpoint its tube can reach: the thickest tube plus half a segment. */
    private static final double ARC_REACH = 14;

    private final TerrainSampler sampler;
    private final ReservedLookup reservedLookup;
    private final ChunkArea area;
    private final ModifierChain chain;
    private final List<Continent.Seed> seeds;
    private final List<Mountains.Mountain> mountains;
    private final List<Lakes.Lake> lakes;
    private final List<Holes.Hole> holes;
    private final List<ZoneIslands.Island> zoneIslands;
    private final List<Islets.Islet> islets;
    private final List<Shoals.School> schools;
    private final ArcPaths.Segments arcs;
    private final Map<Long, ColumnState> columns = new ConcurrentHashMap<>();
    private volatile List<Shape> shapes;
    /** Grid nodes outside the chunk, read by densityAt(), keyed by their block position. */
    private final Map<Long, Double> outerNodes = new ConcurrentHashMap<>();
    /** Density grids by half and shapes, built on first use. */
    private final double[][] fields = new double[4][];

    public ChunkPlan(TerrainSampler sampler, int chunkX, int chunkZ, ReservedLookup reservedLookup) {
        this.sampler = sampler;
        this.reservedLookup = reservedLookup;
        long seed = sampler.seed();
        List<Area> reserved = reservedLookup.at(chunkX, chunkZ);
        this.area = new ChunkArea(seed, chunkX, chunkZ, reserved, this);
        List<Modifier> modifiers = TerrainSampler.columnModifiers(sampler.picker());
        modifiers.addAll(FeatureModifiers.core());
        this.chain = new ModifierChain(modifiers);
        double cx = chunkX * 16 + 8, cz = chunkZ * 16 + 8;
        seeds = Continent.seedsNear(seed, cx, cz, RANGE + Continent.warpReach());
        mountains = Mountains.near(seed, cx, cz, RANGE);
        lakes = Lakes.withoutReserved(Lakes.near(seed, cx, cz, RANGE, sampler.lakeProbe()), reserved);
        holes = Holes.near(seed, cx, cz, RANGE, sampler.holeProbe());
        zoneIslands = ZoneIslands
            .withoutReserved(ZoneIslands.near(seed, cx, cz, RANGE + ZoneIslands.CELL, sampler.landProbe()), reserved);
        List<Islets.Islet> keptIslets = new ArrayList<>();
        for (Islets.Islet islet : Islets.near(seed, cx, cz, RANGE, sampler.isletProbe())) {
            if (Reservations.clear(reserved, islet.x, islet.y - islet.flat - islet.depth, islet.z))
                keptIslets.add(islet);
        }
        islets = keptIslets;
        schools = Shoals.near(seed, cx, cz, RANGE, sampler.shoalProbe());
        // Arc paths wander out of their region: keep the stretches still on ARCS ground, clear of reservations and
        // zone islands. Each test uses the segment alone, so neighbouring chunks agree; the box is padded so a tube
        // left out by one chunk is too far from every shared node to move the surface.
        arcs = ArcPaths.segmentsNear(
            ArcPaths.pathsNear(seed, cx, cz, RANGE + ARC_PAD, sampler.arcProbe()),
            area.originX() - 8 - ARC_PAD,
            area.originX() + 24 + ARC_PAD,
            area.originZ() - 8 - ARC_PAD,
            area.originZ() + 24 + ARC_PAD,
            sampler.arcGround(),
            (x, y, z) -> Reservations.clear(reserved, x, y - ARC_REACH, z));
    }

    public ChunkArea area() {
        return area;
    }

    /** The column at (x, z), computed once. */
    @Override
    public ColumnState column(double x, double z) {
        // Columns sit on whole blocks; key on the block coordinates (mixing the double bits collided).
        long key = ((long) (int) Math.floor(x) << 32) ^ ((int) Math.floor(z) & 0xFFFFFFFFL);
        ColumnState state = columns.get(key);
        if (state == null) {
            state = new ColumnState(x, z);
            state.suppression = Reservations.suppression(area.reserved, x, z);
            chain.column(area, state, m -> sampler.weightOf(m, x, z));
            columns.put(key, state);
        }
        return state;
    }

    @Override
    public double[] densityField(boolean upper, boolean withShapes) {
        // Without shapes both grids are the same.
        if (withShapes && shapes().isEmpty()) withShapes = false;
        int slot = (upper ? 2 : 0) + (withShapes ? 1 : 0);
        double[] field = fields[slot];
        if (field == null) {
            field = new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
            DensityBuilder.fill(this, field, area.chunkX * 2, area.chunkZ * 2, upper ? 32 : 0, withShapes);
            fields[slot] = field;
        }
        return field;
    }

    @Override
    public double densityAt(int x, int y, int z) {
        int lx = x - area.originX(), lz = z - area.originZ();
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
            boolean upper = y >= 128;
            return DensityField.at(densityField(upper, true), lx, y - (upper ? 128 : 0), lz);
        }
        int x0 = Math.floorDiv(x, 8) * 8, y0 = Math.floorDiv(y, 4) * 4, z0 = Math.floorDiv(z, 8) * 8;
        double tx = (x - x0) / 8.0, ty = (y - y0) / 4.0, tz = (z - z0) / 8.0;
        double a = lerp(outerNode(x0, y0, z0), outerNode(x0, y0 + 4, z0), ty);
        double b = lerp(outerNode(x0, y0, z0 + 8), outerNode(x0, y0 + 4, z0 + 8), ty);
        double c = lerp(outerNode(x0 + 8, y0, z0), outerNode(x0 + 8, y0 + 4, z0), ty);
        double d = lerp(outerNode(x0 + 8, y0, z0 + 8), outerNode(x0 + 8, y0 + 4, z0 + 8), ty);
        double near = a + (c - a) * tx, far = b + (d - b) * tx;
        return near + (far - near) * tz;
    }

    private double outerNode(int x, int y, int z) {
        long key = ((long) (x >> 3) << 40) ^ ((long) (y >> 2) << 20) ^ ((z >> 3) & 0xFFFFFL);
        Double cached = outerNodes.get(key);
        if (cached != null) return cached;
        double value = DensityBuilder.node(this, x, y, z);
        outerNodes.put(key, value);
        return value;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public List<Shape> shapes() {
        if (shapes == null) shapes = chain.shapes(area, this::chunkWeight);
        return shapes;
    }

    public void blocks(BlockSink sink) {
        chain.blocks(area, sink, this::chunkWeight);
    }

    /** A modifier's strongest weight over the chunk's corners and centre: chunk-wide passes run if it is present. */
    private double chunkWeight(Modifier modifier) {
        double best = 0;
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                best = Math.max(best, sampler.weightOf(modifier, area.originX() + i * 8, area.originZ() + j * 8));
            }
        }
        return best;
    }

    @Override
    public List<Continent.Seed> seeds() {
        return seeds;
    }

    @Override
    public List<Mountains.Mountain> mountains() {
        return mountains;
    }

    @Override
    public List<Lakes.Lake> lakes() {
        return lakes;
    }

    @Override
    public List<Holes.Hole> holes() {
        return holes;
    }

    @Override
    public List<ZoneIslands.Island> zoneIslands() {
        return zoneIslands;
    }

    @Override
    public List<Islets.Islet> islets() {
        return islets;
    }

    @Override
    public List<Shoals.School> schools() {
        return schools;
    }

    @Override
    public ArcPaths.Segments arcs() {
        return arcs;
    }

    @Override
    public List<Area> reservedAt(int chunkX, int chunkZ) {
        return reservedLookup.at(chunkX, chunkZ);
    }

    @Override
    public double voidShare(double x, double z) {
        return sampler.weights(x, z)
            .voidShare();
    }

    @Override
    public double mountainScale(double x, double z) {
        return sampler.weights(x, z)
            .mountainScale();
    }

    @Override
    public double valleyScale(double x, double z) {
        return sampler.weights(x, z)
            .valleyScale();
    }

    @Override
    public double weight(String style, double x, double z) {
        return sampler.styleWeight(style, x, z);
    }

    @Override
    public StructureProbe structures() {
        return sampler.structureProbe();
    }
}
