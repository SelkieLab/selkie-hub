package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The QML context object plugins: what data/plugins/&lt;id&gt;/manifest.json says, with
 * mtime-driven hot reload.
 *
 * <h2>QML contract (fixed by the Lead)</h2>
 * <ul>
 *   <li>plugins.items - every plugin, official first then name;</li>
 *   <li>plugins.officialItems / plugins.thirdPartyItems - the two groups;</li>
 *   <li>plugins.count, plugins.source ("manifest"), plugins.revision (bumped on every
 *       successful re-read);</li>
 *   <li>plugins.reload() - force an immediate re-read (a refresh button may call it).</li>
 * </ul>
 * Each item is a {@link PluginInfo}: QML reads its <b>public fields</b>
 * (modelData.name, modelData.official, modelData.health, modelData.providesText, ...).
 *
 * <h2>Threading</h2>
 * Constructed and ticked on the render thread only. Reading files is local I/O; the HTTP
 * side of the app never touches these properties (see {@link StatusBridge}).
 *
 * <h2>Hot reload</h2>
 * {@link #tick()} runs once per frame: it stats the manifest tree through {@link DataStamp}
 * and re-reads only when that fingerprint changed, so editing a manifest (or adding or
 * removing a plugin directory) shows up in the UI within a frame or two.
 */
public final class PluginBridge extends QObject {

    /** Value of {@link #source}: the data comes from manifests on disk. */
    public static final String SOURCE = "manifest";

    /** Every plugin, official first then name. */
    public final Property<List<PluginInfo>> items = new Property<>(List.of());
    /** items filtered to official == true. */
    public final Property<List<PluginInfo>> officialItems = new Property<>(List.of());
    /** items filtered to official == false. */
    public final Property<List<PluginInfo>> thirdPartyItems = new Property<>(List.of());
    /** Number of installed plugins, so headers do not depend on a list read. */
    public final Property<Integer> count = new Property<>(0);
    /** Where the data came from - always {@link #SOURCE} for this bridge. */
    public final Property<String> source = new Property<>(SOURCE);
    /** Incremented on every successful re-read; the log prints it, tests assert on it. */
    public final Property<Long> revision = new Property<>(0L);

    private final Path pluginsDirectory;
    private final Consumer<String> log;

    /** Last fingerprint that was read (or attempted), so an unchanged tree costs only stats. */
    private DataStamp attempted = DataStamp.NEVER;
    /** The host's own copy - never handed to QML. */
    private List<PluginInfo> master = List.of();
    /** Newest health report from the dev backend, applied whenever a snapshot is published. */
    private Map<String, PluginHealth> healthById = Map.of();
    private long revisionCounter;

    /** Opens &lt;dataRoot&gt;/plugins and reads it once. */
    public PluginBridge(Path dataRoot) {
        this(dataRoot, System.out::println);
    }

    PluginBridge(Path dataRoot, Consumer<String> log) {
        this.pluginsDirectory = dataRoot.resolve("plugins");
        this.log = log;
        attempted = DataStamp.ofPluginTree(pluginsDirectory);
        read();
    }

    // ------------------------------------------------------------------
    // Host entry points
    // ------------------------------------------------------------------

    /** Once per frame, render thread: re-read only when the manifests on disk changed. */
    public void tick() {
        DataStamp current = DataStamp.ofPluginTree(pluginsDirectory);
        if (current.equals(attempted)) {
            return;
        }
        attempted = current;
        read();
    }

    /** QML entry point: force a re-read now (plugins.reload()). */
    public void reload() {
        attempted = DataStamp.ofPluginTree(pluginsDirectory);
        read();
    }

    /**
     * Applies a health report from the dev backend (render thread). Every publish hands QML
     * fresh {@link PluginInfo} copies, so a changed health/latencyMs really re-runs the
     * delegate bindings.
     */
    public void applyHealth(Map<String, PluginHealth> reported) {
        if (reported == null || reported.isEmpty() || reported.equals(healthById)) {
            return;
        }
        healthById = Map.copyOf(reported);
        if (!master.isEmpty()) {
            publish();
        }
    }

    /** Directory the bridge watches (host/test diagnostics). */
    public Path pluginsDirectory() {
        return pluginsDirectory;
    }

    /** Host-side snapshot of the parsed manifests (no health applied). */
    List<PluginInfo> master() {
        return master;
    }

    // ------------------------------------------------------------------
    // Read + publish
    // ------------------------------------------------------------------

    private void read() {
        ManifestReader.Snapshot snapshot = ManifestReader.read(pluginsDirectory, log);
        if (snapshot.plugins().isEmpty() && snapshot.manifests() > 0 && !master.isEmpty()) {
            // Every manifest failed to parse - almost always a half-written file caught
            // mid-save. Keep the last good snapshot (no empty-list flash) and retry when the
            // fingerprint changes again; the failing fingerprint is remembered, so this does
            // not re-parse every frame.
            log.accept("[data] plugins: " + snapshot.manifests() + " manifest(s) on disk but none"
                    + " parseable (" + snapshot.skipped() + " skipped) - keeping revision "
                    + revisionCounter);
            return;
        }
        master = snapshot.plugins();
        publish();
    }

    private void publish() {
        List<PluginInfo> all = new ArrayList<>(master.size());
        List<PluginInfo> official = new ArrayList<>();
        List<PluginInfo> thirdParty = new ArrayList<>();
        for (PluginInfo plugin : master) {
            PluginInfo copy = plugin.copy();
            PluginHealth health = healthById.get(copy.id);
            if (health != null) {
                copy.health = health.health();
                copy.latencyMs = health.latencyMs();
            }
            all.add(copy);
            if (copy.official) {
                official.add(copy);
            } else {
                thirdParty.add(copy);
            }
        }
        List<PluginInfo> immutableAll = List.copyOf(all);
        List<PluginInfo> immutableOfficial = List.copyOf(official);
        List<PluginInfo> immutableThirdParty = List.copyOf(thirdParty);
        revisionCounter++;
        // Property.set compares by value: an unchanged re-read (a touch, a save that produced
        // identical JSON) publishes equal lists and QML correctly does not rebind.
        items.set(immutableAll);
        officialItems.set(immutableOfficial);
        thirdPartyItems.set(immutableThirdParty);
        count.set(immutableAll.size());
        source.set(SOURCE);
        revision.set(revisionCounter);
        log.accept("[data] plugins revision=" + revisionCounter
                + " count=" + immutableAll.size()
                + " official=" + immutableOfficial.size()
                + " thirdParty=" + immutableThirdParty.size()
                + " source=" + SOURCE);
    }
}
