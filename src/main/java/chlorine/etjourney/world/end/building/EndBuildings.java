package chlorine.etjourney.world.end.building;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.building.BuildingWriter;
import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.building.TemplateStore;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.feature.Tool;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.reserve.Reservations;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/** The End buildings: registered kinds, laid out per cell and written at populate time, one window at a time. */
public final class EndBuildings implements IWorldGenerator {

    private static final int END = 1;
    private static final List<BuildingKind> KINDS = new ArrayList<>();
    private static TemplateStore store = new TemplateStore(null, EndBuildings::resource);

    private boolean warned;

    public static void register() {
        File okmodular = new File(
            Loader.instance()
                .getConfigDir(),
            "okmodular/structures");
        store = new TemplateStore(okmodular, EndBuildings::resource);
        // FML runs lighter weights first: after the biospheres (10) and the plants (4, 5).
        GameRegistry.registerWorldGenerator(new EndBuildings(), 20);
    }

    /** Adds a kind; content calls this in init. */
    public static void add(BuildingKind kind) {
        KINDS.add(kind);
    }

    public static List<BuildingKind> kinds() {
        return Collections.unmodifiableList(KINDS);
    }

    public static TemplateStore store() {
        return store;
    }

    public static BuildingKind kind(String id) {
        for (BuildingKind kind : KINDS) if (kind.id.equals(id)) return kind;
        return null;
    }

    public static Template template(BuildingKind kind) {
        return store.get(kind.templateName, kind.blankIsAir);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        TerrainSampler sampler = EndTerrain.sampler(seed);
        StructureProbe probe = sampler.structureProbe(), standing = sampler.standingProbe();
        WorldTarget target = new WorldTarget(world);
        for (BuildingKind kind : KINDS) {
            try {
                Template t = template(kind);
                if (t == null) continue;
                for (Placement p : kind.near(seed, ox, oz, t, probe, standing)) {
                    if (!blocked(generator, world, seed, p, probe)) BuildingWriter.write(p, ox, oz, target);
                }
            } catch (RuntimeException e) {
                // A last guard: buildings must never take End generation down with them.
                if (!warned) ModLog.LOG.warn(
                    "End building {} failed at chunk {},{}; later failures stay quiet",
                    kind.id,
                    chunkX,
                    chunkZ,
                    e);
                warned = true;
            }
        }
    }

    /** The free-standing shapes a building keeps clear of. */
    private static final List<Structure.Kind<? extends Structure>> SHAPES = shapes();

    private static List<Structure.Kind<? extends Structure>> shapes() {
        List<Structure.Kind<? extends Structure>> shapes = new ArrayList<>(Structures.kinds());
        shapes.addAll(
            Arrays.asList(Structures.MUSHROOMS, Structures.RINGLETS, Structures.CROSSES, Tool.KIND, Biosphere.KIND));
        return shapes;
    }

    /**
     * Whether the building gives way to an HEE island, a free-standing shape or a biosphere; the same from every
     * window.
     */
    public static boolean blocked(Object generator, World end, long seed, Placement p, StructureProbe probe) {
        double cx = (p.minX() + p.maxX()) / 2.0, cz = (p.minZ() + p.maxZ()) / 2.0;
        double radius = Distance.planar((p.maxX() - p.minX()) / 2.0, (p.maxZ() - p.minZ()) / 2.0);
        if (p.minY() < Reservations.CLEAR_Y && Reservations.touches(
            Reservations.forChunk(generator, end, seed, (int) Math.floor(cx) >> 4, (int) Math.floor(cz) >> 4),
            cx,
            cz,
            radius)) return true;
        for (Structure.Kind<? extends Structure> kind : SHAPES) if (!kind.near(seed, cx, cz, radius, probe)
            .isEmpty()) return true;
        return false;
    }

    /** A building found by find. */
    public static final class Found {

        public final BuildingKind kind;
        public final Placement placement;

        Found(BuildingKind kind, Placement placement) {
            this.kind = kind;
            this.placement = placement;
        }
    }

    /** The generated building nearest (x, z) within radius, of kind id or of any kind when id is null. */
    public static Found find(Object generator, World end, long seed, int x, int z, String id, int radius) {
        TerrainSampler sampler = EndTerrain.sampler(seed);
        StructureProbe probe = sampler.structureProbe(), standing = sampler.standingProbe();
        Found best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BuildingKind kind : KINDS) {
            if (id != null && !kind.id.equals(id)) continue;
            Template t = template(kind);
            if (t == null) continue;
            int c0x = Math.floorDiv(x - radius, kind.cell), c1x = Math.floorDiv(x + radius, kind.cell);
            int c0z = Math.floorDiv(z - radius, kind.cell), c1z = Math.floorDiv(z + radius, kind.cell);
            for (int cx = c0x; cx <= c1x; cx++) for (int cz = c0z; cz <= c1z; cz++) {
                Placement p = kind.compute(seed, cx, cz, t, probe, standing);
                if (p == null) continue;
                double d = Distance.planar(p.anchorX - x, p.anchorZ - z);
                if (d > radius || d >= bestDistance || blocked(generator, end, seed, p, probe)) continue;
                best = new Found(kind, p);
                bestDistance = d;
            }
        }
        return best;
    }

    private static InputStream resource(String path) {
        return EndBuildings.class.getClassLoader()
            .getResourceAsStream(path);
    }
}
