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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Imports are used properly: no fully-qualified names in code bodies, no unused imports, and the pure End terrain
 * packages stay free of Minecraft, FML and Forge so their maths is testable without the game.
 */
class ImportHygieneTest {

    private static final List<String> PURE = Arrays.asList(
        "world/end/noise/",
        "world/end/region/",
        "world/end/modifier/",
        "world/end/feature/",
        "world/end/reserve/");
    private static final List<String> GAME = Arrays.asList("net.minecraft.", "cpw.mods.", "net.minecraftforge.");
    private static final Pattern IMPORT = Pattern
        .compile("^import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?;", Pattern.MULTILINE);
    /** A dotted lower-case package path followed by a capitalised type, such as java.util.List. */
    private static final Pattern QUALIFIED = Pattern
        .compile("\\b(?:java|javax|net|cpw|org|com|chlorine)(?:\\.[a-z_]\\w*)+\\.[A-Z]\\w*");

    static List<String> violations(Path sourceRoot) throws IOException {
        List<String> found = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            files = walk.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .sorted()
                .collect(Collectors.toList());
        }
        for (Path file : files) {
            String rel = sourceRoot.relativize(file)
                .toString()
                .replace('\\', '/');
            String body = stripCommentsAndStrings(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            String code = body.replaceAll("(?m)^\\s*(package|import)\\s[^;]*;", "");
            Matcher qualified = QUALIFIED.matcher(code);
            while (qualified.find()) found.add(rel + ": fully-qualified name " + qualified.group());
            Matcher imports = IMPORT.matcher(body);
            while (imports.find()) {
                String name = imports.group(2);
                boolean isStatic = imports.group(1) != null;
                if (imports.group(3) == null) {
                    String simple = name.substring(name.lastIndexOf('.') + 1);
                    if (!Pattern.compile("\\b" + Pattern.quote(simple) + "\\b")
                        .matcher(code)
                        .find()) found.add(rel + ": unused import " + name);
                }
                if (!isStatic && PURE.stream()
                    .anyMatch(rel::contains)
                    && GAME.stream()
                        .anyMatch(name::startsWith))
                    found.add(rel + ": game import " + name);
            }
        }
        return found;
    }

    private static String stripCommentsAndStrings(String text) {
        return text.replaceAll("(?s)/\\*.*?\\*/", " ")
            .replaceAll("//[^\\n]*", " ")
            .replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    @Test
    void mainSourcesAreClean() throws IOException {
        List<String> found = violations(Paths.get("src/main/java"));
        assertTrue(found.isEmpty(), String.join("\n", found));
    }

    @Test
    void testSourcesAreClean() throws IOException {
        List<String> found = violations(Paths.get("src/test/java"));
        assertTrue(found.isEmpty(), String.join("\n", found));
    }

    @Test
    void catchesAllThreeKinds(@TempDir Path dir) throws IOException {
        Path bad = dir.resolve("chlorine/etjourney/world/end/noise/Bad.java");
        Files.createDirectories(bad.getParent());
        Files.write(
            bad,
            ("package x;\nimport java.util.Map;\nimport net.minecraft.world.World;\n"
                + "class Bad { java.util.List<String> a; World w; }\n").getBytes(StandardCharsets.UTF_8));
        List<String> found = violations(dir);
        assertEquals(3, found.size(), String.join("\n", found));
    }
}
