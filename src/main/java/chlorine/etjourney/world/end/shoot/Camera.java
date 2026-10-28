package chlorine.etjourney.world.end.shoot;

/** Where the screenshot camera stands: feet position (eyes are 1.62 above) and view angles in degrees. */
final class Camera {

    final double x, y, z;
    final float yaw, pitch;

    Camera(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
