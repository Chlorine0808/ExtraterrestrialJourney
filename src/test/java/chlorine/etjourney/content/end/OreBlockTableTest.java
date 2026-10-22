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
import java.util.EnumSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Ore;
import chlorine.etjourney.world.end.modifier.EndBlock;

class OreBlockTableTest {

    private static final Path ASSETS = Paths.get("src/main/resources/assets/etjourney");

    @Test
    void coversEveryOreOnce() {
        Set<EndBlock> expected = EnumSet.noneOf(EndBlock.class);
        for (Ore ore : Ore.values()) expected.add(ore.block);
        Set<EndBlock> bound = EnumSet.noneOf(EndBlock.class);
        for (OreBlockTable.Entry e : OreBlockTable.ENTRIES) assertTrue(bound.add(e.kind), "twice: " + e.kind);
        assertEquals(expected, bound);
    }

    @Test
    void everyTextureIsA16By16Png() throws IOException {
        for (OreBlockTable.Entry e : OreBlockTable.ENTRIES) {
            Path png = ASSETS.resolve("textures/blocks/" + e.name + ".png");
            BufferedImage image = ImageIO.read(png.toFile());
            assertNotNull(image, png.toString());
            assertEquals(16, image.getWidth(), e.name);
            assertEquals(16, image.getHeight(), e.name);
        }
    }

    @Test
    void everyBlockIsNamedInBothLanguages() throws IOException {
        for (String lang : new String[] { "en_US", "ja_JP" }) {
            String text = new String(
                Files.readAllBytes(ASSETS.resolve("lang/" + lang + ".lang")),
                StandardCharsets.UTF_8);
            for (OreBlockTable.Entry e : OreBlockTable.ENTRIES) {
                assertTrue(text.contains("tile.etjourney." + e.name + ".name="), lang + ": " + e.name);
            }
        }
    }
}
