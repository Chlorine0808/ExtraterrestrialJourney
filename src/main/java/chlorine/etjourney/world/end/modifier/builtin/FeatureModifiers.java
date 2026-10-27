package chlorine.etjourney.world.end.modifier.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Column;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Enoki;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Ore;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.feature.Tool;
import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.feature.cloud.Anvil;
import chlorine.etjourney.world.end.feature.cloud.Billow;
import chlorine.etjourney.world.end.feature.cloud.Cumulus;
import chlorine.etjourney.world.end.feature.cloud.Lenticular;
import chlorine.etjourney.world.end.feature.cloud.Mackerel;
import chlorine.etjourney.world.end.feature.cloud.Mammatus;
import chlorine.etjourney.world.end.feature.cloud.Paper;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** Shape and block modifiers built from the chunk's features. */
public final class FeatureModifiers {

    /** Footprint reach, in island radii, over which a zone island's surface is repainted. */
    private static final double PAINT_REACH = 1.3;
    private static final int PAINT_DEPTH = 3;
    /**
     * How far outside the island's own grid density a block may lie and still be painted: over void near land the
     * terrain grid sits above the island's floor, which lifts the rim by a few blocks.
     */
    private static final double ISLAND_MARGIN = 8;

    private FeatureModifiers() {}

    /**
     * Shapes and blocks that every chunk runs. Features that belong to a style (arcs, islets, structures) check the
     * style at their own centre: their shapes reach beyond the region, and a chunk whose own weight is zero must
     * still draw the part that crosses it.
     */
    public static List<Modifier> core() {
        List<Modifier> all = new ArrayList<>(
            Arrays.asList(faces(), biomes(), zoneIslands(), ores(), holes(), lakeWater(), shoals(), arcs(), islets()));
        int order = 850;
        for (Structure.Kind<? extends Structure> kind : Structures.kinds())
            all.add(StructureModifiers.of(kind, order++));
        // Clouds block by block too, so their round edges are not cut into the grid's polygons.
        all.add(StructureModifiers.voxels(Cumulus.KIND, 855, EndBlock.STONE));
        all.add(StructureModifiers.hanging(Mammatus.KIND, 856, m -> m.centreY + 0.4 * m.radius));
        all.add(StructureModifiers.voxels(Anvil.KIND, 857, EndBlock.STONE));
        // Mushrooms block by block, so their round caps are not cut into the 8-block grid's polygons.
        all.add(StructureModifiers.voxels(Structures.MUSHROOMS, 859, EndBlock.STONE));
        all.add(StructureModifiers.ringlets());
        all.add(StructureModifiers.voxels(Structures.CROSSES, 861, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Structures.HANGING_CHAINS, 862, EndBlock.CHAIN));
        all.add(StructureModifiers.voxels(Structures.SKY_CHAINS, 863, EndBlock.CHAIN));
        all.add(StructureModifiers.voxels(Lenticular.KIND, 864, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Mackerel.KIND, 865, EndBlock.STONE));
        all.add(StructureModifiers.virga(866));
        all.add(StructureModifiers.voxels(Billow.KIND, 867, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Paper.KIND, 868, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Enoki.KIND, 869, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Column.KIND, 870, EndBlock.STONE));
        all.add(StructureModifiers.voxels(Tool.KIND, 872, EndBlock.STONE));
        all.add(StructureModifiers.spires(873));
        all.add(StructureModifiers.biospheres());
        return all;
    }

