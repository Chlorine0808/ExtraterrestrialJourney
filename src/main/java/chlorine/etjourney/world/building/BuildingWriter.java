package chlorine.etjourney.world.building;

import chlorine.etjourney.world.end.noise.Hash;

/**
 * Writes the part of a placement inside one populate window. Every choice comes from the salt and the world
 * position, so the windows of one building join up whatever order they run in.
 */
public final class BuildingWriter {

    public static final int MIN_Y = 1, MAX_Y = 255;
    private static final int WINDOW = 16;

    private BuildingWriter() {}

    /** Writes the window [windowX, windowX + 16) x [windowZ, windowZ + 16); returns the positions written. */
    public static int write(Placement p, int windowX, int windowZ, BlockTarget target) {
        return write(p, windowX, windowZ, WINDOW, WINDOW, target);
    }

    /** Writes the whole building at once, as the place command does. */
    public static int writeAll(Placement p, BlockTarget target) {
        return write(p, p.minX(), p.minZ(), p.maxX() - p.minX() + 1, p.maxZ() - p.minZ() + 1, target);
    }

    private static int write(Placement p, int x0, int z0, int width, int depth, BlockTarget target) {
        Template t = p.template;
        int xa = Math.max(x0, p.minX()), xb = Math.min(x0 + width - 1, p.maxX());
        int za = Math.max(z0, p.minZ()), zb = Math.min(z0 + depth - 1, p.maxZ());
        int bottom = p.bottomY();
        int written = 0;
        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                int[] local = p.rotation.toLocal(t, x - p.anchorX, 0, z - p.anchorZ);
                int lx = local[0], lz = local[2];
                for (int ly = 0; ly < t.sizeY; ly++) {
                    int y = bottom + ly;
                    if (y < MIN_Y || y > MAX_Y) continue;
                    Cell cell = t.get(lx, ly, lz);
                    if (cell.isKeep()) continue;
                    if (cell.isAir()) target.setAir(x, y, z);
                    else target.set(x, y, z, cell.pick(Hash.hash01(p.salt, x, y, z)), p.rotation);
                    written++;
                }
                written += foundation(p, t.get(lx, 0, lz), x, bottom, z, target);
            }
        }
        return written;
    }

    private static int foundation(Placement p, Cell bottomCell, int x, int bottom, int z, BlockTarget target) {
        if (p.foundation == null || bottomCell.isKeep() || bottomCell.isAir()) return 0;
        int written = 0;
        for (int y = bottom - 1; y >= bottom - p.foundationDepth && y >= MIN_Y; y--) {
            if (y > MAX_Y) continue;
            if (!target.replaceable(x, y, z)) break;
            target.set(x, y, z, p.foundation, p.rotation);
            written++;
        }
        return written;
    }
}
