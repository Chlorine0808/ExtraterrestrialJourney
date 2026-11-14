package chlorine.etjourney.content.area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.registry.RegistryNames;

/** One kind of lingering area: a row of the area table. */
public final class AreaSpec {

    private static final double FLOOR_HEIGHT = 0.5, CLOUD_HEIGHT = 2;

    public final String name;
    public final AreaShape shape;
    public final AreaSize size;
    /** Thickness in blocks: up from a floor's base, or split above and below a cloud's centre. */
    public final double height;
    /** Ticks between two workings on the same creature. */
    public final int interval;
    /** Null for an area that only slows or gives effects. */
    public final ElementalDamage damage;
    /** Movement speed multiplier while inside; 1 leaves it alone. */
    public final double slow;
    /** Whether an enemy's area also works on other mobs. */
    public final boolean hitsAllies;
    public final List<AreaEffect> effects;
    public final String particle;

    private AreaSpec(Builder b, AreaSize size, double height) {
        name = b.name;
        shape = b.shape;
        this.size = size;
        this.height = height;
        interval = b.interval;
        damage = b.damage;
        slow = b.slow;
        hitsAllies = b.hitsAllies;
        effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        particle = b.particle;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private AreaShape shape = AreaShape.FLOOR;
        private double radius = 2, height = Double.NaN, startRadius = Double.NaN, radiusPerUse, minRadius = 0.5;
        private int duration = 200, growTicks, shrinkTicks, interval = 20;
        private ElementalDamage damage;
        private double slow = 1;
        private boolean hitsAllies;
        private final List<AreaEffect> effects = new ArrayList<>();
        private String particle = "smoke";

        private Builder(String name) {
            this.name = RegistryNames.validate(name);
        }

        public Builder shape(AreaShape v) {
            shape = v;
            return this;
        }

        public Builder radius(double v) {
            radius = v;
            return this;
        }

        public Builder height(double v) {
            height = v;
            return this;
        }

        public Builder duration(int v) {
            duration = v;
            return this;
        }

        public Builder startRadius(double v) {
            startRadius = v;
            return this;
        }

        public Builder growTicks(int v) {
            growTicks = v;
            return this;
        }

        public Builder shrinkTicks(int v) {
            shrinkTicks = v;
            return this;
        }

        public Builder radiusPerUse(double v) {
            radiusPerUse = v;
            return this;
        }

        public Builder minRadius(double v) {
            minRadius = v;
            return this;
        }

        public Builder interval(int v) {
            interval = v;
            return this;
        }

        public Builder damage(ElementalDamage v) {
            damage = v;
            return this;
        }

        public Builder slow(double v) {
            slow = v;
            return this;
        }

        public Builder hitsAllies(boolean v) {
            hitsAllies = v;
            return this;
        }

        public Builder effect(AreaEffect v) {
            effects.add(v);
            return this;
        }

        public Builder particle(String v) {
            particle = v;
            return this;
        }

        public AreaSpec build() {
            double h = Double.isNaN(height) ? shape == AreaShape.FLOOR ? FLOOR_HEIGHT : CLOUD_HEIGHT : height;
            double start = Double.isNaN(startRadius) ? radius : startRadius;
            check(radius > 0, "radius must be > 0: " + radius);
            check(h > 0, "height must be > 0: " + h);
            check(duration > 0, "duration must be > 0: " + duration);
            check(growTicks >= 0, "growTicks must be >= 0: " + growTicks);
            check(shrinkTicks >= 0, "shrinkTicks must be >= 0: " + shrinkTicks);
            check(radiusPerUse >= 0, "radiusPerUse must be >= 0: " + radiusPerUse);
            check(minRadius >= 0, "minRadius must be >= 0: " + minRadius);
            check(start >= minRadius, "startRadius " + start + " is below minRadius " + minRadius);
            check(interval > 0, "interval must be > 0: " + interval);
            check(slow >= 0 && slow <= 1, "slow must be in [0, 1]: " + slow);
            AreaSize size = new AreaSize(start, radius, growTicks, shrinkTicks, radiusPerUse, minRadius, duration);
            return new AreaSpec(this, size, h);
        }

        private static void check(boolean ok, String message) {
            if (!ok) throw new IllegalArgumentException(message);
        }
    }
}
