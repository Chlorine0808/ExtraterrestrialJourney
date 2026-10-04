package chlorine.etjourney.world.end.region;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import chlorine.etjourney.world.end.modifier.Modifier;

/** A named set of modifiers, with where it may appear as a base or an overlay and which features it allows. */
public final class Style {

    public static final Predicate<Style> ANY = s -> true;
    public static final Predicate<Style> LAND = Style::hasLand;

    public final String name;
    public final StyleKind kind;
    /** Weight when drawing a base. */
    public final int share;
    /** Chance to be drawn as an overlay on an accepted base (0 for never). */
    public final double overlayChance;
    /** Weight an overlay applies with, on top of its base's full weight. */
    public final double overlayStrength;
    /** Scale of single mountains and valleys, as a share of PLAINS. */
    public final double mountains, valleys;
    public final boolean lakes, holes;
    public final List<Modifier> modifiers;
    private final Predicate<Style> overlayTarget;

    private Style(Builder b) {
        name = b.name;
        kind = b.kind;
        share = b.share;
        overlayChance = b.overlayChance;
        overlayStrength = b.overlayStrength;
        mountains = b.mountains;
        valleys = b.valleys;
        lakes = b.lakes;
        holes = b.holes;
        modifiers = Collections.unmodifiableList(Arrays.asList(b.modifiers));
        overlayTarget = b.overlayTarget;
    }

    public static Builder builder(String name, StyleKind kind) {
        return new Builder(name, kind);
    }

    public static Predicate<Style> named(String... names) {
        Set<String> set = new HashSet<>(Arrays.asList(names));
        return s -> set.contains(s.name);
    }

    public boolean isBase() {
        return kind != StyleKind.OVERLAY;
    }

    public boolean hasLand() {
        return kind == StyleKind.BASE_LAND || kind == StyleKind.BOTH;
    }

    /** Any style with an overlay chance may be laid over the bases its target accepts, never over itself. */
    public boolean canOverlay(Style base) {
        return overlayChance > 0 && base != this && overlayTarget.test(base);
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class Builder {

        private final String name;
        private final StyleKind kind;
        private int share;
        private double overlayChance;
        private double overlayStrength = 0.8;
        private double mountains = 1, valleys = 1;
        private boolean lakes, holes;
        private Modifier[] modifiers = new Modifier[0];
        private Predicate<Style> overlayTarget = s -> false;

        private Builder(String name, StyleKind kind) {
            this.name = name;
            this.kind = kind;
        }

        public Builder share(int share) {
            this.share = share;
            return this;
        }

        public Builder overlay(double chance, Predicate<Style> target) {
            this.overlayChance = chance;
            this.overlayTarget = target;
            return this;
        }

        public Builder strength(double strength) {
            this.overlayStrength = strength;
            return this;
        }

        public Builder mountains(double scale) {
            this.mountains = scale;
            return this;
        }

        public Builder valleys(double scale) {
            this.valleys = scale;
            return this;
        }

        public Builder lakes(boolean allowed) {
            this.lakes = allowed;
            return this;
        }

        public Builder holes(boolean allowed) {
            this.holes = allowed;
            return this;
        }

        public Builder modifiers(Modifier... modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Style build() {
            return new Style(this);
        }
    }
}
