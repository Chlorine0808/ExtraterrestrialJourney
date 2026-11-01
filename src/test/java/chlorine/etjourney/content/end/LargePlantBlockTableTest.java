package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.modifier.EndBlock;

class LargePlantBlockTableTest {

    private static final Path ASSETS = Paths.get("src/main/resources/assets/etjourney");

    @Test
    void coversEveryLargePlantKindOnce() {
        // The kinds LargeFlora's growers and the giant tree can write, plus the crystals and their buds.
        Set<EndBlock> expected = EnumSet.of(
            EndBlock.GROVE_LOG,
            EndBlock.GROVE_LEAVES,
            EndBlock.GROVE_BLOSSOM,
            EndBlock.HIGHLAND_LOG,
            EndBlock.HIGHLAND_LEAVES,
            EndBlock.MYCELIA_STEM,
            EndBlock.MYCELIA_CAP,
            EndBlock.MYCELIA_GILL,
            EndBlock.MYCELIA_VINE,
            EndBlock.MYCELIA_VINE_LAST,
            EndBlock.AMBER_ROOT,
            EndBlock.AMBER_SEED,
            EndBlock.AMBER_STEM,
            EndBlock.CRYSTAL,
            EndBlock.BUDDING_CRYSTAL,
            EndBlock.CRYSTAL_BUD_SMALL,
            EndBlock.CRYSTAL_BUD_MEDIUM,
            EndBlock.CRYSTAL_BUD_LARGE,
            EndBlock.CRYSTAL_CLUSTER);
        Set<EndBlock> bound = EnumSet.noneOf(EndBlock.class);
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            assertTrue(bound.add(e.kind), "twice: " + e.kind);
        }
        assertEquals(expected, bound);
    }

    @Test
    void furHangsAndGlows() {
        int found = 0;
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            if (e.kind != EndBlock.MYCELIA_VINE && e.kind != EndBlock.MYCELIA_VINE_LAST) continue;
            assertEquals(LargePlantBlockTable.Type.HANGING, e.type, e.name);
            assertEquals(10, e.light, e.name);
            found++;
        }
        assertEquals(2, found);
    }

    @Test
    void saplingsAreOnePerBiomeWithADistinctName() {
        Set<String> names = new HashSet<>();
        Set<EndBiome> biomes = EnumSet.noneOf(EndBiome.class);
        for (LargePlantBlockTable.Sapling s : LargePlantBlockTable.SAPLINGS) {
            assertTrue(names.add(s.name), "twice: " + s.name);
            assertTrue(biomes.add(s.biome), "twice: " + s.biome);
        }
        assertEquals(2, LargePlantBlockTable.SAPLINGS.size());
    }

    /** Logs, planks and leaves join the ore dictionary and catch fire with the vanilla wood's spread and burn rates. */
    @Test
    void woodIsSharedAndBurnsLikeVanilla() {
        Set<String> logs = new HashSet<>(), leaves = new HashSet<>();
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            LargePlantBlockTable.Type t = e.type;
            if ("logWood".equals(t.oreName)) {
                logs.add(e.name);
                assertEquals(5, t.fireSpread);
                assertEquals(5, t.flammability);
            } else if ("treeLeaves".equals(t.oreName)) {
                leaves.add(e.name);
                assertEquals(30, t.fireSpread);
                assertEquals(60, t.flammability);
            } else {
                assertEquals(null, t.oreName, e.name);
                assertEquals(0, t.flammability, e.name);
            }
        }
        assertEquals(new HashSet<>(Arrays.asList("grove_log", "highland_log", "mycelia_stem", "amber_root")), logs);
        assertEquals(new HashSet<>(Arrays.asList("grove_leaves", "grove_blossom", "highland_leaves")), leaves);
        assertEquals("plankWood", LargePlantBlockTable.PLANKS_ORE);
        assertEquals(5, LargePlantBlockTable.PLANKS_FIRE_SPREAD);
        assertEquals(20, LargePlantBlockTable.PLANKS_FLAMMABILITY);
    }

    @Test
    void lightStaysWithinTheBlockRange() {
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            assertTrue(e.light >= 0 && e.light <= 15, e.name);
        }
    }

    @Test
    void everyTextureIsA16By16Png() throws IOException {
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            if (e.type == LargePlantBlockTable.Type.LOG) {
                check("textures/blocks/" + e.name + "_side.png", false);
                check("textures/blocks/" + e.name + "_top.png", false);
            } else {
                // Buds and fur are drawn crossed, so their corners must be clear.
                check(
                    "textures/blocks/" + e.name + ".png",
                    e.type == LargePlantBlockTable.Type.BUD || e.type == LargePlantBlockTable.Type.HANGING);
            }
        }
        for (LargePlantBlockTable.Wood w : LargePlantBlockTable.WOODS)
            check("textures/blocks/" + w.planks + ".png", false);
        // Saplings are drawn crossed, so their corners must be clear.
        for (LargePlantBlockTable.Sapling s : LargePlantBlockTable.SAPLINGS)
            check("textures/blocks/" + s.name + ".png", true);
        check("textures/items/" + LargePlantBlockTable.SHARD + ".png", false);
    }

    private static void check(String path, boolean clearCorner) throws IOException {
        Path png = ASSETS.resolve(path);
        BufferedImage image = ImageIO.read(png.toFile());
        assertNotNull(image, png.toString());
        assertEquals(16, image.getWidth(), path);
        assertEquals(16, image.getHeight(), path);
        if (clearCorner) assertEquals(0, image.getRGB(0, 0) >>> 24, path);
    }

    @Test
    void everyBlockIsNamedInBothLanguages() throws IOException {
        List<String> keys = new ArrayList<>();
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES)
            keys.add("tile.etjourney." + e.name + ".name=");
        for (LargePlantBlockTable.Wood w : LargePlantBlockTable.WOODS)
            keys.add("tile.etjourney." + w.planks + ".name=");
        for (LargePlantBlockTable.Sapling s : LargePlantBlockTable.SAPLINGS)
            keys.add("tile.etjourney." + s.name + ".name=");
        keys.add("item.etjourney." + LargePlantBlockTable.SHARD + ".name=");
        for (String lang : new String[] { "en_US", "ja_JP" }) {
            String text = new String(
                Files.readAllBytes(ASSETS.resolve("lang/" + lang + ".lang")),
                StandardCharsets.UTF_8);
            for (String key : keys) assertTrue(text.contains(key), lang + ": " + key);
        }
    }
}
