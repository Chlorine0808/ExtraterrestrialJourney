package chlorine.etjourney.world.end.modifier.builtin;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.feature.cloud.Cirrus;
import chlorine.etjourney.world.end.feature.cloud.CloudSea;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.TerrainView;

/** Modifiers of the cloud styles: thin sheets drawn block by block, and the cloud sea's sunken land. */
public final class CloudModifiers {

    /** The sheets of one column as layers whose top and bottom are whole block heights. */
    public interface SheetSource {

        void sheets(long seed, double x, double z, double ground, double weight, List<Layer> out);
    }

    private CloudModifiers() {}

    public static Modifier cirrus() {
        return sheets(871, "CIRRUS", Cirrus.LOWEST, Cirrus.HIGHEST, Cirrus::sheets);
    }

    public static Modifier cloudSea() {
        return sheets(872, "CLOUD_SEA", CloudSea.LOWEST, CloudSea.HIGHEST, CloudSea::sheets);
    }

    /** Land sinks 30 blocks, so only the peaks reach the sheet. */
    public static Modifier cloudSeaFloor() {
        return CoreModifiers.simple(215, (area, view, s, weight) -> s.level -= 30 * weight);
    }

    /**
     * Fills the air of each sheet block by block, since the 4-block density grid loses anything thinner. Sheets keep
     * out of the ground and its top two blocks, and fade out where another mod reserves the land. A sink that lies
     * wholly outside the style's heights, low to high, is skipped.
     */
    static Modifier sheets(int order, String style, int low, int high, SheetSource source) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            if (sink.maxY() <= low || sink.minY() > high) return;
            Inputs in = new Inputs(view, sink.originX(), sink.originZ(), style);
            if (!in.any()) return;
            List<Layer> found = new ArrayList<>();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    double weight = in.weight(lx, lz);
                    if (weight <= 0) continue;
                    int x = sink.originX() + lx, z = sink.originZ() + lz;
                    double ground = in.ground(lx, lz);
                    found.clear();
                    source.sheets(area.seed, x, z, ground, weight, found);
                    for (Layer sheet : found) {
                        int y0 = Math.max(sink.minY(), Math.max((int) sheet.bottom, (int) Math.floor(ground) + 3));
                        int y1 = Math.min(sink.maxY() - 1, (int) sheet.top);
                        for (int y = y0; y <= y1; y++) sink.place(x, y, z, EndBlock.STONE);
                    }
                }
            }
        });
    }

    /** {ground, weight} the sheet pass of the chunk at (originX, originZ) uses at column (x, z). */
    public static double[] sheetInputs(TerrainView view, int originX, int originZ, String style, int x, int z) {
        Inputs in = new Inputs(view, originX, originZ, style);
        return new double[] { in.ground(x - originX, z - originZ), in.weight(x - originX, z - originZ) };
    }

    /**
     * What a sheet needs at each column of a chunk, without running every column's modifier chain: the style weight
     * and the reserved-area fade come from the chunk's 3 x 3 grid columns, which the density grid builds anyway, and
     * the ground from the land's density grid.
     */
    static final class Inputs {

        private final TerrainView view;
        /** Style weight already lowered by the reserved-area fade, at the grid columns 8 blocks apart. */
        private final double[] weights = new double[9];
        private double[] lower, upper;

        Inputs(TerrainView view, int originX, int originZ, String style) {
            this.view = view;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    double x = originX + 8 * i, z = originZ + 8 * j;
                    double suppression = view.column(x, z).suppression;
                    weights[i * 3 + j] = view.weight(style, x, z) * (1 - suppression);
                }
            }
        }

        boolean any() {
            for (double w : weights) if (w > 0) return true;
            return false;
        }

        /** Weight at (lx, lz) of the chunk, blended between the grid columns. */
        double weight(int lx, int lz) {
            int i = Math.min(1, lx >> 3), j = Math.min(1, lz >> 3);
            double tx = (lx - 8 * i) / 8.0, tz = (lz - 8 * j) / 8.0;
            double near = weights[i * 3 + j] + (weights[(i + 1) * 3 + j] - weights[i * 3 + j]) * tx;
            double far = weights[i * 3 + j + 1] + (weights[(i + 1) * 3 + j + 1] - weights[i * 3 + j + 1]) * tx;
            return near + (far - near) * tz;
        }

        /** Highest block of land (free-standing shapes aside) at (lx, lz), or -1000 where there is none. */
        double ground(int lx, int lz) {
            if (lower == null) {
                lower = view.densityField(false, false);
                upper = view.densityField(true, false);
            }
            for (int y = 255; y >= 0; y--) {
                if (DensityField.at(y >= 128 ? upper : lower, lx, y & 127, lz) > 0) return y;
            }
            return -1000;
        }
    }
}
