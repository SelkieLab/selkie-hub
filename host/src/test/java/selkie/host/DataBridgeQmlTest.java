package selkie.host;

import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Item;
import io.github.timer_err.qml4j.render.items.core.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bridge-to-QML contract, checked headlessly: this loads a real scene through qml4j and
 * reads the <em>resolved</em> values, so it proves what a delegate really sees rather than
 * what the bridge intended to publish.
 *
 * <p>Covered: {@code Repeater { model: plugins.items }} with a delegate reading public POJO
 * fields, {@code tokens.*} driving ratio-bar widths, {@code status.*} degrading to
 * "backend unreachable" and recovering, and a hot reload re-running the live bindings.
 */
class DataBridgeQmlTest {

    /**
     * Deliberately Theme-free: this test is about the data bridges, so it uses literal
     * colours and only needs the stock QtQuick types.
     */
    private static final String SCENE = ""
            + "import QtQuick 2.15\n"
            + "Rectangle {\n"
            + "    width: 400; height: 300; color: \"#ffffff\"\n"
            + "    Repeater {\n"
            + "        model: plugins.items\n"
            + "        delegate: Item {\n"
            + "            width: 200; height: 24\n"
            + "            Text { objectName: \"name-\" + modelData.id; text: modelData.name;"
            + "                   font.pixelSize: 12; color: \"#000000\" }\n"
            + "            Text { objectName: \"health-\" + modelData.id; text: modelData.health;"
            + "                   font.pixelSize: 12; color: \"#000000\" }\n"
            + "            Rectangle { objectName: \"official-flag-\" + modelData.id;"
            + "                        width: modelData.official ? 8 : 2; height: 2; color: \"#123456\" }\n"
            + "            Rectangle { objectName: \"latency-\" + modelData.id;"
            + "                        width: modelData.latencyMs * 1.0; height: 2; color: \"#654321\" }\n"
            + "        }\n"
            + "    }\n"
            + "    Rectangle { objectName: \"token-used-bar\"; width: tokens.used / 1000; height: 4;"
            + "                color: \"#00ff00\" }\n"
            + "    Rectangle { objectName: \"token-total-bar\"; width: tokens.total / 1000; height: 4;"
            + "                color: \"#0000ff\" }\n"
            + "    Rectangle { objectName: \"persona-bar\"; width: tokens.persona / 100; height: 4;"
            + "                color: \"#ff00ff\" }\n"
            + "    Text { objectName: \"preflight-text\"; text: status.preflightText;"
            + "           font.pixelSize: 12; color: \"#000000\" }\n"
            + "    Rectangle { objectName: \"preflight-flag\"; width: status.preflightOk ? 9 : 3;"
            + "                height: 2; color: \"#333333\" }\n"
            + "    Rectangle { objectName: \"poll-count\"; width: status.pollCount * 2; height: 2;"
            + "                color: \"#333333\" }\n"
            + "}\n";

    @TempDir
    Path dataRoot;

    private PluginBridge plugins;
    private TokenBridge tokens;
    private FakeSource source;
    private StatusBridge status;
    private QmlView view;

    @BeforeEach
    void loadScene() throws IOException {
        Fixtures.writeManifest(dataRoot, "weather-kit", Fixtures.manifest("weather-kit", "Weather Kit",
                true, List.of("tool", "prompt"), List.of("net:http"), "schema-only"));
        Fixtures.writeManifest(dataRoot, "telemetry-sink", Fixtures.manifest("telemetry-sink", "Telemetry Sink",
                false, List.of("tool"), List.of("net:http"), "full-body"));
        Fixtures.writeTokens(dataRoot, 118_400, 200_000, 24_600, 19_000, 41_200, 33_600);

        plugins = new PluginBridge(dataRoot, message -> { });
        tokens = new TokenBridge(dataRoot, message -> { });
        source = new FakeSource();
        status = new StatusBridge(source, message -> { });

        view = QmlView.withStockTypes(new QmlEngine());
        view.resources(DataBridgeQmlTest::classpath);
        // Same ordering rule as the host: every bridge exists before load().
        view.context("plugins", plugins);
        view.context("tokens", tokens);
        view.context("status", status);
        view.load(SCENE, "");
    }

    @AfterEach
    void dispose() {
        status.close();
        if (view != null) {
            view.dispose();
        }
    }

    @Test
    void repeaterDelegatesReadTheManifestFields() {
        assertEquals("Weather Kit", textOf("name-weather-kit"));
        assertEquals("Telemetry Sink", textOf("name-telemetry-sink"));
        // public boolean official -> a usable conditional in the delegate
        assertEquals(8.0, widthOf("official-flag-weather-kit"), 0.001);
        assertEquals(2.0, widthOf("official-flag-telemetry-sink"), 0.001);
        // ... and public String health / public int latencyMs are readable too
        assertEquals(PluginHealth.UNKNOWN, textOf("health-weather-kit"));
        assertEquals(0.0, widthOf("latency-weather-kit"), 0.001);
    }

