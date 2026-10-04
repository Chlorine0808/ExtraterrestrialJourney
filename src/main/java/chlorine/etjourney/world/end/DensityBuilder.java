package chlorine.etjourney.world.end;

import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * Fills a 3x33x3 density grid like the vanilla End generator's (8 blocks across, 4 up), from column states and 3D
 * shapes. yOffsetCells shifts the grid up: 0 for Y 0-128, 32 for Y 128-256.
 */
public final class DensityBuilder {

    public static final int SIZE_X = 3, SIZE_Y = 33, SIZE_Z = 3;
    private static final long SALT = 0x510E527FADE682D1L;

    private DensityBuilder() {}

    /** posX and posZ are in 8-block units, as the vanilla generator passes them (chunk coordinate times 2). */
    public static void fill(ChunkPlan plan, double[] field, int posX, int posZ, int yOffsetCells) {
        fill(plan, field, posX, posZ, yOffsetCells, true);
    }

    static void fill(ChunkPlan plan, double[] field, int posX, int posZ, int yOffsetCells, boolean withShapes) {
        long s = plan.area().seed ^ SALT;
        List<Shape> shapes = withShapes ? plan.shapes() : Collections.<Shape>emptyList();
        for (int i = 0; i < SIZE_X; i++) {
            for (int j = 0; j < SIZE_Z; j++) {
                double x = posX * 8.0 + i * 8, z = posZ * 8.0 + j * 8;
                ColumnState column = plan.column(x, z);
                for (int k = 0; k < SIZE_Y; k++) {
                    double y = (k + yOffsetCells) * 4.0;
                    double d = density(s, column, x, y, z);
                    for (Shape shape : shapes) d = Math.max(d, shape.density(x, y, z));
                    field[(i * SIZE_Z + j) * SIZE_Y + k] = d;
                }
            }
        }
    }

    /** The value of the grid node at (x, y, z), as fill() computes it; nodes are shared by every chunk. */
    static double node(ChunkPlan plan, double x, double y, double z) {
        double d = density(plan.area().seed ^ SALT, plan.column(x, z), x, y, z);
        for (Shape shape : plan.shapes()) d = Math.max(d, shape.density(x, y, z));
        return d;
    }

    /** Positive inside the slab (the column's surface on top, a lumpy underside below), its layers and pillars. */
    static double density(long s, ColumnState c, double x, double y, double z) {
        if (c.land <= 0) return Math.max(-20, c.land / 4 - 2);
        double toTop = c.top - y, toBottom = y - c.bottom, thickness = c.top - c.bottom;
        double lumps = (ValueNoise.noise3(s + 7, x, y, z, 9) - 0.5) * 14
            * Math.min(1, Math.max(0, 1 - toTop / (thickness + 1)));
        double d = Math.min(toTop + (ValueNoise.noise3(s + 8, x, y, z, 6) - 0.5) * 2, toBottom + lumps);
        double highest = c.top;
        for (Layer layer : c.layers) {
            double slab = Math.min(layer.top - y, y - layer.bottom) + (ValueNoise.noise3(s + 9, x, y, z, 7) - 0.5) * 6;
            d = Math.max(d, slab);
            highest = Math.max(highest, layer.top);
        }
        if (c.pillar > 0 && y > c.top - 2 && y < highest) d = Math.max(d, c.pillar);
        return d;
    }
}
