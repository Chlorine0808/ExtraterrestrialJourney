package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.Structures;

/** Fixed terrain answers for the cloud structure tests. */
final class Probes {

    /** Every style at full weight over flat land: ground at Y 70, underside at Y 50. */
    static final StructureProbe LAND = of(1, 60, 70, 50);
    /** Every style at full weight over the void. */
    static final StructureProbe VOID = of(1, -100, -1000, -1000);

    private Probes() {}

    static StructureProbe of(double weight, double land, double ground, double underside) {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return weight;
            }

            @Override
            public double land(double x, double z) {
                return land;
            }

            @Override
            public double ground(double x, double z) {
                return ground;
            }

            @Override
            public double underside(double x, double z) {
                return underside;
            }
        };
    }

    /** First cell index whose cells all lie beyond Structures.MIN_RADIUS, where nothing is refused for nearness. */
    static int cell0(Structure.Kind<?> kind) {
        return (int) Math.ceil(Structures.MIN_RADIUS / kind.cell) + 1;
    }

    /** The first structure in the 40 x 40 cells from cell0. */
    static <T extends Structure> T first(Structure.Kind<T> kind, long seed, StructureProbe probe) {
        int c0 = cell0(kind);
        for (int i = 0; i < 1600; i++) {
            T s = kind.inCell(seed, c0 + i % 40, c0 + i / 40, probe);
            if (s != null) return s;
        }
        return null;
    }

    static int count(Structure.Kind<?> kind, long seed, StructureProbe probe) {
        int n = 0, c0 = cell0(kind);
        for (int i = 0; i < 1600; i++) if (kind.inCell(seed, c0 + i % 40, c0 + i / 40, probe) != null) n++;
        return n;
    }
}
