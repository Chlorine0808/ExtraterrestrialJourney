package chlorine.etjourney.world.end.biosphere;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.BiomeBlacklist;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/** The biomes the config keeps out of biospheres, by biome ID, worked out once all biomes are registered. */
final class BiomeExclusions {

    private static boolean[] table;
    private static final Set<BiosphereSource.Kind> WARNED = EnumSet.noneOf(BiosphereSource.Kind.class);

    private BiomeExclusions() {}

    /** Excluded flags by biome ID; null when nothing is excluded. */
    static synchronized boolean[] table() {
        if (table == null) table = build();
        return table.length == 0 ? null : table;
    }

    /**
     * Whether a ball of the given radius cut around (x, z) in the world holds no excluded biome. A biome map that
     * fails to answer allows the point: the cut-out that follows reports the failure and falls back on its own.
     */
    static boolean allowed(World w, int x, int z, double radius) {
        boolean[] excluded = table();
        if (excluded == null) return true;
        int r = (int) Math.ceil(radius), size = 2 * r + 1;
        int[] ids = new int[size * size];
        try {
            BiomeGenBase[] area = w.getWorldChunkManager()
                .getBiomeGenAt(null, x - r, z - r, size, size, false);
            for (int i = 0; i < ids.length; i++) ids[i] = area[i] == null ? -1 : area[i].biomeID;
        } catch (Throwable t) {
            SampleWorlds.rethrowFatal(t);
            return true;
        }
        return BiosphereSource.clear(ids, size, radius, id -> id >= 0 && id < excluded.length && excluded[id]);
    }

    /** Whether the config keeps the biome with this ID out of biospheres. */
    static boolean excludes(int id) {
        boolean[] excluded = table();
        return excluded != null && id >= 0 && id < excluded.length && excluded[id];
    }

    /** Logs once per kind, while the server runs, that the blacklist left a ball no point in that world. */
    static synchronized void noPoint(BiosphereSource.Kind kind) {
        if (WARNED.add(kind)) ModLog.LOG.warn(
            "Every candidate point for a {} biosphere holds a biome kept out by excludedBiomes or excludedMods;"
                + " such balls are cut from the other world or left out",
            kind);
    }

    private static boolean[] build() {
        BiomeBlacklist list = new BiomeBlacklist(ETJConfig.excludedBiomes, ETJConfig.excludedMods);
        if (list.isEmpty()) return new boolean[0];
        Map<String, String> owners = owners();
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        boolean[] out = new boolean[biomes.length];
        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (BiomeGenBase b : biomes) {
            if (b == null || b.biomeID < 0 || b.biomeID >= out.length) continue;
            ids.add(b.biomeID);
            names.add(b.biomeName);
            out[b.biomeID] = list.excludes(b.biomeID, b.biomeName, owner(owners, b));
        }
        Set<String> mods = new HashSet<>(owners.values());
        mods.add("minecraft");
        for (ModContainer mod : Loader.instance()
            .getActiveModList()) mods.add(mod.getModId());
        for (String entry : list.unmatched(ids, names, mods))
            ModLog.LOG.warn("Biosphere blacklist: {} matches nothing loaded and is ignored", entry);
        List<String> kept = new ArrayList<>();
        for (BiomeGenBase b : biomes) if (b != null && b.biomeID >= 0 && b.biomeID < out.length && out[b.biomeID])
            kept.add(b.biomeID + " " + b.biomeName);
        ModLog.LOG.info("Biosphere blacklist keeps {} biomes out: {}", kept.size(), kept);
        return out;
    }

    /** Mod ID by package, from the packages each loaded mod's files hold. */
    private static Map<String, String> owners() {
        Map<String, String> out = new HashMap<>();
        for (ModContainer mod : Loader.instance()
            .getActiveModList()) {
            try {
                for (String pkg : mod.getOwnedPackages()) out.putIfAbsent(pkg.replace('/', '.'), mod.getModId());
            } catch (Throwable t) {
                // A container that cannot list its packages owns none here.
                SampleWorlds.rethrowFatal(t);
            }
        }
        return out;
    }

    /** The mod a biome's class belongs to: "minecraft" for vanilla, null when no loaded mod owns its package. */
    private static String owner(Map<String, String> owners, BiomeGenBase b) {
        String name = b.getClass()
            .getName();
        if (name.startsWith("net.minecraft.")) return "minecraft";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : owners.get(name.substring(0, dot));
    }
}
