package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * A free-standing 3D structure placed one per grid cell: a shape with a centre and a footprint radius. Placement is
 * decided per cell from a StructureProbe, so every chunk sees the same structures.
 */
public abstract class Structure implements Shape {

    public final double centreX, centreZ;
    /** Horizontal radius covering the whole structure. */
    public final double footprint;
    private final double minY, maxY;
    /** How badly bitten, 0 for whole; set once, before the structure is cached. */
    private double wear;
    private double wearScale;
    private long wearSalt;

    protected Structure(double centreX, double centreZ, double footprint, double minY, double maxY) {
        this.centreX = centreX;
        this.centreZ = centreZ;
        this.footprint = footprint;
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * Cut off beyond the footprint: chunks only see structures whose footprint reaches them, so nothing outside it may
     * lift the density.
     */
    @Override
    public final double density(double x, double y, double z) {
        if (Distance.planar(x - centreX, z - centreZ) > footprint) return Double.NEGATIVE_INFINITY;
        double d = body(x, y, z);
        if (wear <= 0) return d;
        // Bites where a 3D noise peaks; heavier wear lowers the peak that bites.
        double limit = 0.85 - 0.3 * wear;
        return Math.min(d, (limit - ValueNoise.noise3(wearSalt, x, y, z, wearScale)) * 40);
    }

    protected abstract double body(double x, double y, double z);

    @Override
    public final Line line(double x, double z) {
        if (Distance.planar(x - centreX, z - centreZ) > footprint) return Line.EMPTY;
        Line body = bodyLine(x, z);
        if (wear <= 0) return body;
        double limit = 0.85 - 0.3 * wear;
        return Line.of(
            body.minY(),
            body.maxY(),
            y -> body.solid(y) && (limit - ValueNoise.noise3(wearSalt, x, y, z, wearScale)) * 40 >= 0);
    }

    /** The body along the vertical line through (x, z); kinds drawn block by block narrow it to save work. */
    protected Line bodyLine(double x, double z) {
        return Line.of(minY(), maxY(), y -> body(x, y, z) >= 0);
    }

    void wearDown(double wear, double scale, long salt) {
        this.wear = wear;
        this.wearScale = scale;
        this.wearSalt = salt;
    }

    /** How badly bitten, from 0 (whole) to 1. */
    public double wear() {
        return wear;
    }

    /** Where the structure stands on the ground, as {x, z} points; none for floating ones. */
    public double[][] feet() {
        return new double[][] { { centreX, centreZ } };
    }

    @Override
    public double minX() {
        return centreX - footprint;
    }

    @Override
    public double maxX() {
        return centreX + footprint;
    }

    @Override
    public double minZ() {
        return centreZ - footprint;
    }

    @Override
    public double maxZ() {
        return centreZ + footprint;
    }

    @Override
    public double minY() {
        return minY;
    }

    @Override
    public double maxY() {
        return maxY;
    }

    private static final long WEAR_SALT = 0x5A8E2C7F3B1D9064L;

    /** How one kind of structure is laid out on its cell grid. */
    public abstract static class Kind<T extends Structure> {

        public final String style;
        public final int cell;
        /** Whether it may stand on or pass through reserved islands. */
        public final boolean overIslands;
        /** Largest footprint of this kind; bounds the cell search. */
        final double maxFootprint;
        private final CellCache<T> cells = new CellCache<>(8192);
        private double wearChance, wearScale;

        protected Kind(String style, int cell, double maxFootprint) {
            this(style, cell, maxFootprint, false);
        }

        protected Kind(String style, int cell, double maxFootprint, boolean overIslands) {
            this.style = style;
            this.cell = cell;
            this.maxFootprint = maxFootprint;
            this.overIslands = overIslands;
        }

        /** Let this share of the structures come out worn, bitten by noise of the given scale. */
        protected Kind<T> worn(double chance, double scale) {
            wearChance = chance;
            wearScale = scale;
            return this;
        }

        /** The structure of a cell, or null; called once per cell and seed. */
        protected abstract T compute(long seed, int cx, int cz, StructureProbe probe);

        public T inCell(long seed, int cx, int cz, StructureProbe probe) {
            return cells.get(seed, cx, cz, (s, i, j) -> {
                T structure = compute(s, i, j, probe);
                if (structure == null || Distance.planar(structure.centreX, structure.centreZ) < Structures.MIN_RADIUS)
                    return null;
                long w = s ^ WEAR_SALT ^ style.hashCode();
                if (Hash.hash01(w, i, j) < wearChance)
                    structure.wearDown(0.3 + 0.7 * Hash.hash01(w + 1, i, j), wearScale, w + 31L * i + j);
                return structure;
            });
        }

        /** Structures whose footprint reaches within range of (x, z). */
        public List<T> near(long seed, double x, double z, double range, StructureProbe probe) {
            double reach = range + maxFootprint;
            int c0x = (int) Math.floor((x - reach) / cell), c1x = (int) Math.floor((x + reach) / cell);
            int c0z = (int) Math.floor((z - reach) / cell), c1z = (int) Math.floor((z + reach) / cell);
            List<T> out = null;
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) {
                    T s = inCell(seed, cx, cz, probe);
                    if (s == null || Distance.planar(s.centreX - x, s.centreZ - z) > range + s.footprint) continue;
                    if (out == null) out = new ArrayList<>();
                    out.add(s);
                }
            }
            return out == null ? Collections.emptyList() : out;
        }
    }
}
