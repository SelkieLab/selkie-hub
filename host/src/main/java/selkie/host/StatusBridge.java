package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The QML context object status: the dev backend's preflight verdict, fed by
 * {@link BackendPoller}'s HTTP client.
 *
 * <h2>QML contract (fixed by the Lead)</h2>
 * <ul>
 *   <li>status.preflightOk - true when /api/preflight/status answered;</li>
 *   <li>status.preflightText - the backend sentence, or "backend unreachable";</li>
 *   <li>status.endpoint - the URL being polled;</li>
 *   <li>status.pollCount - how many polls have completed (proves the poller is alive).</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * The HTTP work happens on a daemon thread inside {@link BackendPoller}; this bridge only
 * drains queued samples, and it does so from {@link #tick()} on the render thread. No
 * property here is ever written by the worker.
 *
 * <p>{@link #tick()} also returns the newest official-plugin health map, which Main hands to
 * {@link PluginBridge#applyHealth(Map)} so the plugin rows show health/latency.
 */
public final class StatusBridge extends QObject {

    /** Text QML shows while the backend is not answering. */
    public static final String UNREACHABLE = "backend unreachable";

    public final Property<Boolean> preflightOk = new Property<>(Boolean.FALSE);
    public final Property<String> preflightText = new Property<>(UNREACHABLE);
    public final Property<String> endpoint = new Property<>("");
    public final Property<Integer> pollCount = new Property<>(0);

    private final StatusSource source;
    private final Consumer<String> log;
    private String lastText;

    public StatusBridge(StatusSource source) {
        this(source, System.out::println);
    }

    StatusBridge(StatusSource source, Consumer<String> log) {
        this.source = source;
        this.log = log;
        this.endpoint.set(source.describe());
        log.accept("[status] polling " + source.describe() + " every "
                + BackendPoller.INTERVAL.toMillis() + "ms (async, drained on the render thread)");
    }

    /**
     * Render-thread drain: applies every sample that arrived since the last frame.
     *
     * @return the newest official-plugin health map, or an empty map when no successful
     *         health payload has arrived (the caller then keeps the previous health)
     */
    public Map<String, PluginHealth> tick() {
        Map<String, PluginHealth> health = Map.of();
        StatusSample sample;
        while ((sample = source.poll()) != null) {
            pollCount.set(pollCount.peek() + 1);
            preflightOk.set(sample.ok());
            preflightText.set(sample.text());
            if (sample.ok() && !sample.health().isEmpty()) {
                health = sample.health();
            }
        }
        String text = preflightText.peek();
        if (!text.equals(lastText)) {
            // Log transitions only: a backend that is down must not fill stderr with one
            // line per poll attempt.
            lastText = text;
            log.accept("[status] preflight " + (Boolean.TRUE.equals(preflightOk.peek()) ? "ok" : "FAILED")
                    + " after " + pollCount.peek() + " poll(s): \"" + text + "\""
                    + " endpoint=" + endpoint.peek());
        }
        return health;
    }

    /** Stops the background poller (host shutdown). */
    public void close() {
        source.close();
    }

    /** Current polling URL, also exposed to QML as the {@link #endpoint} property. */
    public String endpointUrl() {
        return endpoint.peek();
    }
}
