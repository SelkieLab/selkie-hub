package selkie.host;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The QML context object tokens: the context-budget sample in data/tokens.json.
 *
 * <h2>QML contract (fixed by the Lead)</h2>
 * tokens.used, .total, .persona, .recall, .tools, .history are all Property&lt;Double&gt;
 * (a ratio bar needs doubles, and a percentage is used / total); .source names the file the
 * numbers came from ("tokens.json", or "unavailable" when it could not be read); .revision
 * bumps on every successful re-read; tokens.reload() forces one.
 *
 * <p>Invariant of the file: persona + recall + tools + history == used. The bridge does not
 * enforce it - it reports a mismatch once in the log, so a broken fixture is visible instead
 * of silently producing percentages that do not add up.
 *
 * <p>Same hot-reload contract as {@link PluginBridge}: {@link #tick()} stats the file once
 * per frame and re-reads only when the fingerprint changed.
 */
public final class TokenBridge extends QObject {

    /** Value of {@link #source} once the file has been read. */
    public static final String SOURCE = "tokens.json";
    /** Value of {@link #source} when the file is missing or unparseable. */
    public static final String UNAVAILABLE = "unavailable";

    public final Property<Double> used = new Property<>(0.0);
    public final Property<Double> total = new Property<>(0.0);
    public final Property<Double> persona = new Property<>(0.0);
    public final Property<Double> recall = new Property<>(0.0);
    public final Property<Double> tools = new Property<>(0.0);
    public final Property<Double> history = new Property<>(0.0);
    public final Property<String> source = new Property<>(UNAVAILABLE);
    public final Property<Long> revision = new Property<>(0L);

    private final Path file;
    private final Consumer<String> log;
    private DataStamp attempted = DataStamp.NEVER;
    private long revisionCounter;

    /** Opens &lt;dataRoot&gt;/tokens.json and reads it once. */
    public TokenBridge(Path dataRoot) {
        this(dataRoot, System.out::println);
    }

    TokenBridge(Path dataRoot, Consumer<String> log) {
        this.file = dataRoot.resolve(SOURCE);
        this.log = log;
        attempted = DataStamp.ofFile(file);
        read();
    }

    /** Once per frame, render thread: re-read only when tokens.json changed. */
    public void tick() {
        DataStamp current = DataStamp.ofFile(file);
        if (current.equals(attempted)) {
            return;
        }
        attempted = current;
        read();
    }

    /** QML entry point: force a re-read now (tokens.reload()). */
    public void reload() {
        attempted = DataStamp.ofFile(file);
        read();
    }

    /** The file being watched (host/test diagnostics). */
    public Path file() {
        return file;
    }

    private void read() {
        JsonNode root;
        try {
            root = Json.MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8));
            if (root == null || !root.isObject()) {
                throw new IOException("tokens.json is not a JSON object");
            }
        } catch (IOException | RuntimeException error) {
            // Missing fixtures are a normal state (a fresh checkout without data/), not a
            // crash: publish zeros, say so in source, and never print more than once.
            source.set(UNAVAILABLE);
            log.accept("[data] tokens: cannot read " + file + " (" + error.getMessage()
                    + ") - showing zeroes, source=" + UNAVAILABLE);
            apply(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        double usedValue = number(root, "used");
        double totalValue = number(root, "total");
        double personaValue = number(root, "persona");
        double recallValue = number(root, "recall");
        double toolsValue = number(root, "tools");
        double historyValue = number(root, "history");

        apply(usedValue, totalValue, personaValue, recallValue, toolsValue, historyValue);
        source.set(SOURCE);
        revisionCounter++;
        revision.set(revisionCounter);
        log.accept("[data] tokens revision=" + revisionCounter + " source=" + SOURCE
                + " used=" + Math.round(usedValue) + "/" + Math.round(totalValue)
                + " persona=" + Math.round(personaValue)
                + " recall=" + Math.round(recallValue)
                + " tools=" + Math.round(toolsValue)
                + " history=" + Math.round(historyValue));

        double parts = personaValue + recallValue + toolsValue + historyValue;
        if (usedValue > 0 && Math.abs(parts - usedValue) > 0.5) {
            log.accept(String.format(Locale.ROOT,
                    "[data] tokens: breakdown sums to %.0f but used=%.0f - fixture is inconsistent",
                    parts, usedValue));
        }
    }

    private void apply(double usedValue, double totalValue, double personaValue,
                       double recallValue, double toolsValue, double historyValue) {
        used.set(usedValue);
        total.set(totalValue);
        persona.set(personaValue);
        recall.set(recallValue);
        tools.set(toolsValue);
        history.set(historyValue);
    }

    private static double number(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || !node.isNumber() ? 0.0 : node.asDouble();
    }
}
