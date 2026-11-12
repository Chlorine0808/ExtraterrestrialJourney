package chlorine.etjourney.content.worm;

import chlorine.etjourney.content.worm.WormPhases.Medium;

/** The values one kind of worm is built from; distances in blocks, speeds in blocks per tick. */
public final class WormSpec {

    public final Medium medium;
    /** Body segments between the head and the tail. */
    public final int bodySegments;
    public final double spacing;
    public final float size, headScale, tailScale;
    public final double speed, turnRate, gravity;
    public final int warnTicks, leapTicks, maxLeapTicks, cooldown;
    public final double strikeRange, followRange;
    /** Ground: how deep under the target it swims. Air: how high over the target it cruises. */
    public final double depth;
    public final double leapApex, overshoot, dashSpeed;
    /** Share of a hit on a body or tail segment that reaches the head. */
    public final float partDamageFactor;

    private WormSpec(Builder b) {
        medium = b.medium;
        bodySegments = b.bodySegments;
        spacing = b.spacing;
        size = b.size;
        headScale = b.headScale;
        tailScale = b.tailScale;
        speed = b.speed;
        turnRate = b.turnRate;
        gravity = b.gravity;
        warnTicks = b.warnTicks;
        leapTicks = b.leapTicks;
        maxLeapTicks = b.maxLeapTicks;
        cooldown = b.cooldown;
        strikeRange = b.strikeRange;
        followRange = b.followRange;
        depth = b.depth;
        leapApex = b.leapApex;
        overshoot = b.overshoot;
        dashSpeed = b.dashSpeed;
        partDamageFactor = b.partDamageFactor;
    }

    /** Length of the body behind the head, out to the tail. */
    public double length() {
        return (bodySegments + 1) * spacing;
    }

    public WormPhases newPhases() {
        return new WormPhases(medium, warnTicks, leapTicks, maxLeapTicks, cooldown, strikeRange);
    }

    public static Builder builder(Medium medium) {
        return new Builder(medium);
    }

    public static final class Builder {

        private final Medium medium;
        private int bodySegments = 10;
        private double spacing = 0.9;
        private float size = 1.0F, headScale = 1.25F, tailScale = 0.7F;
        private double speed = 0.35, turnRate = 0.15, gravity = 0.08;
        private int warnTicks = 30, leapTicks = 30, maxLeapTicks = 200, cooldown = 60;
        private double strikeRange = 12, followRange = 40, depth = 5;
        private double leapApex = 8, overshoot = 4, dashSpeed = 0.8;
        private float partDamageFactor = 1.0F;

        private Builder(Medium medium) {
            this.medium = medium;
        }

        public Builder body(int segments, double spacing, float size) {
            bodySegments = segments;
            this.spacing = spacing;
            this.size = size;
            return this;
        }

        public Builder swim(double speed, double turnRate) {
            this.speed = speed;
            this.turnRate = turnRate;
            return this;
        }

        public Builder strike(double range, int warnTicks, int cooldown) {
            strikeRange = range;
            this.warnTicks = warnTicks;
            this.cooldown = cooldown;
            return this;
        }

        public Builder leap(double apex, double overshoot) {
            leapApex = apex;
            this.overshoot = overshoot;
            return this;
        }

        public Builder dash(double speed, int ticks) {
            dashSpeed = speed;
            leapTicks = ticks;
            return this;
        }

        public Builder depth(double depth) {
            this.depth = depth;
            return this;
        }

        public Builder followRange(double range) {
            followRange = range;
            return this;
        }

        public Builder partDamageFactor(float factor) {
            partDamageFactor = factor;
            return this;
        }

        public WormSpec build() {
            return new WormSpec(this);
        }
    }
}
