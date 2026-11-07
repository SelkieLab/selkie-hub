package selkie.host;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hot-reload contract, without a window: {@code tick()} re-reads only when the
 * fingerprint of the files on disk changed, every re-read bumps {@code revision}, and a
 * half-written manifest never flashes an empty plugin list into the UI.
 */
class HotReloadTest {

    @TempDir
    Path dataRoot;

    private Path manifestOf(String directory) {
        return dataRoot.resolve("plugins").resolve(directory).resolve(ManifestReader.FILE_NAME);
    }

    private static final String WEATHER_KIT = Fixtures.manifest(
            "weather-kit", "Weather Kit", true, List.of("tool", "prompt"), List.of("net:http"), "schema-only");

    @Test
    void editingAManifestShowsUpOnTheNextTick() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        List<String> log = new ArrayList<>();
        PluginBridge bridge = new PluginBridge(dataRoot, log::add);

        assertEquals(1, bridge.count.peek().intValue());
        assertEquals(1L, bridge.revision.peek().longValue());
        assertEquals("Weather Kit", bridge.items.peek().get(0).name);
        assertTrue(log.get(0).contains("revision=1"), log.toString());

        Files.writeString(manifestOf("weather-kit"),
                Fixtures.manifest("weather-kit", "Weather Kit Plus", true,
                        List.of("tool", "prompt"), List.of("net:http"), "schema-only"),
                StandardCharsets.UTF_8);
        Fixtures.touch(manifestOf("weather-kit"));

        bridge.tick();

