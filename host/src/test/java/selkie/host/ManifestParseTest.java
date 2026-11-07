package selkie.host;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Manifest parsing: the fields QML reads, the ", "-joined display text, the official-first
 * ordering, the documented fallbacks, and the shipped data/ fixtures themselves.
 */
class ManifestParseTest {

    @TempDir
    Path dataRoot;

    private Path pluginsDirectory() {
        return dataRoot.resolve("plugins");
    }

    private List<PluginInfo> read() {
        return ManifestReader.read(pluginsDirectory(), message -> { }).plugins();
    }

    @Test
    void readsEveryContractFieldAndJoinsTheLists() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", """
                {
                  "id": "weather-kit",
                  "name": "Weather Kit",
                  "version": "1.4.0",
                  "publisher": "SelkieLab",
                  "description": "Adds a five-day forecast tool.",
                  "official": true,
                  "provides": ["tool", "prompt"],
                  "permissions": ["net:http"],
                  "token_policy": "schema-only"
                }
                """);

        PluginInfo plugin = read().get(0);

        assertEquals("weather-kit", plugin.id);
        assertEquals("Weather Kit", plugin.name);
        assertEquals("1.4.0", plugin.version);
        assertEquals("SelkieLab", plugin.publisher);
        assertEquals("Adds a five-day forecast tool.", plugin.description);
        assertTrue(plugin.official);
        assertEquals("schema-only", plugin.tokenPolicy);
        assertEquals(List.of("tool", "prompt"), plugin.provides);
        assertEquals(List.of("net:http"), plugin.permissions);
        // QML cannot read List<String> reliably: the joined text is the contract.
        assertEquals("tool, prompt", plugin.providesText);
        assertEquals("net:http", plugin.permissionsText);
        // Health is unknown until a backend reports it.
        assertEquals(PluginHealth.UNKNOWN, plugin.health);
        assertEquals(0, plugin.latencyMs);
    }

    @Test
    void officialPluginsComeFirstThenNameOrder() throws IOException {
        Fixtures.writeManifest(dataRoot, "zulu",
                Fixtures.manifest("zulu", "Zulu Tool", false, List.of("tool"), List.of(), "schema-only"));
        Fixtures.writeManifest(dataRoot, "alpha",
                Fixtures.manifest("alpha", "Alpha Tool", true, List.of("tool"), List.of(), "schema-only"));
        Fixtures.writeManifest(dataRoot, "beta",
                Fixtures.manifest("beta", "Beta Tool", false, List.of("tool"), List.of(), "schema-only"));
        Fixtures.writeManifest(dataRoot, "aardvark",
                Fixtures.manifest("aardvark", "aardvark tool", true, List.of("tool"), List.of(), "schema-only"));

        List<String> ids = read().stream().map(plugin -> plugin.id).toList();

        // Official first (and case-insensitively by name inside a group), then third party.
        assertEquals(List.of("aardvark", "alpha", "beta", "zulu"), ids);
    }

    @Test
    void minimalManifestFallsBackOnEveryDocumentedDefault() throws IOException {
        Fixtures.writeManifest(dataRoot, "minimal", "{\"id\": \"minimal\", \"name\": \"Minimal\"}");

        PluginInfo plugin = read().get(0);

        assertEquals(PluginInfo.DEFAULT_VERSION, plugin.version);
        assertEquals("", plugin.publisher);
        assertEquals("", plugin.description);
        assertFalse(plugin.official);
        assertEquals(PluginInfo.SCHEMA_ONLY, plugin.tokenPolicy);
        assertEquals("", plugin.providesText);
        assertEquals("", plugin.permissionsText);
        assertTrue(plugin.provides.isEmpty());
    }

    @Test
    void missingIdFallsBackToTheDirectoryName() throws IOException {
        Fixtures.writeManifest(dataRoot, "no-id", "{\"name\": \"No Id\"}");

        PluginInfo plugin = read().get(0);

        assertEquals("no-id", plugin.id);
        assertEquals("No Id", plugin.name);
    }

    @Test
    void unparseableManifestIsSkippedWithOneWarning() throws IOException {
        Fixtures.writeManifest(dataRoot, "good",
                Fixtures.manifest("good", "Good", true, List.of("tool"), List.of(), "schema-only"));
        Fixtures.writeManifest(dataRoot, "broken", "{ this is not json ");
        Fixtures.writeManifest(dataRoot, "not-an-object", "[]");

        List<String> warnings = new ArrayList<>();
        ManifestReader.Snapshot snapshot = ManifestReader.read(pluginsDirectory(), warnings::add);

        assertEquals(3, snapshot.manifests());
        assertEquals(2, snapshot.skipped());
        assertEquals(1, snapshot.plugins().size());
        assertEquals("good", snapshot.plugins().get(0).id);
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("broken"), warnings.toString());
    }

    @Test
    void missingDirectoryIsAnEmptySnapshotNotAnError() {
        ManifestReader.Snapshot snapshot = ManifestReader.read(pluginsDirectory(), message -> {
            throw new AssertionError("no warning expected for a missing directory: " + message);
        });

        assertTrue(snapshot.plugins().isEmpty());
        assertEquals(0, snapshot.manifests());
    }

    @Test
    void joinIsTheCommaSpaceTheContractFixes() {
        assertEquals("", ManifestReader.join(List.of()));
        assertEquals("tool", ManifestReader.join(List.of("tool")));
        assertEquals("tool, prompt", ManifestReader.join(List.of("tool", "prompt")));
    }

    @Test
    void shippedFixturesSatisfyTheStage4Contract() {
        Path data = Fixtures.projectData();
        assumeTrue(Files.isDirectory(data.resolve("plugins")),
                "no data/plugins next to the build; skipping the fixture contract check");

        List<PluginInfo> plugins = ManifestReader.read(data.resolve("plugins"), message -> {
            throw new AssertionError("shipped manifest is unusable: " + message);
        }).plugins();

        assertTrue(plugins.size() >= 4, "expected at least 4 shipped manifests, got " + plugins.size());
        long official = plugins.stream().filter(plugin -> plugin.official).count();
        long thirdParty = plugins.size() - official;
        assertTrue(official >= 2, "expected at least 2 official plugins, got " + official);
        assertTrue(thirdParty >= 2, "expected at least 2 third-party plugins, got " + thirdParty);

        List<String> ids = plugins.stream().map(plugin -> plugin.id).toList();
        assertEquals(ids.size(), ids.stream().distinct().count(), "duplicate plugin ids in data/: " + ids);

        assertTrue(plugins.stream().anyMatch(plugin -> plugin.permissions.contains("net:http")),
                "the fixtures must include a net:http plugin so the UI can show a permission");
        assertTrue(plugins.stream().anyMatch(PluginInfo::fullBody),
                "the fixtures must include a full-body token policy so the UI can show a difference");
        assertTrue(plugins.stream().allMatch(plugin -> !plugin.name.isBlank() && !plugin.version.isBlank()),
                "every shipped plugin needs a name and a version");

        // The parsed set is already in display order.
        List<PluginInfo> sorted = new ArrayList<>(plugins);
        sorted.sort(ManifestReader.DISPLAY_ORDER);
        assertEquals(sorted, plugins);
    }

    @Test
    void stringFieldsAreNeverNullEvenForAWeirdManifest() throws IOException {
        // Empty strings, wrong types and a missing token_policy must not produce a null
        // field: QML renders null as "undefined", which is worse than an empty string.
        Fixtures.writeManifest(dataRoot, "weird", """
                {"id": "", "name": "", "publisher": 42, "provides": "tool", "official": "yes"}
                """);

        PluginInfo plugin = read().get(0);

        assertEquals("weird", plugin.id);
        assertEquals("weird", plugin.name);
        // A JSON number in a string field degrades to its text form; it is never null.
        assertEquals("42", plugin.publisher);
        assertTrue(plugin.provides.isEmpty(), "a non-array provides must be ignored");
        assertFalse(plugin.official);
    }

    @Test
    void utf8AndLongDescriptionsSurviveTheRoundTrip() throws IOException {
        Path manifest = Fixtures.writeManifest(dataRoot, "utf8",
                Fixtures.manifest("utf8", "Weather Kit — five day", true,
                        List.of("tool"), List.of("net:http"), "schema-only"));
        assertTrue(Files.readString(manifest, StandardCharsets.UTF_8).contains("five day"));

        PluginInfo plugin = read().get(0);
        assertEquals("Weather Kit — five day", plugin.name);
    }
}
