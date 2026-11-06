package chlorine.etjourney.world.building;

import java.util.ArrayList;
import java.util.List;

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
        // Torches and the like break off when nothing holds them yet, so they wait for the rest of the window.
        List<int[]> later = new ArrayList<>();
        List<BlockRef> laterRefs = new ArrayList<>();
        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                int[] local = p.rotation.toLocal(t, x - p.anchorX, 0, z - p.anchorZ);
                int lx = local[0], lz = local[2];
                for (int ly = 0; ly < t.sizeY; ly++) {
                    int y = bottom + ly;
                    if (y < MIN_Y || y > MAX_Y) continue;
                    Cell cell = t.get(lx, ly, lz);
                    if (cell.isKeep()) continue;
                    written++;
                    if (cell.isAir() || p.isOpen(lx, ly, lz)) {
                        target.setAir(x, y, z);
                        continue;
                    }
                    BlockRef ref = cell.pick(Hash.hash01(p.salt, x, y, z));
                    if (target.attachable(ref)) {
                        later.add(new int[] { x, y, z });
                        laterRefs.add(ref);
                    } else target.set(x, y, z, ref, p.rotation);
                }
                written += foundation(p, t.get(lx, 0, lz), x, bottom, z, target);
            }
        }
        for (int i = 0; i < later.size(); i++) {
            int[] at = later.get(i);
            target.set(at[0], at[1], at[2], laterRefs.get(i), p.rotation);
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
