package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.registry.RegistryNames;

/** One kind of shot: a row of the projectile table. */
public final class ProjectileSpec {

    public final String name;
    /** Blocks a tick at launch. */
    public final double speed;
    /** Added downwards to the velocity each tick. */
    public final double gravity;
    /** Multiplies the velocity each tick; 1 keeps the speed. */
    public final double drag;
    public final int lifetime;
    /** Width of the cube hitbox. */
    public final float size;
    /** Null for a shot that never turns. */
    public final Homing homing;
    public final boolean piercesWalls;
    /** How many entities it passes through; 0 stops at the first. */
    public final int piercesEntities;
    public final Aim aim;
    /** For a lob: how far the arc peaks above the higher of start and target. */
    public final double arcHeight;
    public final ElementalDamage damage;
    public final List<ImpactEffect> effects;
    /** Vanilla item whose icon draws the shot. */
    public final String look;
    /** Particle name trailed behind it; empty for none. */
    public final String trail;

    private ProjectileSpec(Builder b) {
        name = b.name;
        speed = b.speed;
        gravity = b.gravity;
        drag = b.drag;
        lifetime = b.lifetime;
        size = b.size;
        homing = b.homing;
        piercesWalls = b.piercesWalls;
        piercesEntities = b.piercesEntities;
        aim = b.aim;
        arcHeight = b.arcHeight;
        damage = b.damage;
        effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        look = b.look;
        trail = b.trail;
    }

    public static Builder builder(String name, ElementalDamage damage) {
        return new Builder(name, damage);
    }

    public static final class Builder {

        private final String name;
        private final ElementalDamage damage;
        private double speed = 1, gravity, drag = 1, arcHeight = 3;
        private int lifetime = 100, piercesEntities;
        private float size = 0.25F;
        private Homing homing;
        private boolean piercesWalls;
        private Aim aim = Aim.STRAIGHT;
        private final List<ImpactEffect> effects = new ArrayList<>();
        private String look = "snowball", trail = "";

        private Builder(String name, ElementalDamage damage) {
            RegistryNames.validate(name);
            if (damage == null) throw new IllegalArgumentException("damage is null");
            this.name = name;
            this.damage = damage;
        }

        public Builder speed(double v) {
            speed = v;
            return this;
        }

        public Builder gravity(double v) {
            gravity = v;
            return this;
        }

        public Builder drag(double v) {
            drag = v;
            return this;
        }

        public Builder lifetime(int v) {
            lifetime = v;
            return this;
        }

        public Builder size(float v) {
            size = v;
            return this;
        }

        public Builder homing(Homing v) {
            homing = v;
            return this;
        }

        public Builder piercesWalls(boolean v) {
            piercesWalls = v;
            return this;
        }

        public Builder piercesEntities(int v) {
            piercesEntities = v;
            return this;
        }

        public Builder aim(Aim v) {
            aim = v;
            return this;
        }

        public Builder arcHeight(double v) {
            arcHeight = v;
            return this;
        }

        public Builder effect(ImpactEffect v) {
            effects.add(v);
            return this;
        }

        public Builder look(String v) {
            look = v;
            return this;
        }

        public Builder trail(String v) {
            trail = v;
            return this;
        }

        public ProjectileSpec build() {
            check(speed > 0, "speed must be > 0: " + speed);
            check(gravity >= 0, "gravity must be >= 0: " + gravity);
            check(drag > 0 && drag <= 1, "drag must be in (0, 1]: " + drag);
            check(lifetime > 0, "lifetime must be > 0: " + lifetime);
            check(size > 0, "size must be > 0: " + size);
            check(piercesEntities >= 0, "piercesEntities must be >= 0: " + piercesEntities);
            check(aim != Aim.LOB || gravity > 0, "a lob needs gravity");
            check(aim != Aim.LOB || arcHeight > 0, "a lob needs arcHeight > 0: " + arcHeight);
            return new ProjectileSpec(this);
        }

        private static void check(boolean ok, String message) {
            if (!ok) throw new IllegalArgumentException(message);
        }
    }
}
