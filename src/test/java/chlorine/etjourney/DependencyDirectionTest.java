package chlorine.etjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Keeps references flowing core <- world <- content <- compat so core stays extractable. */
class DependencyDirectionTest {

    private static final Map<String, Set<String>> ALLOWED = new HashMap<>();

    static {
        ALLOWED.put("core", layers("core"));
        ALLOWED.put("world", layers("core", "world"));
        ALLOWED.put("content", layers("core", "world", "content"));
        ALLOWED.put("compat", layers("core", "world", "content", "compat"));
    }

    private static final Pattern REFERENCE = Pattern.compile("chlorine\\.etjourney\\.(\\w+|\\*)");
    private static final Path ROOT_PACKAGE = Paths.get("chlorine", "etjourney");

    static List<String> violations(Path sourceRoot) throws IOException {
        Path base = sourceRoot.resolve(ROOT_PACKAGE);
        List<String> found = new ArrayList<>();
        if (!Files.isDirectory(base)) return found;
        List<Path> sources;
        try (Stream<Path> files = Files.walk(base)) {
            sources = files.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .sorted()
                .collect(Collectors.toList());
        }
        for (Path file : sources) {
            Path relative = base.relativize(file);
            if (relative.getNameCount() < 2) continue;
            Set<String> allowed = ALLOWED.get(
                relative.getName(0)
                    .toString());
            if (allowed == null) continue;
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher m = REFERENCE.matcher(source.replaceFirst("(?m)^package\\s+[\\w.]+;", ""));
            Set<String> reported = new HashSet<>();
            while (m.find()) {
                String target = m.group(1);
                if (!allowed.contains(target) && reported.add(target)) {
                    found.add(
                        relative.toString()
                            .replace('\\', '/') + " -> "
                            + target);
                }
            }
        }
        return found;
    }

    private static Set<String> layers(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test
    void mainSourcesFollowTheLayerRules() throws IOException {
        assertEquals(Collections.emptyList(), violations(Paths.get("src", "main", "java")));
    }

    @Test
    void coreImportingContentIsReported(@TempDir Path root) throws IOException {
        write(root, "core/Foo.java", "package chlorine.etjourney.core;\nimport chlorine.etjourney.content.Bar;\n");
        assertEquals(Collections.singletonList("core/Foo.java -> content"), violations(root));
    }

    @Test
    void wildcardImportOfTheRootPackageIsReported(@TempDir Path root) throws IOException {
        write(root, "core/Foo.java", "package chlorine.etjourney.core;\nimport chlorine.etjourney.*;\n");
        assertEquals(Collections.singletonList("core/Foo.java -> *"), violations(root));
    }

    @Test
    void fullyQualifiedReferenceIsReported(@TempDir Path root) throws IOException {
        write(
            root,
            "world/Gen.java",
            "package chlorine.etjourney.world;\nclass Gen { chlorine.etjourney.compat.X x; }\n");
        assertEquals(Collections.singletonList("world/Gen.java -> compat"), violations(root));
    }

    @Test
    void layerReferencingRootClassIsReported(@TempDir Path root) throws IOException {
        write(root, "core/Foo.java", "package chlorine.etjourney.core;\nimport chlorine.etjourney.Tags;\n");
        assertEquals(Collections.singletonList("core/Foo.java -> Tags"), violations(root));
    }

    @Test
    void downwardReferencesAndRootFilesAreAllowed(@TempDir Path root) throws IOException {
        write(
            root,
            "compat/A.java",
            "package chlorine.etjourney.compat;\nimport chlorine.etjourney.core.ModInfo;\nimport chlorine.etjourney.content.ModContent;\n");
        write(root, "ETJourney.java", "package chlorine.etjourney;\nimport chlorine.etjourney.compat.A;\n");
        write(root, "proxy/P.java", "package chlorine.etjourney.proxy;\nimport chlorine.etjourney.world.ModWorld;\n");
        assertTrue(violations(root).isEmpty());
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(ROOT_PACKAGE)
            .resolve(Paths.get("", relative.split("/")));
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }
}
