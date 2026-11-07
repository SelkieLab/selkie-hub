package selkie.host;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The embedded dev backend, over a real socket: both routes, the JSON shape the contract
 * fixes, data read live from data/, and the pairing with {@link BackendPoller}.
 *
 * <p>No window and no GLFW: the backend is plain JDK {@code com.sun.net.httpserver}.
 */
class DevBackendTest {

    @TempDir
    Path dataRoot;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private void writeShippedLikeFixtures() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", Fixtures.manifest("weather-kit", "Weather Kit",
                true, List.of("tool", "prompt"), List.of("net:http"), "schema-only"));
        Fixtures.writeManifest(dataRoot, "memory-vault", Fixtures.manifest("memory-vault", "Memory Vault",
                true, List.of("tool", "prompt"), List.of("fs:read", "fs:write"), "full-body"));
        Fixtures.writeManifest(dataRoot, "theme-lab", Fixtures.manifest("theme-lab", "Theme Lab",
                false, List.of("theme"), List.of(), "schema-only"));
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET " + url + " -> " + response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        return Json.MAPPER.readTree(response.body());
    }

    @Test
    void healthRouteListsOfficialPluginsOnly() throws Exception {
        writeShippedLikeFixtures();
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            JsonNode body = getJson(backend.baseUrl() + DevBackend.HEALTH_PATH);

            assertTrue(body.path("ok").asBoolean());
            JsonNode plugins = body.path("plugins");
            assertEquals(2, plugins.size(), "third-party plugins must not appear: " + plugins);

            JsonNode weather = findById(plugins, "weather-kit");
            assertEquals(PluginHealth.HEALTHY, weather.path("health").asText());
            // net:http adds the documented 60 ms on top of the 18..42 ms base.
            assertTrue(weather.path("latencyMs").asInt() >= 78,
                    "net:http plugin should be slower, got " + weather.path("latencyMs").asInt());

            JsonNode vault = findById(plugins, "memory-vault");
            assertEquals(PluginHealth.DEGRADED, vault.path("health").asText(),
                    "a full-body token policy must report degraded");
        }
    }

    @Test
    void preflightRouteReturnsTheContractShape() throws Exception {
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            JsonNode body = getJson(backend.baseUrl() + DevBackend.PREFLIGHT_PATH);

            assertTrue(body.path("ok").asBoolean());
            assertEquals(DevBackend.PREFLIGHT_TEXT, body.path("text").asText());
            JsonNode checks = body.path("checks");
            assertEquals(2, checks.size());
            assertEquals("font", checks.get(0).path("name").asText());
            assertEquals("network", checks.get(1).path("name").asText());
            assertTrue(checks.get(0).path("ok").asBoolean());
        }
    }

    @Test
    void editingAManifestChangesWhatTheEndpointReturns() throws Exception {
        writeShippedLikeFixtures();
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            JsonNode before = getJson(backend.baseUrl() + DevBackend.HEALTH_PATH);
            assertEquals(PluginHealth.HEALTHY, findById(before.path("plugins"), "weather-kit").path("health").asText());

            // The backend reads data/ per request: no restart, no cache.
            Fixtures.writeManifest(dataRoot, "weather-kit", Fixtures.manifest("weather-kit", "Weather Kit",
                    true, List.of("tool", "prompt"), List.of("net:http"), "full-body"));

            JsonNode after = getJson(backend.baseUrl() + DevBackend.HEALTH_PATH);
            assertEquals(PluginHealth.DEGRADED, findById(after.path("plugins"), "weather-kit").path("health").asText());
        }
    }

    @Test
    void addingAPluginAppearsOnTheNextRequest() throws Exception {
        writeShippedLikeFixtures();
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            int before = getJson(backend.baseUrl() + DevBackend.HEALTH_PATH).path("plugins").size();

            Fixtures.writeManifest(dataRoot, "tool-registry", Fixtures.manifest("tool-registry", "Tool Registry",
                    true, List.of("tool"), List.of(), "schema-only"));

            JsonNode after = getJson(backend.baseUrl() + DevBackend.HEALTH_PATH);
            assertEquals(before + 1, after.path("plugins").size());
            assertEquals("tool-registry", findById(after.path("plugins"), "tool-registry").path("id").asText());
        }
    }

    @Test
    void nonGetRequestsAreRejected() throws Exception {
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(backend.baseUrl() + DevBackend.PREFLIGHT_PATH))
                            .timeout(Duration.ofSeconds(2))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(405, response.statusCode());
        }
    }

    @Test
    void pollerReachesTheBackendWithoutBeingOnTheRenderThread() throws Exception {
        writeShippedLikeFixtures();
        try (DevBackend backend = DevBackend.start(0, dataRoot);
             BackendPoller poller = new BackendPoller(backend.baseUrl())) {
            StatusSample sample = awaitSample(poller);
            assertNotNull(sample, "the poller produced no sample within its budget");
            assertTrue(sample.ok(), "expected a reachable sample, got " + sample);
            assertEquals(DevBackend.PREFLIGHT_TEXT, sample.text());
            assertFalse(sample.health().isEmpty(), "the health route should have been polled too");
            assertEquals(PluginHealth.DEGRADED, sample.health().get("memory-vault").health());
        }
    }

    @Test
    void pollerDegradesQuietlyWhenNothingListens() throws Exception {
        int freePort = freePort();
        try (BackendPoller poller = new BackendPoller("http://127.0.0.1:" + freePort)) {
            StatusSample sample = awaitSample(poller);
            assertNotNull(sample, "the poller must still produce a sample when the backend is down");
            assertFalse(sample.ok());
            assertEquals(StatusBridge.UNREACHABLE, sample.text());
            assertTrue(sample.health().isEmpty());
        }
    }

    /** Waits up to one poll interval plus slack for the first sample. */
    private static StatusSample awaitSample(BackendPoller poller) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            StatusSample sample = poller.poll();
            if (sample != null) {
                return sample;
            }
            Thread.sleep(25L);
        }
        return null;
    }

    private static JsonNode findById(JsonNode plugins, String id) {
        for (JsonNode plugin : plugins) {
            if (id.equals(plugin.path("id").asText())) {
                return plugin;
            }
        }
        throw new AssertionError("no plugin '" + id + "' in " + plugins);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @Test
    void dataRootIsRememberedForDiagnostics() throws Exception {
        Files.createDirectories(dataRoot);
        try (DevBackend backend = DevBackend.start(0, dataRoot)) {
            assertEquals(dataRoot.toAbsolutePath().normalize(),
                    backend.dataRoot().toAbsolutePath().normalize());
            assertEquals(backend.port(), URI.create(backend.baseUrl()).getPort());
            assertTrue(List.of("127.0.0.1").contains(URI.create(backend.baseUrl()).getHost()),
                    "the dev backend must bind the loopback interface only");
        }
    }
}
