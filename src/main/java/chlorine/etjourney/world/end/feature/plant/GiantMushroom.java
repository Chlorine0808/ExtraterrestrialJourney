package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The MYCELIA giant mushrooms: a stem under a domed cap whose underside is lined with gills. */
public enum GiantMushroom implements Grower {

    SMALL(6, 9, 2, 3, 0),
    MEDIUM(12, 18, 4, 6, 0),
    LARGE(22, 30, 6, 8, 1);

    private final int minHeight;
    private final int maxHeight;
    private final int minCap;
    private final int maxCap;
    private final int stemRadius;

    GiantMushroom(int minHeight, int maxHeight, int minCap, int maxCap, int stemRadius) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minCap = minCap;
        this.maxCap = maxCap;
        this.stemRadius = stemRadius;
    }

    @Override
    public int radius() {
        return maxCap;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        if (headroom < minHeight) return false;
        int height = Math.min(minHeight + random.nextInt(maxHeight - minHeight + 1), headroom);
        int cap = minCap + random.nextInt(maxCap - minCap + 1);
        int domeHeight = 1 + (cap + 1) / 2;
        int gillY = height - domeHeight - 1;

        // Cap: a flat gill disc, then a dome that is lowest at the rim and highest at the centre.
        double limit = (cap + 0.5) * (cap + 0.5);
        for (int dx = -cap; dx <= cap; dx++) {
            for (int dz = -cap; dz <= cap; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > cap * cap + 1) continue;
                out.put(x + dx, y + gillY, z + dz, EndBlock.MYCELIA_GILL);
                int layers = Math.max(1, (int) Math.ceil(domeHeight * Math.sqrt(1 - d2 / limit)));
                for (int i = 1; i <= layers; i++) {
                    out.put(x + dx, y + gillY + i, z + dz, EndBlock.MYCELIA_CAP);
                }
            }
        }
        for (int dy = 0; dy < gillY; dy++) {
            for (int dx = -stemRadius; dx <= stemRadius; dx++) {
                for (int dz = -stemRadius; dz <= stemRadius; dz++) {
                    out.put(x + dx, y + dy, z + dz, EndBlock.MYCELIA_STEM);
                }
            }
        }
        return true;
    }
}
