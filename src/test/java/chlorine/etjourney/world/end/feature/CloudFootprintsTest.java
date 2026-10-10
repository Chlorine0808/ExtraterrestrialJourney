package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.cloud.Anvil;
import chlorine.etjourney.world.end.feature.cloud.Billow;
import chlorine.etjourney.world.end.feature.cloud.Cumulus;
import chlorine.etjourney.world.end.feature.cloud.Lenticular;
import chlorine.etjourney.world.end.feature.cloud.Mackerel;
import chlorine.etjourney.world.end.feature.cloud.Mammatus;
import chlorine.etjourney.world.end.feature.cloud.Paper;
import chlorine.etjourney.world.end.feature.cloud.RollCloud;
import chlorine.etjourney.world.end.feature.cloud.Virga;

/** No cloud is wider than its kind declares, or a chunk's cell search would miss it near the edge of its reach. */
class CloudFootprintsTest {

    private static final List<Structure.Kind<? extends Structure>> KINDS = Arrays.asList(
        Cumulus.KIND,
        Mammatus.KIND,
        Anvil.KIND,
        RollCloud.KIND,
        Lenticular.KIND,
        Mackerel.KIND,
        Virga.KIND,
        Billow.KIND,
        Paper.KIND);

    /** Flat land with an underside, so every kind forms. */
    private static final StructureProbe LAND = new StructureProbe() {

        @Override
        public double weight(String style, double x, double z) {
            return 1;
        }

        @Override
        public double land(double x, double z) {
            return 60;
        }

        @Override
        public double ground(double x, double z) {
            return 70;
        }

        @Override
        public double underside(double x, double z) {
            return 50;
        }
    };

    @Test
    void footprintsStayWithinTheDeclaredMaximum() {
        long seed = 780;
        for (Structure.Kind<? extends Structure> kind : KINDS) {
            int c0 = (int) Math.ceil(Structures.MIN_RADIUS / kind.cell) + 1;
            for (int i = 0; i < 2500; i++) {
                Structure s = kind.inCell(seed, c0 + i % 50, c0 + i / 50, LAND);
                if (s == null) continue;
                assertTrue(
                    s.footprint <= kind.maxFootprint,
                    kind.style + " footprint " + s.footprint + " over " + kind.maxFootprint);
            }
            seed++;
        }
    }
}
