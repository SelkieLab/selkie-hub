package selkie.host;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the dev backend with java.net.http.HttpClient every 2 s on a daemon thread and
 * enqueues the results for the render thread.
 *
 * <h2>Why a queue and not a Property write</h2>
 * qml4j's Property change bookkeeping is global and not thread-safe: only the render thread
 * may write a QML-visible property. So this class never touches a bridge - it only appends
 * {@link StatusSample} values, and {@link StatusBridge#tick()} (render thread) drains them.
 *
 * <h2>Backend down is not an error</h2>
 * Connection refused / timeout / malformed JSON all collapse into one
 * {@code unreachable} sample. Nothing is printed per attempt, so an app started without
 * {@code --dev-backend} does not spam stderr; {@link StatusBridge} logs the transition once.
 */
public final class BackendPoller implements StatusSource {

    /** How often the backend is polled. */
    public static final Duration INTERVAL = Duration.ofSeconds(2);
    /** Per-request budget; shorter than {@link #INTERVAL} so polls cannot pile up. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(1200);

    /** Routes the dev backend serves. */
    static final String HEALTH_PATH = "/api/plugins/official/health";
    static final String PREFLIGHT_PATH = "/api/preflight/status";

    /** Never queue more than this many stale samples while the render thread is stalled. */
    private static final int MAX_QUEUED = 4;

    private final String baseUrl;
    private final HttpClient client;
    private final ScheduledExecutorService scheduler;
    private final Queue<StatusSample> inbox = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean polling = new AtomicBoolean();
    private volatile boolean closed;

    public BackendPoller(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
        this.client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "selkie-backend-poll");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::pollOnce, 0L, INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public StatusSample poll() {
        return inbox.poll();
    }

    @Override
    public String describe() {
        return baseUrl;
    }

    @Override
    public void close() {
        closed = true;
        scheduler.shutdownNow();
    }

    // ------------------------------------------------------------------
    // Worker thread
    // ------------------------------------------------------------------

    private void pollOnce() {
        if (closed || !polling.compareAndSet(false, true)) {
            return;
        }
        try {
            HttpResponse<String> preflightResponse = send(PREFLIGHT_PATH);
            if (preflightResponse.statusCode() != 200) {
                enqueue(StatusSample.unreachable("backend answered HTTP " + preflightResponse.statusCode()));
                return;
            }
            JsonNode preflight = Json.MAPPER.readTree(preflightResponse.body());
            String text = preflight == null ? "" : preflight.path("text").asText("").trim();
            if (text.isEmpty()) {
                text = "backend reachable";
            }
            Map<String, PluginHealth> health = Map.of();
            HttpResponse<String> healthResponse = send(HEALTH_PATH);
            if (healthResponse.statusCode() == 200) {
                health = readHealth(Json.MAPPER.readTree(healthResponse.body()));
            }
            enqueue(StatusSample.reachable(text, health));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException error) {
            // Backend not started, connection refused, timeout, bad JSON: one state, no log.
            enqueue(StatusSample.unreachable(StatusBridge.UNREACHABLE));
        } finally {
            polling.set(false);
        }
    }

    private HttpResponse<String> send(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void enqueue(StatusSample sample) {
        while (inbox.size() >= MAX_QUEUED) {
            inbox.poll();
        }
        inbox.add(sample);
    }

    private static Map<String, PluginHealth> readHealth(JsonNode root) {
        if (root == null) {
            return Map.of();
        }
        JsonNode plugins = root.path("plugins");
        if (!plugins.isArray()) {
            return Map.of();
        }
        Map<String, PluginHealth> health = new LinkedHashMap<>();
        for (JsonNode node : plugins) {
            String id = node.path("id").asText("").trim();
            if (id.isEmpty()) {
                continue;
            }
            health.put(id, new PluginHealth(id,
                    node.path("health").asText(PluginHealth.UNKNOWN).trim(),
                    node.path("latencyMs").asInt(0)));
        }
        return Map.copyOf(health);
    }

    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:7071";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
