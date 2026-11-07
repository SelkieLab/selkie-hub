package selkie.host;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The embedded development backend started by {@code --dev-backend=<port>}: a JDK
 * {@link HttpServer} (no extra dependency) that serves the two routes the status layer polls.
 *
 * <h2>Routes</h2>
 * <pre>
 * GET /api/plugins/official/health
 *     {"ok":true,"plugins":[{"id":"weather-kit","health":"healthy","latencyMs":78}, ...]}
 * GET /api/preflight/status
 *     {"ok":true,"checks":[{"name":"font","ok":true},{"name":"network","ok":true}],
 *      "text":"All systems nominal"}
 * </pre>
 *
 * <h2>Data source</h2>
 * Every request re-reads data/plugins/&lt;id&gt;/manifest.json, so editing a manifest (or
 * adding a plugin directory) changes what the endpoint returns <b>while the app runs</b> -
 * that is what makes the health column in the UI a real end-to-end proof instead of a
 * hard-coded string.
 *
 * <h2>Health rule (deterministic, documented)</h2>
 * <ul>
 *   <li>{@code token_policy == "full-body"} -&gt; {@code degraded} (full tool bodies eat the
 *       context budget); anything else -&gt; {@code healthy};</li>
 *   <li>{@code latencyMs} = 18..42 from a hash of the id, +60 when the plugin declares
 *       {@code net:http} (network-bound plugins are slower).</li>
 * </ul>
 *
 * <p>Binds to 127.0.0.1 only: a development endpoint must not be reachable from the network.
 */
public final class DevBackend implements AutoCloseable {

    static final String HEALTH_PATH = BackendPoller.HEALTH_PATH;
    static final String PREFLIGHT_PATH = BackendPoller.PREFLIGHT_PATH;
    /** Preflight sentence the host expects to see in status.preflightText. */
    static final String PREFLIGHT_TEXT = "All systems nominal";

    private final HttpServer server;
    private final ExecutorService executor;
    private final Path dataRoot;
    private final int port;

    private DevBackend(HttpServer server, ExecutorService executor, Path dataRoot, int port) {
        this.server = server;
        this.executor = executor;
        this.dataRoot = dataRoot;
        this.port = port;
    }

    /**
     * Starts the backend on {@code port} (0 picks a free port, which the tests use).
     *
     * @throws IOException when the port is taken or cannot be bound
     */
    public static DevBackend start(int port, Path dataRoot) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "selkie-dev-backend");
            thread.setDaemon(true);
            return thread;
        });
        DevBackend backend = new DevBackend(server, executor, dataRoot, server.getAddress().getPort());
        server.createContext(HEALTH_PATH, exchange -> backend.handle(exchange, backend::healthJson));
        server.createContext(PREFLIGHT_PATH, exchange -> backend.handle(exchange, backend::preflightJson));
        server.setExecutor(executor);
        server.start();
        return backend;
    }

    /** Base URL to poll, e.g. {@code http://127.0.0.1:7071}. */
    public String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /** The actually bound port (differs from the request when 0 was passed). */
    public int port() {
        return port;
    }

    /** Directory the routes read from. */
    public Path dataRoot() {
        return dataRoot;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------
    // Payloads
    // ------------------------------------------------------------------

    /** {@code GET /api/plugins/official/health}. */
    String healthJson() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", Boolean.TRUE);
        List<Map<String, Object>> entries = new ArrayList<>();
        // Re-read on every request: an edit to data/plugins/** must show up on the next poll.
        for (PluginInfo plugin : ManifestReader.read(dataRoot.resolve("plugins"), message -> { }).plugins()) {
            if (!plugin.official) {
                continue;
            }
            PluginHealth health = healthOf(plugin);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", health.id());
            entry.put("health", health.health());
            entry.put("latencyMs", health.latencyMs());
            entries.add(entry);
        }
        body.put("plugins", entries);
        return write(body);
    }

    /** {@code GET /api/preflight/status}. */
    String preflightJson() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", Boolean.TRUE);
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(check("font", true));
        checks.add(check("network", true));
        body.put("checks", checks);
        body.put("text", PREFLIGHT_TEXT);
        return write(body);
    }

    /**
     * The documented health rule - shared by the endpoint and the tests so "what the backend
     * says" and "what the plugin rows must show" cannot drift apart.
     */
    static PluginHealth healthOf(PluginInfo plugin) {
        int latency = 18 + Math.abs(plugin.id.hashCode() % 7) * 4;
        if (plugin.permissions.contains("net:http")) {
            latency += 60;
        }
        String state = plugin.fullBody() ? PluginHealth.DEGRADED : PluginHealth.HEALTHY;
        return new PluginHealth(plugin.id, state, latency);
    }

    private static Map<String, Object> check(String name, boolean ok) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", name);
        check.put("ok", ok);
        return check;
    }

    private static String write(Object body) {
        try {
            return Json.MAPPER.writeValueAsString(body);
        } catch (IOException error) {
            System.err.println("[dev-backend] cannot serialise payload: " + error);
            return "{\"ok\":false,\"error\":\"serialisation failed\"}";
        }
    }

    // ------------------------------------------------------------------
    // HTTP plumbing
    // ------------------------------------------------------------------

    private void handle(HttpExchange exchange, Supplier<String> payload) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"ok\":false,\"error\":\"method not allowed\"}");
                return;
            }
            respond(exchange, 200, payload.get());
        } catch (IOException | RuntimeException error) {
            // A dev server must never take the app down with it.
            System.err.println("[dev-backend] " + exchange.getRequestURI() + " failed: " + error);
            try {
                respond(exchange, 500, "{\"ok\":false,\"error\":\"internal\"}");
            } catch (IOException ignored) {
                // The socket is already gone; nothing left to tell the client.
            }
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
