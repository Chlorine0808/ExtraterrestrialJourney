package chlorine.etjourney.world.end.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.noise.Hash;

/** Draws each region cell's base style by share, then up to MAX_OVERLAYS overlays that accept that base. */
public final class RegionPicker {

    public static final int MAX_OVERLAYS = 2;

    private final List<Style> styles;
    private final List<Style> bases = new ArrayList<>();
    private final int totalShare;

    public RegionPicker(List<Style> styles) {
        this.styles = Collections.unmodifiableList(new ArrayList<>(styles));
        int total = 0;
        for (Style style : styles) {
            if (style.isBase() && style.share > 0) {
                bases.add(style);
                total += style.share;
            }
        }
        if (bases.isEmpty()) throw new IllegalArgumentException("no base styles");
        totalShare = total;
    }

    public List<Style> styles() {
        return styles;
    }

    public Style base(long seed, int cx, int cz) {
        double roll = Hash.hash01(seed ^ RegionMap.SALT, cx, cz) * totalShare;
        for (Style style : bases) {
            roll -= style.share;
            if (roll < 0) return style;
        }
        return bases.get(bases.size() - 1);
    }

    public List<Style> overlays(long seed, int cx, int cz) {
        Style base = base(seed, cx, cz);
        List<Style> out = new ArrayList<>();
        for (int i = 0; i < styles.size() && out.size() < MAX_OVERLAYS; i++) {
            Style style = styles.get(i);
            if (!style.canOverlay(base)) continue;
            if (Hash.hash01((seed ^ RegionMap.SALT) + 10 + i, cx, cz) < style.overlayChance) out.add(style);
        }
        return out;
    }

    /** True when the cell has the style as its base or as one of its overlays. */
    public boolean contains(long seed, int cx, int cz, Style style) {
        return base(seed, cx, cz) == style || overlays(seed, cx, cz).contains(style);
    }

    public Style byName(String name) {
        for (Style style : styles) {
            if (style.name.equalsIgnoreCase(name)) return style;
        }
        return null;
    }
}
