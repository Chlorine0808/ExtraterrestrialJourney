package chlorine.etjourney.world.end.modifier.builtin;

import java.util.List;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Shape modifiers that add free-standing structures of one kind to the chunks they reach. */
public final class StructureModifiers {

    private StructureModifiers() {}

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
                    if (blocked(reserved, s)) continue;
                    out.add(s);
                }
            }
        };
    }
}
