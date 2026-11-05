package chlorine.etjourney.world.end.building;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.building.BlockRef;
import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.Rotation;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;

/** One kind of End building: its template and the rules that lay it out, at most one per grid cell. */
public final class BuildingKind {

    /** A building only forms where one of its styles holds at least this weight. */
    private static final double MIN_WEIGHT = 0.5;

    public final String id;
    public final String templateName;
    public final int cell;
    public final double chance;
    public final boolean standing;
    public final int maxSlope, sink;
    public final int floatMinY, floatMaxY;
    public final BlockRef foundation;
    public final int foundationDepth;
    private final String[] styles;
    private final long salt;

    private BuildingKind(Builder b) {
        this.id = b.id;
        this.templateName = b.templateName;
        this.cell = b.cell;
        this.chance = b.chance;
        this.standing = b.standing;
        this.maxSlope = b.maxSlope;
        this.sink = b.sink;
        this.floatMinY = b.floatMinY;
        this.floatMaxY = b.floatMaxY;
        this.foundation = b.foundation;
        this.foundationDepth = b.foundationDepth;
        this.styles = b.styles;
        this.salt = 0x6A09E667F3BCC909L * (b.id.hashCode() + 1L);
    }

    public static Builder builder(String id, String templateName) {
        return new Builder(id, templateName);
    }

    /** The building of cell (cx, cz), or null; the same for every window that asks. */
    public Placement compute(long seed, int cx, int cz, Template t, StructureProbe probe,
        StructureProbe standingProbe) {
        long s = seed ^ salt;
        if (Hash.hash01(s, cx, cz) >= chance) return null;
        int x = (int) Math.floor((cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell);
        int z = (int) Math.floor((cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell);
        if (Distance.planar(x, z) < Structures.MIN_RADIUS) return null;
        if (!inStyle(probe, x, z)) return null;
        Rotation rotation = Rotation.ofQuarterTurns((int) (Hash.hash01(s + 3, cx, cz) * 4));
        int[] b = rotation.rotatedBounds(t);
        int bottom;
        if (standing) {
            Integer y = BuildingSite.groundY(standingProbe, x + b[0], z + b[1], x + b[2], z + b[3], maxSlope, sink);
            if (y == null) return null;
            bottom = y;
        } else {
            int anchorY = floatMinY + (int) (Hash.hash01(s + 4, cx, cz) * (floatMaxY - floatMinY + 1));
            bottom = anchorY - t.anchorY;
        }
        long placementSalt = s ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL);
        return new Placement(t, rotation, x, bottom + t.anchorY, z, foundation, foundationDepth, placementSalt);
    }

    /** The buildings that reach into the window [windowX, windowX + 16) x [windowZ, windowZ + 16). */
    public List<Placement> near(long seed, int windowX, int windowZ, Template t, StructureProbe probe,
        StructureProbe standingProbe) {
        int reach = Math.max(t.sizeX, t.sizeZ);
        List<Placement> found = new ArrayList<>();
        int c0x = Math.floorDiv(windowX - reach, cell), c1x = Math.floorDiv(windowX + 15 + reach, cell);
        int c0z = Math.floorDiv(windowZ - reach, cell), c1z = Math.floorDiv(windowZ + 15 + reach, cell);
        for (int cx = c0x; cx <= c1x; cx++) for (int cz = c0z; cz <= c1z; cz++) {
            Placement p = compute(seed, cx, cz, t, probe, standingProbe);
            if (p != null && p.maxX() >= windowX
                && p.minX() <= windowX + 15
                && p.maxZ() >= windowZ
                && p.minZ() <= windowZ + 15) found.add(p);
        }
        return found;
    }

    private boolean inStyle(StructureProbe probe, int x, int z) {
        if (styles.length == 0) return true;
        for (String style : styles) if (probe.weight(style, x, z) >= MIN_WEIGHT) return true;
        return false;
    }

    public static final class Builder {

        private final String id, templateName;
        private int cell = 256;
        private double chance = 0.2;
        private boolean standing = true;
        private int maxSlope = 6, sink = 2;
        private int floatMinY, floatMaxY;
        private BlockRef foundation;
        private int foundationDepth = 24;
        private String[] styles = new String[0];

        private Builder(String id, String templateName) {
            this.id = id;
            this.templateName = templateName;
        }

        public Builder cell(int cell) {
            this.cell = cell;
            return this;
        }

        public Builder chance(double chance) {
            this.chance = chance;
            return this;
        }

        public Builder standing(int maxSlope, int sink) {
            this.standing = true;
            this.maxSlope = maxSlope;
            this.sink = sink;
            return this;
        }

        public Builder floating(int minY, int maxY) {
            this.standing = false;
            this.floatMinY = minY;
            this.floatMaxY = maxY;
            return this;
        }

        public Builder foundation(BlockRef block, int depth) {
            this.foundation = block;
            this.foundationDepth = depth;
            return this;
        }

        public Builder styles(String... styles) {
            this.styles = styles.clone();
            return this;
        }

        public BuildingKind build() {
            return new BuildingKind(this);
        }
    }
}
