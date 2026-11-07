package selkie.host;

import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Stage 4 acceptance, made deterministic: editing a plugin manifest or tokens.json on
 * disk while the app is running must reach QML without a restart.
 *
 * <p>Runs headlessly - no window, no screenshots - by binding a live {@link QmlView} to the
 * bridges and reading the resolved {@code Text.text} before and after the file edit. A
 * screen-capture based check cannot prove this reliably (the window gets occluded on this
 * desktop), so the proof lives here instead.
 */
class DataHotReloadTest {

    /** A scene whose plugin names come straight from the bridge, one Text per list entry. */
    private static final String PLUGIN_SCENE = ""
            + "import QtQuick 2.15\n"
            + "Item {\n"
            + "    width: 200; height: 200\n"
            + "    Repeater {\n"
            + "        model: plugins.items\n"
            + "        delegate: Text { objectName: \"plugin-\" + index; text: modelData.name }\n"
            + "    }\n"
            + "}\n";

    private static final String TOKEN_SCENE = ""
            + "import QtQuick 2.15\n"
            + "Item {\n"
            + "    width: 10; height: 10\n"
            + "    Text { objectName: \"token-used\"; text: \"\" + tokens.used }\n"
            + "}\n";

    private static byte[] classpath(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (var in = DataHotReloadTest.class.getClassLoader().getResourceAsStream(normalized)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException error) {
            return null;
        }
    }

    private static void write(Path file, String body) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, body, StandardCharsets.UTF_8);
        // Make the mtime change unambiguous even on a coarse filesystem clock.
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 1000L));
    }

    private static String manifest(String id, String name) {
        return "{ \"id\": \"" + id + "\", \"name\": \"" + name + "\", \"version\": \"1.0.0\","
                + " \"publisher\": \"SelkieLab\", \"description\": \"test fixture\","
                + " \"official\": true, \"provides\": [\"tool\"], \"permissions\": [\"net:http\"],"
                + " \"token_policy\": \"schema-only\" }";
    }

    private static QmlView view(Object bridge, String contextName, String scene) {
        QmlView view = QmlView.withStockTypes(new QmlEngine());
        view.resources(DataHotReloadTest::classpath);
        view.context(contextName, bridge);
        view.load(scene, "");
        return view;
    }

    private static String textOf(QmlView view, String objectName) {
        Text text = (Text) view.findByObjectName(objectName);
        assertNotNull(text, "scene is missing " + objectName);
        return text.text.peek();
    }

    @Test
    void editingAManifestReachesQmlWithoutRestart(@TempDir Path dataRoot) throws Exception {
        Path manifestFile = dataRoot.resolve("plugins").resolve("weather-kit").resolve("manifest.json");
        write(manifestFile, manifest("weather-kit", "Weather Kit"));

        PluginBridge bridge = new PluginBridge(dataRoot);
        bridge.tick();
        QmlView view = view(bridge, "plugins", PLUGIN_SCENE);

        assertEquals("Weather Kit", textOf(view, "plugin-0"));
        Object firstRevision = bridge.revision.peek();

        // The edit a user would make in an editor while the hub is open.
        write(manifestFile, manifest("weather-kit", "Weather Kit PRO"));
        bridge.tick();

        assertEquals("Weather Kit PRO", textOf(view, "plugin-0"),
                "QML did not pick up the edited manifest - hot reload is broken");
        assertNotEquals(firstRevision, bridge.revision.peek(),
                "revision must advance so bindings depending on it re-run");
    }

    @Test
    void editingTheTokenSampleReachesQmlWithoutRestart(@TempDir Path dataRoot) throws Exception {
        Path tokens = dataRoot.resolve("tokens.json");
        write(tokens, "{ \"used\": 118400, \"total\": 200000, \"persona\": 24600,"
                + " \"recall\": 19000, \"tools\": 41200, \"history\": 33600 }");

        TokenBridge bridge = new TokenBridge(dataRoot);
        bridge.tick();
        QmlView view = view(bridge, "tokens", TOKEN_SCENE);

        assertTrue(textOf(view, "token-used").startsWith("118400"),
                "token text should show the file value, got " + textOf(view, "token-used"));

        write(tokens, "{ \"used\": 133700, \"total\": 200000, \"persona\": 24600,"
                + " \"recall\": 19000, \"tools\": 41200, \"history\": 48900 }");
        bridge.tick();

        assertTrue(textOf(view, "token-used").startsWith("133700"),
                "QML did not pick up the edited tokens.json, got " + textOf(view, "token-used"));
    }
}
