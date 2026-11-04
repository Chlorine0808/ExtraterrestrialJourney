package chlorine.etjourney.world.end.modifier.builtin;

import java.util.List;
import java.util.function.ToDoubleFunction;

import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Spire;
import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.feature.cloud.Virga;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Shape modifiers that add free-standing structures of one kind to the chunks they reach. */
public final class StructureModifiers {

    private StructureModifiers() {}

    /** Small rings drawn block by block, so tubes thinner than the density grid stay whole. */
    public static Modifier ringlets() {
        return voxels(Structures.RINGLETS, 860, EndBlock.STONE);
    }

    /**
     * A kind drawn block by block in the block pass instead of as a density shape, for parts thinner than the
     * density grid resolves. A block is placed where the structure's density at its centre is not negative.
     */
    public static Modifier voxels(Structure.Kind<? extends Structure> kind, int order, EndBlock block) {
        return voxels(kind, order, block, false);
    }

    /** Voxels of a kind that stands on the ground, leaving out one with a foot in a lake's water. */
    public static Modifier standingVoxels(Structure.Kind<? extends Structure> kind, int order, EndBlock block) {
        return voxels(kind, order, block, true);
    }

    private static Modifier voxels(Structure.Kind<? extends Structure> kind, int order, EndBlock block,
        boolean standing) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            for (Structure s : kind.near(area.seed, sink.originX() + 8, sink.originZ() + 8, 12, view.structures())) {
                if (standing && wet(view, s)) continue;
                List<Area> reserved = view
                    .reservedAt((int) Math.floor(s.centreX) >> 4, (int) Math.floor(s.centreZ) >> 4);
                Shape placed = placed(reserved, s, kind);
                if (placed != null) draw(placed, sink, block);
            }
        });
    }

    /** Clear water kept around a foot, so a stem does not stand at the water's edge either. */
    private static final double FOOT_MARGIN = 2;

    /** Whether a foot of the structure stands in a lake's water; the same from every chunk. */
    static boolean wet(TerrainView view, Structure s) {
        for (double[] foot : s.feet()) if (view.standing()
            .wet(foot[0], foot[1], FOOT_MARGIN)) return true;
        return false;
    }

    /** How far up or down the rock's real underside is looked for, from the one the structure was placed under. */
    private static final int HANG_SEARCH = 16;
    /** Widest gap between a hanging structure and the rock above it that is filled in. */
    private static final int HANG_GAP = 12;

    /**
     * A kind hanging under the land, drawn block by block against the rock the grid actually laid. Placement only
     * knows the column's smooth underside, but the grid's lumps and slant leave the real one a few blocks off, so the
     * structure is first moved to the real underside above its centre, then every column whose top block is still
     * short of the rock is filled up to it.
     */
    public static <T extends Structure> Modifier hanging(Structure.Kind<T> kind, int order,
        ToDoubleFunction<T> underside) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            for (T s : kind.near(area.seed, sink.originX() + 8, sink.originZ() + 8, 12, view.structures())) {
                if (!stands(view, s, kind)) continue;
                Shape shape = lifted(s, liftTo(view, s, underside.applyAsDouble(s)));
                draw(shape, sink, EndBlock.STONE);
                fillUp(view, shape, sink, false);
            }
        });
    }

    /**
     * Virga hung the same way, leaving out the streaks with no rock above their root, as at the land's edge. Only
     * the streaks' roots are filled up, so a leaning streak does not grow a stub from each column it crosses.
     */
    public static Modifier virga(int order) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            for (Virga v : Virga.KIND.near(area.seed, sink.originX() + 8, sink.originZ() + 8, 12, view.structures())) {
                if (!stands(view, v, Virga.KIND)) continue;
                int dy = liftTo(view, v, v.top - 2);
                int root = (int) v.top + dy;
                // Under a sinkhole's shaft a streak hangs in the open hole: the density still has the rock it cut.
                Virga kept = v.rootedOnly((x, z) -> rockAbove(view, x, root, z) > 0);
                if (kept != null) kept = kept
                    .clearOf((x, z) -> FeatureModifiers.holedThrough(area.seed, view.holesNear(x, z, 0), view, x, z));
                if (kept == null) continue;
                Shape shape = lifted(kept, dy);
                draw(shape, sink, EndBlock.STONE);
                fillUp(view, shape, sink, true);
            }
        });
    }

    /** Whole blocks from the given underside to the real one above the structure's centre; 0 when none is near. */
    private static int liftTo(TerrainView view, Structure s, double underside) {
        int x = (int) Math.floor(s.centreX), z = (int) Math.floor(s.centreZ), y = (int) Math.floor(underside);
        if (view.densityAt(x, y, z) > 0) {
            // Inside the rock: down to its lowest block.
            for (int d = 1; d <= HANG_SEARCH; d++) if (view.densityAt(x, y - d, z) <= 0) return 1 - d;
            return 0;
        }
        for (int d = 1; d <= HANG_SEARCH; d++) if (view.densityAt(x, y + d, z) > 0) return d;
        return 0;
    }

    private static Shape lifted(Shape s, int dy) {
        if (dy == 0) return s;
        return new Shape() {

            @Override
            public double minX() {
                return s.minX();
            }

            @Override
            public double maxX() {
                return s.maxX();
            }

            @Override
            public double minZ() {
                return s.minZ();
            }

            @Override
            public double maxZ() {
                return s.maxZ();
            }

            @Override
            public double minY() {
                return s.minY() + dy;
            }

            @Override
            public double maxY() {
                return s.maxY() + dy;
            }

            @Override
            public double density(double x, double y, double z) {
                return s.density(x, y - dy, z);
            }

            @Override
            public Line line(double x, double z) {
                Line line = s.line(x, z);
                return Line.of(line.minY() + dy, line.maxY() + dy, y -> line.solid(y - dy));
            }
        };
    }

    /** Fills the air between each column's top block of the shape and the rock just above it, within the sink. */
    private static void fillUp(TerrainView view, Shape shape, BlockSink sink, boolean fromTopOnly) {
        int ox = sink.originX(), oz = sink.originZ();
        int x0 = Math.max(ox, (int) Math.floor(shape.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(shape.maxX()));
        int z0 = Math.max(oz, (int) Math.floor(shape.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(shape.maxZ()));
        int bottom = Math.max(0, (int) Math.floor(shape.minY())), top = Math.min(254, (int) Math.ceil(shape.maxY()));
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                Shape.Line line = shape.line(x + 0.5, z + 0.5);
                int y = Math.min(top, lastBlockTo(line.maxY()));
                int lowest = Math.max(bottom, firstBlockFrom(line.minY()));
                while (y >= lowest && !line.solid(y + 0.5)) y--;
                if (y < lowest) y = bottom - 1;
                if (y < bottom || fromTopOnly && y < top - 1) continue;
                int gap = rockAbove(view, x, y, z);
                if (gap <= 0) continue;
                for (int f = Math.max(y + 1, sink.minY()); f < Math.min(y + gap, sink.maxY()); f++) {
                    sink.place(x, f, z, EndBlock.STONE);
                }
            }
        }
    }

    /** Blocks up from (x, y, z) to the first rock within HANG_GAP above it, or 0 when there is none. */
    private static int rockAbove(TerrainView view, int x, int y, int z) {
        for (int gap = 1; gap <= HANG_GAP && y + gap <= 255; gap++) if (view.densityAt(x, y + gap, z) > 0) return gap;
        return 0;
    }

    /** The lowest block whose centre lies at or above y. */
    private static int firstBlockFrom(double y) {
        return (int) Math.max(Integer.MIN_VALUE / 2, Math.min(Integer.MAX_VALUE / 2, Math.ceil(y - 0.5)));
    }

    /** The highest block whose centre lies at or below y. */
    private static int lastBlockTo(double y) {
        return (int) Math.max(Integer.MIN_VALUE / 2, Math.min(Integer.MAX_VALUE / 2, Math.floor(y - 0.5)));
    }

    /** Places the block wherever the shape's density at the block centre is not negative, within the sink. */
    private static void draw(Shape shape, BlockSink sink, EndBlock block) {
        int ox = sink.originX(), oz = sink.originZ();
        int x0 = Math.max(ox, (int) Math.floor(shape.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(shape.maxX()));
        int z0 = Math.max(oz, (int) Math.floor(shape.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(shape.maxZ()));
        int y0 = Math.max(sink.minY(), (int) Math.floor(shape.minY()));
        int y1 = Math.min(sink.maxY() - 1, (int) Math.ceil(shape.maxY()));
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                Shape.Line line = shape.line(x + 0.5, z + 0.5);
                int top = Math.min(y1, lastBlockTo(line.maxY()));
                for (int y = Math.max(y0, firstBlockFrom(line.minY())); y <= top; y++) {
                    if (line.solid(y + 0.5)) sink.place(x, y, z, block);
                }
            }
        }
    }

    /** Spire needles drawn block by block, leaving out those whose root a sinkhole has taken away. */
    public static Modifier spires(int order) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            for (Spire s : Spire.KIND.near(area.seed, sink.originX() + 8, sink.originZ() + 8, 12, view.structures())) {
                if (!stands(view, s, Spire.KIND)) continue;
                Spire kept = s.outsideHoles(area.seed, view.holes());
                if (kept != null) kept = kept.outsideLakes(view.standing());
                if (kept != null) draw(kept, sink, EndBlock.STONE);
            }
        });
    }

    /** Glass balls drawn block by block over whatever else is there, cleared inside for the populate pass. */
    public static Modifier biospheres() {
        return FeatureModifiers.blockModifier(890, (area, view, sink) -> drawBiospheres(area.seed, view, sink));
    }

    private static void drawBiospheres(long seed, TerrainView view, BlockSink sink) {
        int ox = sink.originX(), oz = sink.originZ();
        for (Biosphere b : Biosphere.KIND.near(seed, ox + 8, oz + 8, 12, view.structures())) {
            if (!stands(view, b, Biosphere.KIND)) continue;
            int x0 = Math.max(ox, (int) Math.floor(b.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(b.maxX()));
            int z0 = Math.max(oz, (int) Math.floor(b.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(b.maxZ()));
            int y0 = Math.max(sink.minY(), (int) Math.floor(b.minY()));
            int y1 = Math.min(sink.maxY() - 1, (int) Math.ceil(b.maxY()));
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        Biosphere.Part part = b.part(x, y, z);
                        if (part == Biosphere.Part.GLASS) sink.set(x, y, z, EndBlock.GLASS);
                        else if (part == Biosphere.Part.INSIDE) sink.clear(x, y, z);
                    }
                }
            }
        }
    }

    /** Whether a structure forms, rather than giving way to a reserved island; decided at its centre chunk. */
    public static boolean stands(TerrainView view, Structure s, Structure.Kind<?> kind) {
        List<Area> reserved = view.reservedAt((int) Math.floor(s.centreX) >> 4, (int) Math.floor(s.centreZ) >> 4);
        return placed(reserved, s, kind) != null;
    }

    /** The structure as placed, or null when it must give way to a reserved island. */
    static Shape placed(List<Area> reserved, Structure s, Structure.Kind<?> kind) {
        return kind.overIslands || !blocked(reserved, s) ? s : null;
    }

    /** On a reserved island, or standing on ground that an island's fade has lowered; never above the islands. */
    static boolean blocked(List<Area> reserved, Structure s) {
        if (s.minY() >= Reservations.CLEAR_Y) return false;
        if (Reservations.touches(reserved, s.centreX, s.centreZ, s.footprint)) return true;
        for (double[] foot : s.feet()) {
            if (Reservations.suppression(reserved, foot[0], foot[1]) > 0.1) return true;
        }
        return false;
    }

    public static Modifier of(Structure.Kind<? extends Structure> kind, int order) {
        return new Modifier() {

            @Override
            public int order() {
                return order;
            }

            @Override
            public void shapes(ChunkArea area, List<Shape> out, double weight) {
                double cx = area.originX() + 8, cz = area.originZ() + 8;
                for (Structure s : kind.near(area.seed, cx, cz, 16, area.view.structures())) {
                    // Every kind drawn this way stands on the ground or has no feet.
                    if (wet(area.view, s)) continue;
                    // Decided by the structure's own centre chunk, so every chunk it reaches agrees.
                    List<Area> reserved = area.view
                        .reservedAt((int) Math.floor(s.centreX) >> 4, (int) Math.floor(s.centreZ) >> 4);
                    Shape placed = placed(reserved, s, kind);
                    if (placed != null) out.add(placed);
                }
            }
        };
    }
}
