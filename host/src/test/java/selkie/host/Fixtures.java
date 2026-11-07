package selkie.host;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Test fixtures for the Stage-4 data layer: writes real files into a temp "data/" tree,
 * exactly the shapes the shipped fixtures use.
 */
final class Fixtures {

    static final String TOKENS = "tokens.json";

    private Fixtures() {
    }

    /** Writes {@code <data>/plugins/<directory>/manifest.json} and returns the manifest path. */
    static Path writeManifest(Path dataRoot, String directory, String json) throws IOException {
        Path directoryPath = dataRoot.resolve("plugins").resolve(directory);
        Files.createDirectories(directoryPath);
        Path manifest = directoryPath.resolve(ManifestReader.FILE_NAME);
        Files.writeString(manifest, json, StandardCharsets.UTF_8);
        return manifest;
    }

    /** Builds a complete manifest so tests do not have to hand-write JSON. */
    static String manifest(String id, String name, boolean official, List<String> provides,
                           List<String> permissions, String tokenPolicy) {
        return "{"
                + "\"id\": \"" + id + "\", "
                + "\"name\": \"" + name + "\", "
                + "\"version\": \"1.0.0\", "
                + "\"publisher\": \"Testworks\", "
                + "\"description\": \"Fixture for " + name + ".\", "
                + "\"official\": " + official + ", "
                + "\"provides\": " + array(provides) + ", "
                + "\"permissions\": " + array(permissions) + ", "
                + "\"token_policy\": \"" + tokenPolicy + "\""
                + "}";
    }

    /** Writes {@code <data>/tokens.json}. */
    static void writeTokens(Path dataRoot, int used, int total, int persona, int recall,
                            int tools, int history) throws IOException {
        Files.createDirectories(dataRoot);
        Files.writeString(dataRoot.resolve(TOKENS), "{"
                + "\"used\": " + used + ", \"total\": " + total
                + ", \"persona\": " + persona + ", \"recall\": " + recall
                + ", \"tools\": " + tools + ", \"history\": " + history + "}", StandardCharsets.UTF_8);
    }

    /**
     * Pushes the mtime 5 s into the future. On a coarse timestamp clock (or when two writes
     * land inside the same millisecond) a plain rewrite might not change
     * {@link DataStamp#newest()}, and the test would be flaky rather than wrong.
     */
    static void touch(Path file) throws IOException {
        FileTime current = Files.getLastModifiedTime(file);
        Files.setLastModifiedTime(file, FileTime.fromMillis(current.toMillis() + 5_000L));
    }

    /** The repository's own data/ directory (tests run with the project directory as cwd). */
    static Path projectData() {
        return Path.of(DataPaths.DEFAULT_DIRECTORY);
    }

    private static String array(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append('"').append(values.get(i)).append('"');
        }
        return json.append(']').toString();
    }
}
