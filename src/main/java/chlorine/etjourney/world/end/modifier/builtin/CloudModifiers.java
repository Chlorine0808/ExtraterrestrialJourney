package chlorine.etjourney.world.end.modifier.builtin;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.feature.cloud.Stratus;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;

/** Modifiers of the cloud styles: thin sheets drawn block by block, and the cloud sea's sunken land. */
public final class CloudModifiers {

    /** The sheets of one column as layers whose top and bottom are whole block heights. */
    public interface SheetSource {

        void sheets(long seed, double x, double z, double ground, double weight, List<Layer> out);
    }

    private CloudModifiers() {}

    public static Modifier stratus() {
        return sheets(870, "STRATUS", Stratus::sheets);
    }

    /**
     * Fills the air of each sheet block by block, since the 4-block density grid loses anything thinner. Sheets keep
     * out of the ground and its top two blocks, and fade out where another mod reserves the land.
     */
    static Modifier sheets(int order, String style, SheetSource source) {
        return FeatureModifiers.blockModifier(order, (area, view, sink) -> {
            List<Layer> found = new ArrayList<>();
            for (int x = sink.originX(); x < sink.originX() + 16; x++) {
                for (int z = sink.originZ(); z < sink.originZ() + 16; z++) {
                    double weight = view.weight(style, x, z);
                    if (weight <= 0) continue;
                    ColumnState column = view.column(x, z);
                    double ground = column.land > 0 ? column.top : -1000;
                    found.clear();
                    source.sheets(area.seed, x, z, ground, weight * (1 - column.suppression), found);
                    for (Layer sheet : found) {
                        int y0 = Math.max(sink.minY(), Math.max((int) sheet.bottom, (int) Math.floor(ground) + 3));
                        int y1 = Math.min(sink.maxY() - 1, (int) sheet.top);
                        for (int y = y0; y <= y1; y++) sink.place(x, y, z, EndBlock.STONE);
                    }
                }
            }
        });
    }
}
