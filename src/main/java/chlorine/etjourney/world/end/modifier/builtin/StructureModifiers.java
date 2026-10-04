package chlorine.etjourney.world.end.modifier.builtin;

import java.util.List;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
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
        return FeatureModifiers.blockModifier(860, (area, view, sink) -> {
            int ox = sink.originX(), oz = sink.originZ();
            for (Structures.Ring r : Structures.RINGLETS.near(area.seed, ox + 8, oz + 8, 12, view.structures())) {
                List<Area> reserved = view
                    .reservedAt((int) Math.floor(r.centreX) >> 4, (int) Math.floor(r.centreZ) >> 4);
                Shape placed = placed(reserved, r, Structures.RINGLETS);
                if (placed == null) continue;
                int x0 = Math.max(ox, (int) Math.floor(r.minX())), x1 = Math.min(ox + 15, (int) Math.ceil(r.maxX()));
                int z0 = Math.max(oz, (int) Math.floor(r.minZ())), z1 = Math.min(oz + 15, (int) Math.ceil(r.maxZ()));
                int y0 = Math.max(sink.minY(), (int) Math.floor(r.minY()));
                int y1 = Math.min(sink.maxY() - 1, (int) Math.ceil(r.maxY()));
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        for (int y = y0; y <= y1; y++) {
                            if (placed.density(x + 0.5, y + 0.5, z + 0.5) >= 0) sink.place(x, y, z, EndBlock.STONE);
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
