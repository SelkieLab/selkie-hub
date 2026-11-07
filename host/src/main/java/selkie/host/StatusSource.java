package selkie.host;

/**
 * Where {@link StatusBridge} gets its poll results from.
 *
 * <p>{@link BackendPoller} is the production implementation (java.net.http.HttpClient on a
 * daemon thread, every 2 s). The interface exists so the bridge's drain/apply behaviour can
 * be tested headlessly without a server and without waiting for a timer.
 */
public interface StatusSource extends AutoCloseable {

    /**
     * The next completed sample, or null when nothing has arrived since the last call.
     * Must be safe to call from the render thread while a worker produces samples.
     */
    StatusSample poll();

    /** Endpoint description for the UI (the polling URL). */
    String describe();

    @Override
    void close();
}
