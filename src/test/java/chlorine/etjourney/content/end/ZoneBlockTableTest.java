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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.modifier.EndBlock;

class ZoneBlockTableTest {

    private static final Path ASSETS = Paths.get("src/main/resources/assets/etjourney");

    @Test
    void coversEveryZoneTopAndFillOnly() {
        Set<EndBlock> expected = EnumSet.noneOf(EndBlock.class);
        for (Zone zone : Zone.values()) {
            expected.add(zone.top);
            expected.add(zone.fill);
        }
        Set<EndBlock> bound = EnumSet.noneOf(EndBlock.class);
        for (ZoneBlockTable.Entry e : ZoneBlockTable.ENTRIES) assertTrue(bound.add(e.kind), "twice: " + e.kind);
        assertEquals(expected, bound);
    }

    @Test
    void surfacesUseARockOfTheTable() {
        List<String> rocks = new ArrayList<>();
        for (ZoneBlockTable.Entry e : ZoneBlockTable.ENTRIES) if (e.bottom == null) rocks.add(e.name);
        for (ZoneBlockTable.Entry e : ZoneBlockTable.ENTRIES) {
            if (e.bottom != null) assertTrue(rocks.contains(e.bottom), e.name + " -> " + e.bottom);
        }
    }

    @Test
    void everyTextureIsA16By16Png() throws IOException {
        for (ZoneBlockTable.Entry e : ZoneBlockTable.ENTRIES) {
            for (String file : e.bottom == null ? new String[] { e.name }
                : new String[] { e.name + "_top", e.name + "_side" }) {
                Path png = ASSETS.resolve("textures/blocks/" + file + ".png");
                BufferedImage image = ImageIO.read(png.toFile());
                assertNotNull(image, png.toString());
                assertEquals(16, image.getWidth(), file);
                assertEquals(16, image.getHeight(), file);
            }
        }
    }

    @Test
    void everyBlockIsNamedInBothLanguages() throws IOException {
        for (String lang : new String[] { "en_US", "ja_JP" }) {
            String text = new String(
                Files.readAllBytes(ASSETS.resolve("lang/" + lang + ".lang")),
                StandardCharsets.UTF_8);
            for (ZoneBlockTable.Entry e : ZoneBlockTable.ENTRIES) {
                assertTrue(text.contains("tile.etjourney." + e.name + ".name="), lang + ": " + e.name);
            }
        }
    }
}
