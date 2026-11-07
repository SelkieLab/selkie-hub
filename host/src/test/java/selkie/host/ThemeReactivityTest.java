package selkie.host;

import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Rectangle;
import org.junit.jupiter.api.Test;
import selkie.theme.Md3eThemeGenerator;
import selkie.theme.ThemeBridge;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The engine-level guard for theme reactivity: a host-side scheme flip must re-run the QML
 * bindings that read the {@code Theme} singleton.
 *
 * <p>This runs headlessly (no GLFW window, no screenshots), so it cannot be fooled by a
 * minimised window or a flaky screen capture - it reads the <em>resolved</em> colour of a
 * live QML item before and after {@code setDark(true)}.
 *
 * <p>It also pins the two qml4j behaviours the shell depends on:
 * <ul>
 *   <li>{@code import "."} with an empty unit path resolves the project's {@code qmldir};</li>
 *   <li>a {@code theme.*Hex} {@code Property} write propagates into a downstream binding.</li>
 * </ul>
 */
class ThemeReactivityTest {

    private static final int SEED = 0xFF6750A4;

    /** A minimal scene standing in for the shell: two probes bound to Theme roles. */
    private static final String SCENE = ""
            + "import QtQuick 2.15\n"
            + "import \".\"\n"
            + "Rectangle {\n"
            + "    width: 10; height: 10\n"
            + "    Rectangle { objectName: \"probe-surface\"; width: 1; height: 1; color: Theme.surface }\n"
            + "    Rectangle { objectName: \"probe-primary\"; width: 1; height: 1; color: Theme.primary }\n"
            + "    Rectangle { objectName: \"probe-onSurface\"; width: 1; height: 1; color: Theme.onSurface }\n"
            + "}\n";

    /** qml4j ResourceLoader contract: report a missing resource with null, never by throwing. */
    private static byte[] classpath(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream in = ThemeReactivityTest.class.getClassLoader().getResourceAsStream(normalized)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException error) {
            return null;
        }
    }

    private static String colourOf(QmlView view, String objectName) {
        Rectangle rectangle = (Rectangle) view.findByObjectName(objectName);
        assertNotNull(rectangle, "scene is missing the probe " + objectName);
        return rectangle.color.peek();
    }

    @Test
    void schemeFlipRebindsEveryRoleInLiveQml() {
        ThemeBridge bridge = new ThemeBridge(SEED, false);
        QmlView view = QmlView.withStockTypes(new QmlEngine());
        view.resources(ThemeReactivityTest::classpath);
        view.context("theme", bridge);
        view.load(SCENE, "");

        String[] probes = {"surface", "primary", "onSurface"};
        String[] light = new String[probes.length];
        for (int i = 0; i < probes.length; i++) {
            light[i] = colourOf(view, "probe-" + probes[i]);
        }

        // The light scheme really is the MD3E light scheme, not a placeholder.
        assertEquals(Md3eThemeGenerator.generate(SEED, false).surface(),
                Integer.parseInt(light[0].substring(1), 16) | 0xFF000000,
                "light surface should be the generated light role, got " + light[0]);

        bridge.setDark(true);

        for (int i = 0; i < probes.length; i++) {
            String dark = colourOf(view, "probe-" + probes[i]);
            assertNotEquals(light[i], dark,
                    "binding for Theme." + probes[i] + " did not re-run after setDark(true)");
        }

        // ... and the values are the generated dark roles, not some arbitrary change.
        assertEquals(Md3eThemeGenerator.generate(SEED, true).primary(),
                Integer.parseInt(colourOf(view, "probe-primary").substring(1), 16) | 0xFF000000,
                "dark primary on screen must equal the generated dark primary");
    }

    @Test
    void seedChangeAlsoRebinds() {
        ThemeBridge bridge = new ThemeBridge(SEED, false);
        QmlView view = QmlView.withStockTypes(new QmlEngine());
        view.resources(ThemeReactivityTest::classpath);
        view.context("theme", bridge);
        view.load(SCENE, "");

        String before = colourOf(view, "probe-primary");
        bridge.setSeed(0xFF4285F4);

        assertNotEquals(before, colourOf(view, "probe-primary"),
                "Theme.primary must re-run when the seed changes");
    }
}
