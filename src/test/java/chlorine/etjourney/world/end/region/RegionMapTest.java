package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;

class RegionMapTest {

    private static Map<Long, Double> asMap(List<RegionMap.CellWeight> cells) {
        Map<Long, Double> map = new HashMap<>();
        for (RegionMap.CellWeight c : cells) map.put(((long) c.cx << 32) ^ (c.cz & 0xFFFFFFFFL), c.weight);
        return map;
    }

    @Test
    void weightsSumToOne() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            double sum = 0;
            for (RegionMap.CellWeight c : RegionMap.nearCells(7L, r.nextInt(40000) - 20000, r.nextInt(40000) - 20000)) {
                sum += c.weight;
            }
            assertEquals(1, sum, 1e-9);
        }
    }

    @Test
    void weightsChangeContinuously() {
        Random r = new Random(2);
        for (int i = 0; i < 300; i++) {
            double x = r.nextInt(40000) - 20000, z = r.nextInt(40000) - 20000;
            Map<Long, Double> a = asMap(RegionMap.nearCells(7L, x, z));
            Map<Long, Double> b = asMap(RegionMap.nearCells(7L, x + 1, z));
            for (Long key : a.keySet()) {
                double other = b.containsKey(key) ? b.get(key) : 0;
                assertTrue(Math.abs(a.get(key) - other) < 0.05, "jump at " + x + "," + z);
            }
        }
    }

    @Test
    void structuresUseTheSameRegionCells() {
        assertEquals(RegionMap.REGION, StructureProbe.REGION_CELL);
    }
}
