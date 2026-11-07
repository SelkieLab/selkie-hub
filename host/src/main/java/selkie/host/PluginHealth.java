package selkie.host;

/**
 * Health of one plugin as reported by the dev backend's
 * {@code GET /api/plugins/official/health}.
 *
 * <p>Not a QML type: QML reads the flattened {@link PluginInfo#health} and
 * {@link PluginInfo#latencyMs} fields, because a bridge has to copy this onto the plugin
 * it belongs to anyway.
 *
 * @param id        plugin id the report is about
 * @param health    {@link #HEALTHY}, {@link #DEGRADED} or {@link #UNKNOWN}
 * @param latencyMs last measured round-trip for that plugin, in ms
 */
public record PluginHealth(String id, String health, int latencyMs) {

    public static final String HEALTHY = "healthy";
    public static final String DEGRADED = "degraded";
    /** Used before the first poll answers, and for plugins the backend does not know. */
    public static final String UNKNOWN = "unknown";

    public boolean healthy() {
        return HEALTHY.equals(health);
    }
}
