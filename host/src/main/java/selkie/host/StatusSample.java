package selkie.host;

import java.util.Map;

/**
 * One completed poll of the dev backend, handed from the worker thread to the render thread
 * through a queue (qml4j forbids cross-thread Property writes).
 *
 * @param ok     true when /api/preflight/status answered with a usable payload
 * @param text   the backend's own preflight sentence, or "backend unreachable"
 * @param health official-plugin health keyed by plugin id (empty when unknown)
 */
public record StatusSample(boolean ok, String text, Map<String, PluginHealth> health) {

    /** A successful poll. */
    public static StatusSample reachable(String text, Map<String, PluginHealth> health) {
        return new StatusSample(true, text, Map.copyOf(health));
    }

    /** The backend is not running (or answered with garbage) - a normal state, not an error. */
    public static StatusSample unreachable(String text) {
        return new StatusSample(false, text, Map.of());
    }
}
