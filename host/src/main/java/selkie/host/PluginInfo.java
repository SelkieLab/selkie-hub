package selkie.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One plugin, flattened for QML - registered nowhere except inside
 * {@link PluginBridge#items} / {@code officialItems} / {@code thirdPartyItems}.
 *
 * <h2>PUBLIC FIELDS ONLY</h2>
 * qml4j resolves members with {@code Class.getField}: a QML delegate reads
 * {@code modelData.name}, {@code modelData.official}, never {@code getName()}. A getter, a
 * record accessor or a bean property would be invisible to QML, so this class deliberately
 * has no accessors - just public fields and a default constructor.
 *
 * <h2>Why {@code providesText} / {@code permissionsText} exist</h2>
 * QML cannot index a {@code List<String>} reliably, so the parser also publishes the same
 * values joined with {@code ", "}. The raw lists are kept for the host and the tests.
 *
 * <p>Instances are immutable in spirit: {@link PluginBridge} hands out fresh copies on every
 * publish, because {@code Property.set} compares lists by element equality and a POJO with
 * identity equality would otherwise make an in-place edit invisible to QML.
 */
public final class PluginInfo {

    /** {@code token_policy} value: only tool schemas are fed to the model. */
    public static final String SCHEMA_ONLY = "schema-only";
    /** {@code token_policy} value: full tool bodies enter the context budget. */
    public static final String FULL_BODY = "full-body";
    /** Fallback version for a manifest that omits {@code version}. */
    public static final String DEFAULT_VERSION = "0.0.0";

    // --- manifest fields (public: read directly by QML) ---
    public String id = "";
    public String name = "";
    public String version = DEFAULT_VERSION;
    public String publisher = "";
    public String description = "";
    public String tokenPolicy = SCHEMA_ONLY;
    public boolean official;

    // --- live fields filled from the dev backend (also public fields) ---
    public String health = PluginHealth.UNKNOWN;
    public int latencyMs;

    // --- pre-joined display text next to the raw lists ---
    public String providesText = "";
    public String permissionsText = "";
    public List<String> provides = List.of();
    public List<String> permissions = List.of();

    /** Required for host-side construction; QML never instantiates this class. */
    public PluginInfo() {
    }

    /** Deep copy - what {@link PluginBridge} publishes, so QML can never mutate host state. */
    PluginInfo copy() {
        PluginInfo copy = new PluginInfo();
        copy.id = id;
        copy.name = name;
        copy.version = version;
        copy.publisher = publisher;
        copy.description = description;
        copy.tokenPolicy = tokenPolicy;
        copy.official = official;
        copy.health = health;
        copy.latencyMs = latencyMs;
        copy.providesText = providesText;
        copy.permissionsText = permissionsText;
        copy.provides = List.copyOf(provides);
        copy.permissions = List.copyOf(permissions);
        return copy;
    }

    /** Host/test helper; qml4j also exposes it to QML as a 1-argument method. */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /** True when this plugin's token policy pushes full bodies into the context budget. */
    public boolean fullBody() {
        return FULL_BODY.equalsIgnoreCase(tokenPolicy);
    }

    /** Diagnostics only - never shown as-is in the UI. */
    @Override
    public String toString() {
        return id + " v" + version + (official ? " [official]" : " [third-party]")
                + " health=" + health + " latency=" + latencyMs + "ms"
                + " provides=[" + providesText + "] permissions=[" + permissionsText + "]"
                + " token_policy=" + tokenPolicy;
    }

    /** Equality is field-based so tests can compare parsed manifests readably. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PluginInfo that)) {
            return false;
        }
        return official == that.official
                && latencyMs == that.latencyMs
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Objects.equals(publisher, that.publisher)
                && Objects.equals(description, that.description)
                && Objects.equals(tokenPolicy, that.tokenPolicy)
                && Objects.equals(health, that.health)
                && Objects.equals(providesText, that.providesText)
                && Objects.equals(permissionsText, that.permissionsText)
                && Objects.equals(provides, that.provides)
                && Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, version, publisher, description, tokenPolicy, official,
                health, latencyMs, providesText, permissionsText, provides, permissions);
    }

    /** Fresh, mutable list - the parser fills it in. */
    static List<String> newList() {
        return new ArrayList<>();
    }
}
