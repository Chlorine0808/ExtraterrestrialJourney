package chlorine.etjourney.world.end.shoot;

/** A place worth photographing: a centre, a rough size in blocks, and a key that tells targets apart. */
final class Target {

    final String label;
    final double x, y, z, size;
    final long key;

    Target(String label, double x, double y, double z, double size, long key) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.key = key;
    }
}
