package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/*
 * Every raw ore except Coltan smelts into exactly one ingot. Coltan is split into Tantalum and Niobium by
 * hydrofluoric acid instead, so those two ingots have no ore to smelt from yet.
 */
class IngotTableTest {

    private static final Path ASSETS = Paths.get("src/main/resources/assets/etjourney");

    @Test
    void everyRawOreButColtanSmeltsIntoOneIngot() {
        Set<String> expected = new HashSet<>();
        for (OreBlockTable.Entry e : OreBlockTable.ENTRIES) {
            if (e.dropOreName.startsWith("raw") && !e.name.equals("coltan_ore")) expected.add(e.name);
        }
        Set<String> sources = new HashSet<>();
        for (IngotTable.Entry e : IngotTable.ENTRIES) {
            if (e.ore != null) assertTrue(sources.add(e.ore), "twice: " + e.ore);
        }
        assertEquals(expected, sources);
    }

    @Test
    void tantalumAndNiobiumHaveNoOre() {
        for (IngotTable.Entry e : IngotTable.ENTRIES) {
            boolean fromColtan = e.name.equals("ingot_tantalum") || e.name.equals("ingot_niobium");
            assertEquals(fromColtan, e.ore == null, e.name);
        }
    }

    @Test
    void everyIngotIsListedUnderItsOwnName() {
        Set<String> names = new HashSet<>();
        for (IngotTable.Entry e : IngotTable.ENTRIES) {
            assertTrue(e.oreName.matches("ingot[A-Z]\\w+"), e.oreName);
            assertTrue(names.add(e.oreName), "shared ore name: " + e.oreName);
            assertTrue(e.experience > 0, e.name);
        }
    }

    @Test
    void everyIngotTextureHasAnEntry() throws IOException {
        Set<String> names = new HashSet<>();
        for (IngotTable.Entry e : IngotTable.ENTRIES) assertTrue(names.add(e.name), "twice: " + e.name);
        Set<String> textures = new HashSet<>();
        try (DirectoryStream<Path> pngs = Files.newDirectoryStream(ASSETS.resolve("textures/items"), "ingot_*.png")) {
            for (Path png : pngs) textures.add(png.getFileName().toString().replace(".png", ""));
        }
        assertEquals(textures, names);
    }

    @Test
    void everyTextureIsA16By16Png() throws IOException {
        for (IngotTable.Entry e : IngotTable.ENTRIES) {
            Path png = ASSETS.resolve("textures/items/" + e.name + ".png");
            BufferedImage image = ImageIO.read(png.toFile());
            assertNotNull(image, png.toString());
            assertEquals(16, image.getWidth(), e.name);
            assertEquals(16, image.getHeight(), e.name);
        }
    }

    @Test
    void everyIngotIsNamedInBothLanguages() throws IOException {
        for (String lang : new String[] { "en_US", "ja_JP" }) {
            String text = new String(
                Files.readAllBytes(ASSETS.resolve("lang/" + lang + ".lang")),
                StandardCharsets.UTF_8);
            for (IngotTable.Entry e : IngotTable.ENTRIES) {
                assertTrue(text.contains("item.etjourney." + e.name + ".name="), lang + ": " + e.name);
            }
        }
    }
}
