package selkie.host;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads data/plugins/&lt;id&gt;/manifest.json into {@link PluginInfo} values.
 *
 * <p>Contract: one directory per plugin, the manifest is always named manifest.json. A
 * directory without a manifest is ignored; a manifest that does not parse costs a warning
 * and is skipped (one bad community manifest must not hide the rest of the list).
 *
 * <p>Every field except id/name has a documented fallback, so a minimal manifest such as
 * {"id":"x","name":"X"} is valid.
 */
final class ManifestReader {

    /** Fixed file name inside each plugin directory. */
    static final String FILE_NAME = "manifest.json";

    /** Official first, then case-insensitive name, then id - the order QML shows. */
    static final Comparator<PluginInfo> DISPLAY_ORDER = Comparator
            .comparing((PluginInfo plugin) -> !plugin.official)
            .thenComparing(plugin -> plugin.name.toLowerCase(Locale.ROOT))
            .thenComparing(plugin -> plugin.id);

    private ManifestReader() {
    }

    /**
     * One directory scan.
     *
     * @param plugins   parsed manifests, already in display order
     * @param manifests how many manifest.json files were found on disk
     * @param skipped   how many of those could not be parsed
     */
    record Snapshot(List<PluginInfo> plugins, int manifests, int skipped) {

        static final Snapshot EMPTY = new Snapshot(List.of(), 0, 0);
    }

    /**
     * Scans pluginsDirectory and parses every manifest it finds.
     *
     * @param warn sink for one human-readable line per unusable manifest (never throws)
     */
    static Snapshot read(Path pluginsDirectory, Consumer<String> warn) {
        if (!Files.isDirectory(pluginsDirectory)) {
            return Snapshot.EMPTY;
        }
        List<Path> directories = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(pluginsDirectory)) {
            for (Path child : children) {
                if (Files.isRegularFile(child.resolve(FILE_NAME))) {
                    directories.add(child);
                }
            }
        } catch (IOException | RuntimeException error) {
            warn.accept("[data] cannot list " + pluginsDirectory + ": " + describe(error));
            return Snapshot.EMPTY;
        }
        directories.sort(Comparator.comparing(directory -> directory.getFileName().toString()));

        List<PluginInfo> parsed = new ArrayList<>(directories.size());
        int skipped = 0;
        for (Path directory : directories) {
            Path manifest = directory.resolve(FILE_NAME);
            try {
                parsed.add(parse(Files.readString(manifest, StandardCharsets.UTF_8),
                        directory.getFileName().toString()));
            } catch (IOException | RuntimeException error) {
                skipped++;
                warn.accept("[data] skipping " + manifest + ": " + describe(error));
            }
        }
        parsed.sort(DISPLAY_ORDER);
        return new Snapshot(List.copyOf(parsed), directories.size(), skipped);
    }

    /**
     * Parses one manifest.
     *
     * @param directoryName fallback id when the manifest omits id (the directory name is the
     *                      plugin identity on disk)
     */
    static PluginInfo parse(String json, String directoryName) throws IOException {
        JsonNode root = Json.MAPPER.readTree(json);
        if (root == null || !root.isObject()) {
            throw new IOException("manifest is not a JSON object");
        }
        PluginInfo plugin = new PluginInfo();
        plugin.id = text(root, "id", directoryName);
        plugin.name = text(root, "name", plugin.id);
        plugin.version = text(root, "version", PluginInfo.DEFAULT_VERSION);
        plugin.publisher = text(root, "publisher", "");
        plugin.description = text(root, "description", "");
        plugin.tokenPolicy = text(root, "token_policy", PluginInfo.SCHEMA_ONLY);
        plugin.official = root.path("official").asBoolean(false);
        plugin.provides = strings(root.get("provides"));
        plugin.permissions = strings(root.get("permissions"));
        plugin.providesText = join(plugin.provides);
        plugin.permissionsText = join(plugin.permissions);
        return plugin;
    }

    /** ["tool","prompt"] becomes "tool, prompt" - exactly what QML displays. */
    static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(", ", values);
    }

    private static String text(JsonNode root, String field, String fallback) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.isContainerNode()) {
            return fallback;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? fallback : value;
    }

    private static List<String> strings(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>(array.size());
        for (JsonNode element : array) {
            if (element == null || element.isContainerNode()) {
                continue;
            }
            String value = element.asText("").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
