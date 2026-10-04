package chlorine.etjourney.world.end.region;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.builtin.StyleModifiers;

/** The terrain styles carried over from the spike, as bases and overlays. Core modifiers are not listed here. */
public final class Styles {

    /** Chance of each overlay per region; with fifteen overlays most regions still get none or one. */
    private static final double OVERLAY_CHANCE = 0.06;

    public static final Style PLAINS = Style.builder("PLAINS", StyleKind.BASE_LAND)
        .share(3)
        .lakes(true)
        .holes(true)
        .build();
    public static final Style LOWLANDS = Style.builder("LOWLANDS", StyleKind.BASE_LAND)
        .share(1)
        .mountains(0.2)
        .lakes(true)
        .holes(true)
        .modifiers(StyleModifiers.lowlands())
        .build();
    public static final Style BASIN = Style.builder("BASIN", StyleKind.BASE_LAND)
        .share(1)
        .mountains(0.3)
        .valleys(0.5)
        .lakes(true)
        .holes(true)
        .modifiers(StyleModifiers.basin())
        .build();
    public static final Style RANGES = Style.builder("RANGES", StyleKind.BASE_LAND)
        .share(1)
        .mountains(0.5)
        .holes(true)
        .modifiers(StyleModifiers.ranges())
        .build();
    public static final Style LAYERED = Style.builder("LAYERED", StyleKind.BOTH)
        .share(1)
        .overlay(0.2, Style.named("PLAINS", "LOWLANDS", "BASIN"))
        .mountains(0.4)
        .valleys(0.3)
        .modifiers(StyleModifiers.layers())
        .build();
    public static final Style ISLETS = Style.builder("ISLETS", StyleKind.BASE_VOID)
        .share(1)
        .mountains(0)
        .valleys(0)
        .build();
    /** Shoals spread over the whole End; a SHOALS base removes the continent and makes them dense. */
    public static final Style SHOALS = Style.builder("SHOALS", StyleKind.BASE_VOID)
        .share(1)
        .mountains(0)
        .valleys(0)
        .build();
    public static final Style ARCS = Style.builder("ARCS", StyleKind.BASE_VOID)
        .share(1)
        .overlay(OVERLAY_CHANCE, Style.ANY)
        .mountains(0)
        .valleys(0)
        .build();
    public static final Style SPIRES = Style.builder("SPIRES", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .mountains(0.5)
        .valleys(0.3)
        .holes(true)
        .modifiers(StyleModifiers.spires())
        .build();
    public static final Style WAVES = Style.builder("WAVES", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .mountains(0.6)
        .valleys(0.3)
        .holes(true)
        .modifiers(StyleModifiers.waves())
        .build();
    public static final Style WILD_WAVES = Style.builder("WILD_WAVES", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .mountains(0.6)
        .valleys(0.3)
        .holes(true)
        .modifiers(StyleModifiers.wildWaves())
        .build();

    /** Upside-down continents: a flat top with the relief hanging below. */
    public static final Style INVERTED = Style.builder("INVERTED", StyleKind.BASE_LAND)
        .share(1)
        .holes(true)
        .modifiers(StyleModifiers.inverted())
        .build();
    public static final Style MESAS = Style.builder("MESAS", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .valleys(0.5)
        .holes(true)
        .modifiers(StyleModifiers.mesas())
        .build();
    public static final Style MIRRORED = Style.builder("MIRRORED", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .valleys(0.3)
        .modifiers(StyleModifiers.mirroredLift(), StyleModifiers.mirrored())
        .build();
    public static final Style SHATTERED = Style.builder("SHATTERED", StyleKind.OVERLAY)
        .overlay(OVERLAY_CHANCE, Style.LAND)
        .mountains(0.5)
        .valleys(0.3)
        .modifiers(StyleModifiers.shattered())
        .build();

    private static final List<Style> ALL = Collections.unmodifiableList(
        Arrays.asList(
            PLAINS,
            LOWLANDS,
            BASIN,
            RANGES,
            LAYERED,
            ISLETS,
            SHOALS,
            ARCS,
            SPIRES,
            WAVES,
            WILD_WAVES,
            INVERTED,
            MESAS,
            MIRRORED,
            SHATTERED));

    private Styles() {}

    public static List<Style> all() {
        return ALL;
    }
}
