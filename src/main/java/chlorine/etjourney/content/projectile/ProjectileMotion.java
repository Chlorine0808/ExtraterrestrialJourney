package chlorine.etjourney.content.projectile;

import chlorine.etjourney.content.worm.Steering;

/** One tick of a shot's velocity. */
public final class ProjectileMotion {

    private ProjectileMotion() {}

    /**
     * The next velocity: turned towards the target at the same speed while homing, then pulled down, then slowed.
     *
     * @param toTarget from the shot to its target, or null when it has none
     */
    public static double[] step(double[] velocity, double gravity, double drag, Homing homing, int age,
        double[] toTarget) {
        double[] v = velocity.clone();
        if (homing != null && toTarget != null && homing.activeAt(age)) {
            double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            double[] dir = Steering.turn(v, toTarget, homing.turnRate);
            v = new double[] { dir[0] * speed, dir[1] * speed, dir[2] * speed };
        }
        v[1] -= gravity;
        for (int i = 0; i < 3; i++) v[i] *= drag;
        return v;
    }
}
