package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Finding templates in OKModular's scan folder first, then in the jar. */
class TemplateStoreTest {

    @TempDir
    File dir;

    private static String json(int width) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < width; i++) row.append('A');
        return "[{\"layers\":[{\"rows\":[\"" + row + "\"]}],\"mappings\":{\"A\":\"m:a\"}}]";
    }

    private void write(String name, String content) throws IOException {
        Files.write(new File(dir, name + ".json").toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static Function<String, InputStream> jar(Map<String, String> files) {
        return path -> {
            String content = files.get(path);
            return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
    }

    @Test
    void okmodularDirWinsOverClasspath() throws Exception {
        write("a", json(2));
        Map<String, String> files = new HashMap<>();
        files.put(TemplateStore.CLASSPATH_DIR + "a.json", json(3));
        assertEquals(2, new TemplateStore(dir, jar(files)).get("a", true).sizeX);
    }

    @Test
    void fallsBackToClasspath() {
        Map<String, String> files = new HashMap<>();
        files.put(TemplateStore.CLASSPATH_DIR + "a.json", json(3));
        assertEquals(3, new TemplateStore(dir, jar(files)).get("a", true).sizeX);
        assertEquals(3, new TemplateStore(null, jar(files)).get("a", true).sizeX);
    }

    @Test
    void skipsBrokenFileAndKeepsOthers() throws Exception {
        write("a", "[{");
        write("b", json(1));
        TemplateStore store = new TemplateStore(dir, jar(new HashMap<>()));
        assertNull(store.get("a", true));
        assertTrue(
            store.failures()
                .containsKey("a"));
        assertNotNull(store.get("b", true));
        assertNull(store.get("missing", true));
        assertTrue(
            store.failures()
                .containsKey("missing"));
    }

    @Test
    void blankSettingIsPartOfTheKey() throws Exception {
        write("a", "[{\"layers\":[{\"rows\":[\" \"]}],\"mappings\":{}}]");
        TemplateStore store = new TemplateStore(dir, jar(new HashMap<>()));
        assertTrue(
            store.get("a", true)
                .get(0, 0, 0)
                .isAir());
        assertTrue(
            store.get("a", false)
                .get(0, 0, 0)
                .isKeep());
    }

    @Test
    void clearRereadsFromDisk() throws Exception {
        write("a", json(2));
        TemplateStore store = new TemplateStore(dir, jar(new HashMap<>()));
        assertEquals(2, store.get("a", true).sizeX);
        write("a", json(5));
        assertEquals(2, store.get("a", true).sizeX);
        store.clear();
        assertEquals(5, store.get("a", true).sizeX);
    }
}
