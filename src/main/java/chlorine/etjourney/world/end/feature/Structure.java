package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.noise.CellCache;

/**
 * A free-standing 3D structure placed one per grid cell: a shape with a centre and a footprint radius. Placement is
 * decided per cell from a StructureProbe, so every chunk sees the same structures.
 */
public abstract class Structure implements Shape {

    public final double centreX, centreZ;
    /** Horizontal radius covering the whole structure. */
    public final double footprint;
    private final double minY, maxY;

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
        return Math.hypot(x - centreX, z - centreZ) > footprint ? Double.NEGATIVE_INFINITY : body(x, y, z);
    }

    protected abstract double body(double x, double y, double z);

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

    /** How one kind of structure is laid out on its cell grid. */
    public abstract static class Kind<T extends Structure> {

        public final String style;
        public final int cell;
        /** Largest footprint of this kind; bounds the cell search. */
        final double maxFootprint;
        private final CellCache<T> cells = new CellCache<>(8192);

        protected Kind(String style, int cell, double maxFootprint) {
            this.style = style;
            this.cell = cell;
            this.maxFootprint = maxFootprint;
        }

        /** The structure of a cell, or null; called once per cell and seed. */
        protected abstract T compute(long seed, int cx, int cz, StructureProbe probe);

        public T inCell(long seed, int cx, int cz, StructureProbe probe) {
            return cells.get(seed, cx, cz, (s, i, j) -> {
                T structure = compute(s, i, j, probe);
                return structure == null || Math.hypot(structure.centreX, structure.centreZ) < Structures.MIN_RADIUS
                    ? null
                    : structure;
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
                    if (s == null || Math.hypot(s.centreX - x, s.centreZ - z) > range + s.footprint) continue;
                    if (out == null) out = new ArrayList<>();
                    out.add(s);
                }
            }
            return out == null ? Collections.emptyList() : out;
        }
    }
}
