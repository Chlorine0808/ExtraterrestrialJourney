package chlorine.etjourney.world.end.modifier.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.feature.cloud.Anvil;
import chlorine.etjourney.world.end.feature.cloud.Cumulus;
import chlorine.etjourney.world.end.feature.cloud.Mammatus;
import chlorine.etjourney.world.end.feature.cloud.RollCloud;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** Shape and block modifiers built from the chunk's features. */
public final class FeatureModifiers {

    /** Footprint reach, in island radii, over which a zone island's surface is repainted. */
    private static final double PAINT_REACH = 1.3;
    private static final int PAINT_DEPTH = 3;

    private FeatureModifiers() {}

    /**
     * Shapes and blocks that every chunk runs. Features that belong to a style (arcs, islets, structures) check the
     * style at their own centre: their shapes reach beyond the region, and a chunk whose own weight is zero must
     * still draw the part that crosses it.
     */
    public static List<Modifier> core() {
        List<Modifier> all = new ArrayList<>(
            Arrays.asList(faces(), zoneIslands(), holes(), lakeWater(), shoals(), arcs(), islets()));
        int order = 850;
        for (Structure.Kind<? extends Structure> kind : Structures.kinds())
            all.add(StructureModifiers.of(kind, order++));
        all.add(StructureModifiers.of(Cumulus.KIND, 855));
        all.add(StructureModifiers.of(Mammatus.KIND, 856));
        all.add(StructureModifiers.of(Anvil.KIND, 857));
        all.add(StructureModifiers.of(RollCloud.KIND, 858));
        all.add(StructureModifiers.ringlets());
        all.add(StructureModifiers.voxels(Structures.CROSSES, 861, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Structures.HANGING_CHAINS, 862, EndBlock.CHAIN));
        all.add(StructureModifiers.voxels(Structures.SKY_CHAINS, 863, EndBlock.CHAIN));
        return all;
    }

    /** How far a steep face moves in or out, in blocks. */
    private static final double FACE_DEPTH = 4;
    /** Sideways density change per block at which roughening starts. */
    private static final double FACE_STEEP = 0.8;
    private static final long FACE_SALT = 0x2C9B5E7A1F3D8046L;