    @Test
    void tokenBudgetDrivesEveryRatioBar() {
        assertEquals(118.4, widthOf("token-used-bar"), 0.001);
        assertEquals(200.0, widthOf("token-total-bar"), 0.001);
        assertEquals(246.0, widthOf("persona-bar"), 0.001);
    }

    @Test
    void statusStartsUnreachableAndRecoversWhenTheBackendAnswers() {
        assertEquals(StatusBridge.UNREACHABLE, textOf("preflight-text"));
        assertEquals(3.0, widthOf("preflight-flag"), 0.001);

        source.push(StatusSample.reachable("All systems nominal", Map.of()));
        status.tick();

        assertEquals("All systems nominal", textOf("preflight-text"));
        assertEquals(9.0, widthOf("preflight-flag"), 0.001);
        assertEquals(2.0, widthOf("poll-count"), 0.001, "pollCount must count drained polls");
    }

    @Test
    void healthFromTheBackendLandsInTheDelegateFields() {
        source.push(StatusSample.reachable("All systems nominal",
                Map.of("weather-kit", new PluginHealth("weather-kit", PluginHealth.DEGRADED, 91))));

        Map<String, PluginHealth> health = status.tick();
        assertFalse(health.isEmpty(), "a successful poll must surface the health map");
        // Exactly what Main's DataLayer does with it.
        plugins.applyHealth(health);

        assertEquals(PluginHealth.DEGRADED, textOf("health-weather-kit"));
        assertEquals(91.0, widthOf("latency-weather-kit"), 0.001);
    }

    @Test
    void hotReloadReRunsTheLiveQmlBindings() throws IOException {
        assertEquals("Weather Kit", textOf("name-weather-kit"));
        assertEquals(246.0, widthOf("persona-bar"), 0.001);

        // 1. edit a manifest while the scene is live
        Path manifest = dataRoot.resolve("plugins").resolve("weather-kit").resolve(ManifestReader.FILE_NAME);
        Files.writeString(manifest, Fixtures.manifest("weather-kit", "Weather Kit Reloaded",
                true, List.of("tool"), List.of("net:http"), "schema-only"), StandardCharsets.UTF_8);
        Fixtures.touch(manifest);
        plugins.tick();

        assertEquals("Weather Kit Reloaded", textOf("name-weather-kit"),
                "the delegate must re-read modelData after a manifest edit");
        assertEquals(2L, plugins.revision.peek().longValue());

        // 2. edit tokens.json while the scene is live
        Fixtures.writeTokens(dataRoot, 118_400, 200_000, 84_600, 0, 8_000, 25_800);
        Fixtures.touch(dataRoot.resolve(Fixtures.TOKENS));
        tokens.tick();

        assertEquals(846.0, widthOf("persona-bar"),
                "the ratio bar must re-run when tokens.persona changes");
        assertEquals(2L, tokens.revision.peek().longValue());

        // 3. an added plugin really appends a delegate
        Fixtures.writeManifest(dataRoot, "theme-lab", Fixtures.manifest("theme-lab", "Theme Lab",
                false, List.of("theme"), List.of(), "schema-only"));
        plugins.tick();

        assertEquals(3, plugins.count.peek().intValue());
        assertEquals("Theme Lab", textOf("name-theme-lab"));
        assertTrue(plugins.items.peek().stream().anyMatch(plugin -> "theme-lab".equals(plugin.id)));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String textOf(String objectName) {
        Item item = view.findByObjectName(objectName);
        assertNotNull(item, "the scene has no item named " + objectName);
        assertTrue(item instanceof Text, objectName + " is not a Text item");
        return ((Text) item).text.peek();
    }

    private double widthOf(String objectName) {
        Item item = view.findByObjectName(objectName);
        assertNotNull(item, "the scene has no item named " + objectName);
        return item.width.peek().doubleValue();
    }

    /** qml4j ResourceLoader contract: report a missing resource with null, never by throwing. */
    private static byte[] classpath(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream in = DataBridgeQmlTest.class.getClassLoader().getResourceAsStream(normalized)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException error) {
            return null;
        }
    }

    /** Test double for the HTTP poller: the test decides what the "backend" answered. */
    private static final class FakeSource implements StatusSource {

        private final Queue<StatusSample> samples = new ArrayDeque<>();
        private boolean closed;

        void push(StatusSample sample) {
            samples.add(sample);
        }

        @Override
        public StatusSample poll() {
            return samples.poll();
        }

        @Override
        public String describe() {
            return "http://127.0.0.1:7071";
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
