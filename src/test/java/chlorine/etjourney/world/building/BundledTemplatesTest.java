package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Every building shipped in the jar reads without error. */
class BundledTemplatesTest {

    @Test
    void everyBundledTemplateParses() throws Exception {
        File[] files = new File("src/main/resources/" + TemplateStore.CLASSPATH_DIR)
            .listFiles((d, n) -> n.endsWith(".json"));
        assertFalse(files == null || files.length == 0);
        for (File f : files) try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            TemplateParser.parse(f.getName(), r, true);
        }
    }
}
