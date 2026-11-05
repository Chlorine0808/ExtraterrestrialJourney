package chlorine.etjourney.world.building;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import chlorine.etjourney.core.util.ModLog;

/**
 * Templates by file name: OKModular's scan folder first, so a fresh scan can be tried without copying it, then the
 * jar. Results, failures included, are kept until clear.
 */
public final class TemplateStore {

    public static final String CLASSPATH_DIR = "assets/etjourney/buildings/";

    private final File okmodularDir;
    private final Function<String, InputStream> classpath;
    private final Map<String, Optional<Template>> cache = new ConcurrentHashMap<>();
    private final Map<String, String> failures = new ConcurrentHashMap<>();

    public TemplateStore(File okmodularDir, Function<String, InputStream> classpath) {
        this.okmodularDir = okmodularDir;
        this.classpath = classpath;
    }

    /** The template, or null when it is missing or broken; the reason is logged once and kept in failures. */
    public Template get(String name, boolean blankIsAir) {
        return cache
            .computeIfAbsent(name + (blankIsAir ? "|air" : "|keep"), k -> Optional.ofNullable(load(name, blankIsAir)))
            .orElse(null);
    }

    public void clear() {
        cache.clear();
        failures.clear();
    }

    public Map<String, String> failures() {
        return Collections.unmodifiableMap(failures);
    }

    private Template load(String name, boolean blankIsAir) {
        try (InputStream in = open(name)) {
            if (in == null) return fail(name, "no file");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return TemplateParser.parse(name, reader, blankIsAir);
            }
        } catch (TemplateException | IOException e) {
            return fail(name, e.getMessage());
        }
    }

    private InputStream open(String name) throws IOException {
        if (okmodularDir != null) {
            File file = new File(okmodularDir, name + ".json");
            if (file.isFile()) return new FileInputStream(file);
        }
        return classpath.apply(CLASSPATH_DIR + name + ".json");
    }

    private Template fail(String name, String reason) {
        if (failures.put(name, reason) == null)
            ModLog.LOG.warn("Building template {} could not be read: {}", name, reason);
        return null;
    }
}