    /** How far a steep face moves in or out, in blocks. */
    private static final double FACE_DEPTH = 3;
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
                    double n = (ValueNoise.noise3(s, x, y, z, 8) - 0.5) * 2 * 0.65
                        + (ValueNoise.noise3(s + 1, x / 4.0, y, z / 4.0, 3) - 0.5) * 2 * 0.35;
                    n = Math.max(-1, Math.min(1, n * 1.4));
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
                // After the biome paint, which it paints over; shapes join by maximum, so their order is free.
                return 815;
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
                    List<ZoneIslands.Island> own = Collections.singletonList(owner);
                    // Rock the grid laid near the island's own grid density; the exact density strays from the
                    // grid by a few blocks where the rim noise changes within a cell.
                    IntPredicate onIsland = y -> area.view.densityAt(x, y, z) > -1
                        && ZoneIslands.gridDensity(area.seed, own, x, y, z) > -ISLAND_MARGIN;
                    // The dome drops steeply at its rim, so all of it is painted, not just the top few blocks.
                    int floor = (int) Math.floor(owner.y);
                    paintColumn(sink, x, z, ZoneIslands.zoneOf(area.seed, owner), above, onIsland, floor);
                });
            }
        };
    }

    /**
     * Paints the top PAINT_DEPTH blocks of every run of rock with the column's biome. It runs after sinkholes and
     * canyons, so their floors and walls are painted too; before zone islands, which paint over their own rock; and
     * before lake water and anything placed block by block (islets, structures, clouds), so those stay bare. Rock that
     * carries on above the sink is measured on the density, so both halves agree.
     */
    public static Modifier biomes() {
        return blockModifier(814, (area, view, sink) -> forEachColumn(sink, (x, z) -> {
            EndBiome biome = EndBiome.at(area.seed, view.seeds(), x, z);
            if (biome == null) return;
            int depth = 0;
            while (depth < PAINT_DEPTH && sink.maxY() + depth <= 255 && view.densityAt(x, sink.maxY() + depth, z) > 0)
                depth++;
            for (int y = sink.maxY() - 1; y >= sink.minY(); y--) {
                if (sink.isAir(x, y, z)) {
                    depth = 0;
                    continue;
                }
                if (depth < PAINT_DEPTH) sink.set(x, y, z, depth == 0 ? biome.top : biome.fill);
                depth++;
            }
        }));
    }

    /** How far up past the sink the rock is counted on the density for ore depths; deeper counts as this deep. */
    private static final int ORE_DEPTH_CAP = 32;

    /**
     * Ore veins in the rock, after both paints so the depth counts painted rock as rock, and before lake water and
     * everything placed block by block (islets, structures, clouds), which so never hold ore. An ore never replaces a
     * surface block: its depth is at least one.
     */
    public static Modifier ores() {
        return blockModifier(816, (area, view, sink) -> {
            int ox = sink.originX(), oz = sink.originZ(), minY = sink.minY(), height = sink.maxY() - minY;
            int[] depth = oreDepths(view, sink);
            EndBiome[] biomes = new EndBiome[256];
            boolean[] biomeKnown = new boolean[256];
            Boolean[] offIslands = new Boolean[256];
            for (Ore ore : Ore.values()) {
                int y0 = Math.max(minY, ore.minY), y1 = Math.min(sink.maxY() - 1, ore.maxY);
                if (y0 > y1) continue;
                int cubeY0 = Math.floorDiv(y0 - Ore.REACH, Ore.CUBE), cubeY1 = Math.floorDiv(y1 + Ore.REACH, Ore.CUBE);
                for (int cx = Math.floorDiv(ox - Ore.REACH, Ore.CUBE); cx
                    <= Math.floorDiv(ox + 15 + Ore.REACH, Ore.CUBE); cx++) {
                    for (int cz = Math.floorDiv(oz - Ore.REACH, Ore.CUBE); cz
                        <= Math.floorDiv(oz + 15 + Ore.REACH, Ore.CUBE); cz++) {
                        for (int cy = cubeY0; cy <= cubeY1; cy++) ore.veins(area.seed, cx, cy, cz, (x, y, z) -> {
                            if (x < ox || x >= ox + 16 || z < oz || z >= oz + 16 || y < y0 || y > y1) return;
                            int column = (x - ox) * 16 + z - oz;
                            int d = depth[column * height + y - minY];
                            if (d < ore.minDepth || d > ore.maxDepth) return;
                            if (ore.biome != null) {
                                if (!biomeKnown[column]) {
                                    biomes[column] = EndBiome.at(area.seed, view.seeds(), x, z);
                                    biomeKnown[column] = true;
                                }
                                if (biomes[column] != ore.biome) return;
                                if (offIslands[column] == null)
                                    offIslands[column] = ZoneIslands.ownerAt(view.zoneIslands(), x, z, PAINT_REACH)
                                        == null;
                                if (!offIslands[column]) return;
                            }
                            sink.set(x, y, z, ore.block);
                        });
                    }
                }
            }
        });
    }

    /** Each block's depth below the surface of its run of rock, -1 for air, by column then height. */
    private static int[] oreDepths(TerrainView view, BlockSink sink) {
        int minY = sink.minY(), height = sink.maxY() - minY;
        int[] depth = new int[256 * height];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = sink.originX() + lx, z = sink.originZ() + lz, base = (lx * 16 + lz) * height;
                int d = -1;
                while (d + 1 < ORE_DEPTH_CAP && sink.maxY() + d + 1 <= 255
                    && view.densityAt(x, sink.maxY() + d + 1, z) > 0) d++;
                for (int y = sink.maxY() - 1; y >= minY; y--) {
                    d = sink.isAir(x, y, z) ? -1 : Math.min(d + 1, ORE_DEPTH_CAP);
                    depth[base + y - minY] = d;
                }
            }
        }
        return depth;
    }

    /**
     * Sinkholes: remove a share of each column's rock from the top, all of it inside the shaft. The rock is measured
     * on the density across the whole height, so both halves of a column cut down to the same floor.
     */
    public static Modifier holes() {
        return blockModifier(810, (area, view, sink) -> {
            List<Holes.Hole> holes = view.holes();
            if (holes.isEmpty()) return;
            forEachColumn(sink, (x, z) -> {
                double cut = Holes.cutFraction(area.seed, holes, x, z);
                if (cut <= 0) return;
                int top = 255;
                while (top >= 0 && view.densityAt(x, top, z) <= 0) top--;
                if (top < 0) return;
                int bottom = 0;
                while (view.densityAt(x, bottom, z) <= 0) bottom++;
                int floor = cut >= 1 ? 0 : top + 1 - (int) Math.round((top - bottom + 1) * cut);
                // Up to the rock's top plus what faces() may have pushed out above it.
                int ceiling = Math.min(sink.maxY() - 1, top + (int) FACE_DEPTH + 2);
                for (int y = Math.max(floor, sink.minY()); y <= ceiling; y++) sink.clear(x, y, z);
            });
        });
    }

    /** Floods every column inside a lake's flat ring whose ground ended up below the water level. */
    public static Modifier lakeWater() {
        return blockModifier(817, (area, view, sink) -> {
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
                    int[] span = Islets.span(islet, Distance.planar(x + 0.5 - islet.x, z + 0.5 - islet.z));
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
                int[] box = Shoals.bounds(school);
                if (box[1] < sink.originX() || box[0] > sink.originX() + 15
                    || box[3] < sink.minY()
                    || box[2] >= sink.maxY()
                    || box[5] < sink.originZ()
                    || box[4] > sink.originZ() + 15) continue;
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
     * Paints the top PAINT_DEPTH blocks of every solid run of island rock in a column, and all of it from floor up.
     * When the column continues above
     * the sink, the run touching the sink's top is not a surface and is left alone; the other half paints the real top.
     * Blocks that are not on the island are never painted, and the island rock under them counts as a surface.
     */
    static void paintColumn(BlockSink sink, int x, int z, Zone zone, boolean continuesAbove, IntPredicate onIsland,
        int floor) {
        int depth = continuesAbove ? PAINT_DEPTH : -1;
        for (int y = sink.maxY() - 1; y >= sink.minY(); y--) {
            if (sink.isAir(x, y, z) || !onIsland.test(y)) {
                depth = -1;
                continue;
            }
            depth++;
            if (depth < PAINT_DEPTH || y >= floor) sink.set(x, y, z, depth == 0 ? zone.top : zone.fill);
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
