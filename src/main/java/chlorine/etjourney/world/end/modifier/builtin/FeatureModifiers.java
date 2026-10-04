package chlorine.etjourney.world.end.modifier.builtin;

import java.util.Arrays;
import java.util.List;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.modifier.TerrainView;

/** Shape and block modifiers built from the chunk's features. */
public final class FeatureModifiers {

    /** Footprint reach, in island radii, over which a zone island's surface is repainted. */
    private static final double PAINT_REACH = 1.3;
    private static final int PAINT_DEPTH = 3;

    private FeatureModifiers() {}

    /** Shapes and blocks that every region runs: zone islands, holes, lake water and shoals. */
    public static List<Modifier> core() {
        return Arrays.asList(zoneIslands(), holes(), lakeWater(), shoals());
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
                out.add(new ChunkShape(area) {

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
                    Zone zone = ZoneIslands.zoneOf(area.seed, owner);
                    int depth = -1;
                    for (int y = sink.height() - 1; y >= 0; y--) {
                        if (sink.isAir(x, y, z)) {
                            depth = -1;
                            continue;
                        }
                        depth++;
                        if (depth < PAINT_DEPTH) sink.set(x, y, z, depth == 0 ? zone.top : zone.fill);
                    }
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
                int top = sink.height() - 1;
                while (top >= 0 && sink.isAir(x, top, z)) top--;
                if (top < 0) return;
                int bottom = 0;
                while (sink.isAir(x, bottom, z)) bottom++;
                int remove = cut >= 1 ? top + 1 : (int) Math.round((top - bottom + 1) * cut);
                for (int y = top; y > top - remove && y >= 0; y--) sink.clear(x, y, z);
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
                if (lake == null || lake.waterLevel >= sink.height()) return;
                int ground = lake.waterLevel;
                while (ground >= 0 && sink.isAir(x, ground, z)) ground--;
                if (ground < 0) return;
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
                    for (int y = Math.max(0, span[0]); y <= Math.min(sink.height() - 1, span[1]); y++) {
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
                ArcPaths.Segments segments = area.view.arcs();
                if (segments.isEmpty()) return;
                out.add(new ChunkShape(area) {

                    @Override
                    public double density(double x, double y, double z) {
                        return ArcPaths.density(segments, x, y, z);
                    }
                });
            }
        };
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
            && y >= 0
            && y < sink.height();
    }

    /** A shape bounded by the chunk it was built for, padded by one density cell. */
    private abstract static class ChunkShape implements Shape {

        private final ChunkArea area;

        ChunkShape(ChunkArea area) {
            this.area = area;
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
