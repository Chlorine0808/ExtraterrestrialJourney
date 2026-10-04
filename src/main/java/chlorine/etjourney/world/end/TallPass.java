package chlorine.etjourney.world.end;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Shape;

/**
 * Terrain above the generator's Y 127: the same density function on a grid for Y 128-256, interpolated exactly as
 * the generator interpolates its own, so the surface carries on across Y 128 without a step or vertical streaks.
 */
public final class TallPass {

    public static final int BASE_Y = 128;
    /** Columns reaching this high need the upper grid (one 4-block cell below the ceiling). */
    private static final double REACH = 124;

    private TallPass() {}

    public interface SolidVisitor {

        /** lx and lz are 0-15 within the chunk; y is the world height. */
        void solid(int lx, int y, int lz);
    }

    public static boolean needed(ChunkPlan plan) {
        int ox = plan.area()
            .originX(),
            oz = plan.area()
                .originZ();
        List<ColumnState> columns = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) columns.add(plan.column(ox + i * 8, oz + j * 8));
        }
        return needed(columns, plan.shapes());
    }

    /** True when a column's surface or slab, or a shape, reaches the cell below the generator's ceiling. */
    public static boolean needed(List<ColumnState> columns, List<Shape> shapes) {
        for (ColumnState column : columns) {
            if (column.top >= REACH) return true;
            for (Layer layer : column.layers) if (layer.top >= REACH) return true;
        }
        for (Shape shape : shapes) if (shape.maxY() >= REACH) return true;
        return false;
    }

    /** The density grid for Y 128-256 (node k at Y 128 + 4k). */
    public static double[] upperField(ChunkPlan plan) {
        double[] field = new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
        DensityBuilder.fill(plan, field, plan.area().chunkX * 2, plan.area().chunkZ * 2, 32);
        return field;
    }

    /** Trilinear interpolation over 8x4x8-block cells, as the vanilla End generator does, for Y 128-255. */
    public static void forEachSolid(double[] f, SolidVisitor visitor) {
        int sy = DensityBuilder.SIZE_Y, sz = DensityBuilder.SIZE_Z;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 32; k++) {
                    double a0 = f[(i * sz + j) * sy + k], b0 = f[(i * sz + j + 1) * sy + k];
                    double c0 = f[((i + 1) * sz + j) * sy + k], d0 = f[((i + 1) * sz + j + 1) * sy + k];
                    double a1 = f[(i * sz + j) * sy + k + 1], b1 = f[(i * sz + j + 1) * sy + k + 1];
                    double c1 = f[((i + 1) * sz + j) * sy + k + 1], d1 = f[((i + 1) * sz + j + 1) * sy + k + 1];
                    for (int l = 0; l < 4; l++) {
                        double ty = l / 4.0;
                        double a = a0 + (a1 - a0) * ty, b = b0 + (b1 - b0) * ty;
                        double c = c0 + (c1 - c0) * ty, d = d0 + (d1 - d0) * ty;
                        for (int x = 0; x < 8; x++) {
                            double tx = x / 8.0;
                            double near = a + (c - a) * tx, far = b + (d - b) * tx;
                            for (int z = 0; z < 8; z++) {
                                if (near + (far - near) * (z / 8.0) > 0) {
                                    visitor.solid(i * 8 + x, BASE_Y + k * 4 + l, j * 8 + z);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