        assertEquals(2L, bridge.revision.peek().longValue());
        assertEquals("Weather Kit Plus", bridge.items.peek().get(0).name);
        assertTrue(log.stream().anyMatch(line -> line.contains("plugins revision=2")), log.toString());
    }

    @Test
    void untouchedTreeDoesNotRebumpTheRevision() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        PluginBridge bridge = new PluginBridge(dataRoot, message -> { });

        for (int frame = 0; frame < 20; frame++) {
            bridge.tick();
        }

        assertEquals(1L, bridge.revision.peek().longValue(),
                "tick() must not re-read (and re-publish) an unchanged tree");
    }

    @Test
    void addingAPluginDirectoryIsPickedUp() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        PluginBridge bridge = new PluginBridge(dataRoot, message -> { });
        assertEquals(1, bridge.count.peek().intValue());

        Fixtures.writeManifest(dataRoot, "theme-lab",
                Fixtures.manifest("theme-lab", "Theme Lab", false, List.of("theme"), List.of(), "schema-only"));

        bridge.tick();

        assertEquals(2, bridge.count.peek().intValue());
        assertEquals(1, bridge.officialItems.peek().size());
        assertEquals(1, bridge.thirdPartyItems.peek().size());
        assertEquals(2L, bridge.revision.peek().longValue());
    }

    @Test
    void removingAPluginDirectoryIsPickedUp() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        Fixtures.writeManifest(dataRoot, "theme-lab",
                Fixtures.manifest("theme-lab", "Theme Lab", false, List.of("theme"), List.of(), "schema-only"));
        PluginBridge bridge = new PluginBridge(dataRoot, message -> { });
        assertEquals(2, bridge.count.peek().intValue());

        Files.delete(manifestOf("theme-lab"));

        bridge.tick();

        assertEquals(1, bridge.count.peek().intValue());
        assertEquals("weather-kit", bridge.items.peek().get(0).id);
    }

    @Test
    void reloadForcesAReadEvenWithoutAnMtimeChange() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        PluginBridge bridge = new PluginBridge(dataRoot, message -> { });

        bridge.reload();

        assertEquals(2L, bridge.revision.peek().longValue(),
                "reload() is the explicit refresh path and always re-reads");
    }

    @Test
    void halfWrittenManifestKeepsTheLastGoodSnapshot() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        List<String> log = new ArrayList<>();
        PluginBridge bridge = new PluginBridge(dataRoot, log::add);
        assertEquals(1, bridge.count.peek().intValue());

        // Exactly what a save-in-progress looks like: valid truncation, not valid JSON yet.
        Files.writeString(manifestOf("weather-kit"), "{\"id\": \"weather-kit\", \"na",
                StandardCharsets.UTF_8);
        Fixtures.touch(manifestOf("weather-kit"));

        bridge.tick();

        assertEquals(1, bridge.count.peek().intValue(), "the UI must not flash an empty list");
        assertEquals(1L, bridge.revision.peek().longValue(), "a failed read is not a revision");
        assertEquals("Weather Kit", bridge.items.peek().get(0).name);
        assertTrue(log.stream().anyMatch(line -> line.contains("none parseable")), log.toString());

        // ... and once the write completes, the edit still lands.
        Files.writeString(manifestOf("weather-kit"),
                Fixtures.manifest("weather-kit", "Weather Kit Complete", true,
                        List.of("tool"), List.of("net:http"), "full-body"),
                StandardCharsets.UTF_8);
        Fixtures.touch(manifestOf("weather-kit"));

        bridge.tick();

        assertEquals(2L, bridge.revision.peek().longValue());
        assertEquals("Weather Kit Complete", bridge.items.peek().get(0).name);
        assertEquals("full-body", bridge.items.peek().get(0).tokenPolicy);
    }

    @Test
    void healthReportIsFlattenedOntoThePluginFieldsQmlReads() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", WEATHER_KIT);
        PluginBridge bridge = new PluginBridge(dataRoot, message -> { });
        assertEquals(PluginHealth.UNKNOWN, bridge.items.peek().get(0).health);

        List<PluginInfo> before = bridge.items.peek();
        bridge.applyHealth(Map.of("weather-kit", new PluginHealth("weather-kit", PluginHealth.DEGRADED, 91)));

        List<PluginInfo> after = bridge.items.peek();
        assertEquals(PluginHealth.DEGRADED, after.get(0).health);
        assertEquals(91, after.get(0).latencyMs);
        assertNotEquals(before, after, "a health change must republish the list");
        assertFalse(before.get(0) == after.get(0), "QML gets fresh copies, never the host's objects");

        // An identical report must not churn the scene.
        bridge.applyHealth(Map.of("weather-kit", new PluginHealth("weather-kit", PluginHealth.DEGRADED, 91)));
        assertEquals(after, bridge.items.peek());
    }

    @Test
    void editingTokensJsonShowsUpOnTheNextTick() throws IOException {
        Fixtures.writeTokens(dataRoot, 118_400, 200_000, 24_600, 19_000, 41_200, 33_600);
        List<String> log = new ArrayList<>();
        TokenBridge bridge = new TokenBridge(dataRoot, log::add);
        assertEquals(24_600.0, bridge.persona.peek().doubleValue());
        assertEquals(1L, bridge.revision.peek().longValue());

        Fixtures.writeTokens(dataRoot, 118_400, 200_000, 84_600, 0, 8_000, 25_800);
        Fixtures.touch(dataRoot.resolve(Fixtures.TOKENS));

        bridge.tick();

        assertEquals(84_600.0, bridge.persona.peek().doubleValue());
        assertEquals(0.0, bridge.recall.peek().doubleValue());
        assertEquals(118_400.0, bridge.used.peek().doubleValue());
        assertEquals(2L, bridge.revision.peek().longValue());
        assertTrue(log.stream().anyMatch(line -> line.contains("tokens revision=2")), log.toString());
    }

    @Test
    void unchangedTokensFileIsNotReRead() throws IOException {
        Fixtures.writeTokens(dataRoot, 1, 2, 1, 0, 0, 0);
        TokenBridge bridge = new TokenBridge(dataRoot, message -> { });

        for (int frame = 0; frame < 20; frame++) {
            bridge.tick();
        }

        assertEquals(1L, bridge.revision.peek().longValue());
    }
}
