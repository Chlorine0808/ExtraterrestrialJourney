package chlorine.etjourney.world.end.modifier.builtin;

import java.util.List;

import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.modifier.BiomePart;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
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
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            int ox = sink.originX(), oz = sink.originZ();
            for (Structure s : kind.near(area.seed, ox + 8, oz + 8, 12, view.structures())) {
                List<Area> reserved = view
                    .reservedAt((int) Math.floor(s.centreX) >> 4, (int) Math.floor(s.centreZ) >> 4);
                Shape placed = placed(reserved, s, kind);
                if (placed == null) continue;
                int x0 = Math.max(ox, (int) Math.floor(s.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(s.maxX()));
                int z0 = Math.max(oz, (int) Math.floor(s.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(s.maxZ()));
                int y0 = Math.max(sink.minY(), (int) Math.floor(s.minY()));
                int y1 = Math.min(sink.maxY() - 1, (int) Math.ceil(s.maxY()));
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        for (int y = y0; y <= y1; y++) {
                            if (placed.density(x + 0.5, y + 0.5, z + 0.5) >= 0) sink.place(x, y, z, block);
                        }
                    }
                }
            }
        });
    }

    /**
     * Glass balls drawn block by block over whatever else is there: the shell is glass, the air inside is cleared,
     * and the floor takes the blocks of the ball's biome.
     */
    public static Modifier biospheres() {
        return FeatureModifiers.blockModifier(890, (area, view, sink) -> {
            int ox = sink.originX(), oz = sink.originZ();
            for (Biosphere b : Biosphere.KIND.near(area.seed, ox + 8, oz + 8, 12, view.structures())) {
                List<Area> reserved = view
                    .reservedAt((int) Math.floor(b.centreX) >> 4, (int) Math.floor(b.centreZ) >> 4);
                if (placed(reserved, b, Biosphere.KIND) == null) continue;
                int x0 = Math.max(ox, (int) Math.floor(b.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(b.maxX()));
                int z0 = Math.max(oz, (int) Math.floor(b.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(b.maxZ()));
                int y0 = Math.max(sink.minY(), (int) Math.floor(b.minY()));
                int y1 = Math.min(sink.maxY() - 1, (int) Math.ceil(b.maxY()));
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        for (int y = y0; y <= y1; y++) {
                            switch (b.part(x, y, z)) {
                                case GLASS:
                                    sink.set(x, y, z, EndBlock.GLASS);
                                    break;
                                case AIR:
                                    sink.clear(x, y, z);
                                    break;
                                case TOP:
                                    sink.setBiome(x, y, z, BiomePart.TOP, b.pick);
                                    break;
                                case FILLER:
                                    sink.setBiome(x, y, z, BiomePart.FILLER, b.pick);
                                    break;
                                case DEEP:
                                    sink.setBiome(x, y, z, BiomePart.DEEP, b.pick);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    /** The structure as placed, or null when it must give way to a reserved island. */
    static Shape placed(List<Area> reserved, Structure s, Structure.Kind<?> kind) {
        return kind.overIslands || !blocked(reserved, s) ? s : null;
    }

    /** On a reserved island, or standing on ground that an island's fade has lowered. */
    static boolean blocked(List<Area> reserved, Structure s) {
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
