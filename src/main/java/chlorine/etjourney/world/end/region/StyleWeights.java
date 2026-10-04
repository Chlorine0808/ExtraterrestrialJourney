package chlorine.etjourney.world.end.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Style weights at one point. Bases share a total of 1; an overlay adds its cell's weight times its strength on
 * top, so composite regions sum above 1.
 */
public final class StyleWeights {

    private final Map<Style, Double> weights;
    /** Base weights only; they sum to 1. */
    private final Map<Style, Double> bases;

    private StyleWeights(Map<Style, Double> weights, Map<Style, Double> bases) {
        this.weights = weights;
        this.bases = bases;
    }

    public static StyleWeights at(RegionPicker picker, long seed, double x, double z) {
        Map<Style, Double> map = new LinkedHashMap<>(), bases = new LinkedHashMap<>();
        for (RegionMap.CellWeight cell : RegionMap.nearCells(seed, x, z)) {
            Style base = picker.base(seed, cell.cx, cell.cz);
            map.merge(base, cell.weight, Double::sum);
            bases.merge(base, cell.weight, Double::sum);
            for (Style overlay : picker.overlays(seed, cell.cx, cell.cz)) {
                map.merge(overlay, cell.weight * overlay.overlayStrength, Double::sum);
            }
        }
        return new StyleWeights(map, bases);
    }

    /** For tests and tools: weights given directly, all treated as bases. */
    public static StyleWeights of(Map<Style, Double> weights) {
        return new StyleWeights(new LinkedHashMap<>(weights), new LinkedHashMap<>(weights));
    }

    /** Weight of a style as a base only. */
    public double baseOf(Style style) {
        Double w = bases.get(style);
        return w == null ? 0 : w;
    }

    public double of(Style style) {
        Double w = weights.get(style);
        return w == null ? 0 : w;
    }

    public Map<Style, Double> asMap() {
        return Collections.unmodifiableMap(weights);
    }

    /** Weighted mean of the styles' mountain scales; always between the scales of the styles present. */
    public double mountainScale() {
        double sum = 0, total = 0;
        for (Map.Entry<Style, Double> e : weights.entrySet()) {
            sum += e.getValue() * e.getKey().mountains;
            total += e.getValue();
        }
        return total > 0 ? sum / total : 1;
    }

    public double valleyScale() {
        double sum = 0, total = 0;
        for (Map.Entry<Style, Double> e : weights.entrySet()) {
            sum += e.getValue() * e.getKey().valleys;
            total += e.getValue();
        }
        return total > 0 ? sum / total : 1;
    }

    /** Lakes go where bases that allow them hold at least half the base weight. */
    public boolean allowsLakes() {
        double allowed = 0;
        for (Map.Entry<Style, Double> e : bases.entrySet()) {
            if (e.getKey().lakes) allowed += e.getValue();
        }
        return allowed >= 0.5;
    }

    public boolean allowsHoles() {
        double allowed = 0, denied = 0;
        for (Map.Entry<Style, Double> e : weights.entrySet()) {
            if (e.getKey().holes) allowed += e.getValue();
            else denied += e.getValue();
        }
        return allowed >= 0.8 && denied < 0.2;
    }

    /** Base weight of styles without a continent; overlays never remove land. */
    public double voidShare() {
        double sum = 0;
        for (Map.Entry<Style, Double> e : bases.entrySet()) {
            if (e.getKey().kind == StyleKind.BASE_VOID) sum += e.getValue();
        }
        return sum;
    }
}