    /**
     * Roughens steep faces block by block. The density grid is 8 blocks wide, so inside a cell a cliff is a smooth
     * blend of its corners; this pushes the face in and out with lumps and flat ledges, more the steeper it is. Every
     * value comes from the shared grid nodes, so chunks and the two halves of a column agree. Where a 3D shape
     * decides the block, it is left alone.
     */
    public static Modifier faces() {
        return blockModifier(790, (area, view, sink) -> {
            long s = area.seed ^ FACE_SALT;
            // Each half of the world has its own grid; a sink may cover either or both.
            for (int half = 0; half < 2; half++) {
                int base = half * 128, y0 = Math.max(sink.minY(), base), y1 = Math.min(sink.maxY(), base + 128);
                if (y0 >= y1) continue;
                double[] all = view.densityField(half == 1, true), terrain = view.densityField(half == 1, false);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = (y0 - base) >> 2; k <= (y1 - 1 - base) >> 2; k++) {
                            double slope = DensityField.cellSlope(all, i, j, k);
                            // Gentle cells are left alone; FACE_STEEP is where roughening starts.
                            if (slope < FACE_STEEP) continue;
                            if (!DensityField.cellCrosses(all, i, j, k, FACE_DEPTH * slope)) continue;
                            roughenCell(view, sink, all, terrain, s, i, j, k, base, y0, y1);
                        }
                    }
                }
            }
        });
    }

    private static void roughenCell(TerrainView view, BlockSink sink, double[] all, double[] terrain, long s, int i,
        int j, int k, int base, int y0, int y1) {
        for (int lx = i * 8; lx < i * 8 + 8; lx++) {
            for (int lz = j * 8; lz < j * 8 + 8; lz++) {
                int x = sink.originX() + lx, z = sink.originZ() + lz;
                for (int ly = k * 4; ly < k * 4 + 4; ly++) {
                    int y = base + ly;
                    if (y < y0 || y >= y1) continue;
                    double d = DensityField.at(all, lx, ly, lz);
                    if (all != terrain && d > DensityField.at(terrain, lx, ly, lz) + 0.5) continue;
                    double g = DensityField.sideways(terrain, lx, ly, lz);
                    double steep = ValueNoise.smooth(Math.max(0, Math.min(1, (g - FACE_STEEP) / 1.4)));
                    double reach = FACE_DEPTH * g * steep;
                    if (reach <= 0 || Math.abs(d) >= reach) continue;
                    double n = (ValueNoise.noise3(s, x, y, z, 5) - 0.5) * 2 * 0.65
                        + (ValueNoise.noise3(s + 1, x / 4.0, y, z / 4.0, 3) - 0.5) * 2 * 0.35;
                    n = Math.max(-1, Math.min(1, n * 2));
                    boolean solid = d + n * reach > 0;
                    if (solid == d > 0 || !surfaceNear(view, all, lx, ly, lz, x, y, z, d > 0)) continue;
                    if (solid && sink.isAir(x, y, z)) sink.set(x, y, z, EndBlock.STONE);
                    else if (!solid && !sink.isAir(x, y, z)) sink.clear(x, y, z);
                }
            }
        }
    }

    /**
     * Whether stepping from a block toward the surface along the gradient reaches it within FACE_DEPTH + 1 blocks.
     * The gradient alone can overstate how near the surface is where it crosses into the next grid cell.
     */
    private static boolean surfaceNear(TerrainView view, double[] f, int lx, int ly, int lz, int x, int y, int z,
        boolean solid) {
        double[] g = DensityField.gradient(f, lx, ly, lz);
        double length = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
        if (length == 0) return false;
        double sign = solid ? -1 : 1;
        for (int step = 1; step <= FACE_DEPTH + 1; step++) {
            int px = (int) Math.round(x + sign * g[0] / length * step);
            int py = (int) Math.round(y + sign * g[1] / length * step);
            int pz = (int) Math.round(z + sign * g[2] / length * step);
            if (py < 0 || py > 255) return false;
            if (view.densityAt(px, py, pz) > 0 != solid) return true;
        }
        return false;
    }

    /** Zone islands as a 3D shape, and their surface repainted with their zone's blocks. */
    public static Modifier zoneIslands() {
        return new Modifier() {

            @Override
            public int order() {
                return 800;
            }

            @Override
            public void shapes(ChunkArea area, List<Shape> out, double weight) {
                List<ZoneIslands.Island> islands = area.view.zoneIslands();
                if (islands.isEmpty()) return;
                double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
                for (ZoneIslands.Island island : islands) {
                    low = Math.min(low, island.y - island.down);
                    high = Math.max(high, island.y + island.up * 1.2);
                }
                out.add(new ChunkShape(area, low - 4, high + 4) {

                    @Override
                    public double density(double x, double y, double z) {
                        return ZoneIslands.density(islands, x, y, z, ZoneIslands.edge(area.seed, x, z));
                    }
                });
            }

            @Override
            public void blocks(ChunkArea area, BlockSink sink, double weight) {
                List<ZoneIslands.Island> islands = area.view.zoneIslands();
                if (islands.isEmpty()) return;
                forEachColumn(sink, (x, z) -> {
                    ZoneIslands.Island owner = ZoneIslands.ownerAt(islands, x, z, PAINT_REACH);
                    if (owner == null) return;
                    // The island may carry on above this sink (the generator's half ends at Y 127).
                    boolean above = owner.y + owner.up * 1.2 >= sink.maxY();
                    paintColumn(sink, x, z, ZoneIslands.zoneOf(area.seed, owner), above);
                });
            }
        };
    }

    /** Sinkholes: remove a share of each column's rock from the top, all of it inside the shaft. */
    public static Modifier holes() {
        return blockModifier(810, (area, view, sink) -> {
            List<Holes.Hole> holes = view.holes();
            if (holes.isEmpty()) return;
            forEachColumn(sink, (x, z) -> {
                double cut = Holes.cutFraction(area.seed, holes, x, z);
                if (cut <= 0) return;
                int top = sink.maxY() - 1;
                while (top >= sink.minY() && sink.isAir(x, top, z)) top--;
                if (top < sink.minY()) return;
                int bottom = sink.minY();
                while (sink.isAir(x, bottom, z)) bottom++;
                int remove = cut >= 1 ? top - sink.minY() + 1 : (int) Math.round((top - bottom + 1) * cut);
                for (int y = top; y > top - remove && y >= sink.minY(); y--) sink.clear(x, y, z);
            });
        });
    }

    /** Floods every column inside a lake's flat ring whose ground ended up below the water level. */
    public static Modifier lakeWater() {
        return blockModifier(815, (area, view, sink) -> {
            List<Lakes.Lake> lakes = view.lakes();
            if (lakes.isEmpty()) return;
            forEachColumn(sink, (x, z) -> {
                Lakes.Lake lake = Lakes.floodAt(lakes, x, z);
                if (lake == null || lake.waterLevel < sink.minY() || lake.waterLevel >= sink.maxY()) return;
                int ground = lake.waterLevel;
                while (ground >= sink.minY() && sink.isAir(x, ground, z)) ground--;
                if (ground < sink.minY()) return;
                for (int y = ground + 1; y <= lake.waterLevel; y++) sink.place(x, y, z, EndBlock.WATER);
            });
        });
    }

    /** Islets of an ISLETS region, block by block. */
    public static Modifier islets() {
        return blockModifier(820, (area, view, sink) -> {
            for (Islets.Islet islet : view.islets()) {
                forEachColumn(sink, (x, z) -> {
                    int[] span = Islets.span(islet, Math.hypot(x + 0.5 - islet.x, z + 0.5 - islet.z));
                    if (span == null) return;
                    for (int y = Math.max(sink.minY(), span[0]); y <= Math.min(sink.maxY() - 1, span[1]); y++) {
                        sink.place(x, y, z, EndBlock.STONE);
                    }
                });
            }
        });
    }

    /** Shoal platforms of every school that forms, decided by the school's own centre chunk. */
    public static Modifier shoals() {
        return blockModifier(830, (area, view, sink) -> {
            for (Shoals.School school : view.schools()) {
                int scx = (int) Math.floor(school.x) >> 4, scz = (int) Math.floor(school.z) >> 4;
                if (!Shoals.forms(school, view.reservedAt(scx, scz))) continue;
                for (int[] b : Shoals.blocks(school)) {
                    if (inside(sink, b[0], b[1], b[2])) sink.place(b[0], b[1], b[2], EndBlock.STONE);
                }
            }
        });
    }

    /** Arc tubes as a 3D shape. */
    public static Modifier arcs() {
        return new Modifier() {

            @Override
            public int order() {
                return 805;
            }

            @Override
            public void shapes(ChunkArea area, List<Shape> out, double weight) {
                // Kept segments are decided over a padded box; only those near this chunk add density in it.
                ArcPaths.Segments segments = area.view.arcs()
                    .within(area.originX() - 8, area.originX() + 24, area.originZ() - 8, area.originZ() + 24);
                if (segments.isEmpty()) return;
                out.add(new ChunkShape(area, segments.minY(), segments.maxY()) {

                    @Override
                    public double density(double x, double y, double z) {
                        return ArcPaths.density(segments, x, y, z);
                    }
                });
            }
        };
    }

    /**
     * Paints the top PAINT_DEPTH blocks of every solid run in a column. When the column continues above the sink, the
     * run touching the sink's top is not a surface and is left alone; the other half paints the real top.
     */
    static void paintColumn(BlockSink sink, int x, int z, Zone zone, boolean continuesAbove) {
        int depth = continuesAbove ? PAINT_DEPTH : -1;
        for (int y = sink.maxY() - 1; y >= sink.minY(); y--) {
            if (sink.isAir(x, y, z)) {
                depth = -1;
                continue;
            }
            depth++;
            if (depth < PAINT_DEPTH) sink.set(x, y, z, depth == 0 ? zone.top : zone.fill);
        }
    }

    interface BlockStep {

        void apply(ChunkArea area, TerrainView view, BlockSink sink);
    }

    interface ColumnVisitor {

        void visit(int x, int z);
    }

    static Modifier blockModifier(int order, BlockStep step) {
        return new Modifier() {

            @Override
            public int order() {
                return order;
            }

            @Override
            public void blocks(ChunkArea area, BlockSink sink, double weight) {
                step.apply(area, area.view, sink);
            }
        };
    }

    private static void forEachColumn(BlockSink sink, ColumnVisitor visitor) {
        for (int x = sink.originX(); x < sink.originX() + 16; x++) {
            for (int z = sink.originZ(); z < sink.originZ() + 16; z++) visitor.visit(x, z);
        }
    }

    private static boolean inside(BlockSink sink, int x, int y, int z) {
        return x >= sink.originX() && x < sink.originX() + 16
            && z >= sink.originZ()
            && z < sink.originZ() + 16
            && y >= sink.minY()
            && y < sink.maxY();
    }

    /** A shape bounded by the chunk it was built for, padded by one density cell, between two heights. */
    private abstract static class ChunkShape implements Shape {

        private final ChunkArea area;
        private final double minY, maxY;

        ChunkShape(ChunkArea area, double minY, double maxY) {
            this.area = area;
            this.minY = minY;
            this.maxY = maxY;
        }

        @Override
        public double minY() {
            return minY;
        }

        @Override
        public double maxY() {
            return maxY;
        }

        @Override
        public double minX() {
            return area.originX() - 8;
        }

        @Override
        public double maxX() {
            return area.originX() + 24;
        }

        @Override
        public double minZ() {
            return area.originZ() - 8;
        }

        @Override
        public double maxZ() {
            return area.originZ() + 24;
        }
    }
}
