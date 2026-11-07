package selkie.host;

import io.github.humbleui.skija.DirectContext;
import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Item;
import io.github.timer_err.qml4j.render.items.core.Rectangle;
import io.github.timer_err.qml4j.render.items.core.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import selkie.theme.ColorScheme;
import selkie.theme.Md3eThemeGenerator;
import selkie.theme.MotionTokens;
import selkie.theme.ThemeBridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Selkie Hub host: an OS window (GLFW + OpenGL 3.2 core) with Skija drawing into its
 * framebuffer, qml4j rendering {@code main.qml} into that Skia surface, and the MD3E
 * theme exposed to QML as the {@code Theme} singleton over the {@code theme} context
 * object.
 *
 * <p>Run it with {@code gradlew run}. The QML entry point is the classpath resource
 * {@code main.qml} (the Gradle build makes the project's {@code qml/} directory a
 * resources root, so {@code qml/Theme.qml} is {@code "Theme.qml"} and {@code qml/qmldir}
 * is {@code "qmldir"}).
 *
 * <p>Optional flags:
 * <ul>
 *   <li>{@code --quit-after=<ms>} - close the window after N milliseconds (scripted
 *       smoke runs and screenshots).</li>
 *   <li>{@code --title=<text>} - override the window title.</li>
 *   <li>{@code --dark} - start in the dark scheme instead of light.</li>
 *   <li>{@code --seed=0xAARRGGBB} - override the default M3 baseline seed.</li>
 *   <li>{@code --toggle-after=<ms>} - call {@code theme.toggleDark()} once after N ms
 *       (deterministic proof that a host-side scheme flip repaints QML).</li>
 * </ul>
 */
public final class Main {

    private static final String DEFAULT_TITLE = "Selkie Hub - Stage 1b";
    private static final String QML_ENTRY = "main.qml";
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 720;
    private static final long FRAME_BUDGET_NANOS = 16_666_667L;

    /** Swatches the demo draws; the host re-reads each resolved color as proof. */
    private static final List<String> PROOF_ROLES = List.of(
            "primary", "primaryContainer", "secondary", "secondaryContainer",
            "tertiary", "tertiaryContainer", "error", "errorContainer",
            "surface", "surfaceVariant", "inverseSurface", "surfaceContainerHighest");

    /** Set by --trace-resources: logs every resource lookup qml4j makes. */
    private static boolean traceResources;

    private static final List<String> PROOF_SPRINGS = List.of(
            "fastSpatial", "defaultSpatial", "slowSpatial",
            "fastEffects", "defaultEffects", "slowEffects");

    private Main() {
    }

    public static void main(String[] args) {
        String title = flag(args, "--title=", DEFAULT_TITLE);
        long quitAfterMs = parseLong(flag(args, "--quit-after=", "0"), 0L);
        long toggleAfterMs = parseLong(flag(args, "--toggle-after=", "0"), 0L);
        boolean startDark = hasFlag(args, "--dark");
        traceResources = hasFlag(args, "--trace-resources");
        int seedArgb = parseInt(flag(args, "--seed=", ""), ThemeBridge.DEFAULT_SEED);

        // Rhino in interpreter mode: the reference qml4j desktop host does the same so
        // QML bindings/handlers behave identically on a stock JVM (no bytecode mode).
        System.setProperty("qml4j.rhino.opt", "-1");

        System.out.println("[selkie] starting host: java " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");

        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);

        ThemeBridge theme = new ThemeBridge(seedArgb, startDark);

        long window = MemoryUtil.NULL;
        DirectContext directContext = null;
        SkijaGlBackend backend = null;
        QmlView view = null;
        try {
            if (!GLFW.glfwInit()) {
                throw new IllegalStateException("glfwInit() failed - GLFW cannot start on this "
                        + "session (headless / no window station / no display)");
            }
            System.out.println("[selkie] GLFW " + GLFW.glfwGetVersionString());

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 8);
            // Keep the window above the user's other windows: acceptance is verified by
            // capturing this window only, so it must not be hidden behind them.
            GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);

            window = GLFW.glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT, title,
                    MemoryUtil.NULL, MemoryUtil.NULL);
            if (window == MemoryUtil.NULL) {
                throw new IllegalStateException("glfwCreateWindow() failed - no usable OpenGL "
                        + "3.2 core context on this session");
            }

            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            // A Windows driver waiting for vblank inside SwapBuffers can stall after
            // iconification; the loop paces frames in software instead.
            GLFW.glfwSwapInterval(0);

            String glVersion = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MAJOR)
                    + "." + GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MINOR);
            System.out.println("[selkie] OpenGL context " + glVersion
                    + " renderer=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER)
                    + " version=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));

            directContext = DirectContext.makeGL();
            backend = new SkijaGlBackend(window, directContext);

            int[] fbWidth = new int[1];
            int[] fbHeight = new int[1];
            GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
            backend.init(fbWidth[0], fbHeight[0]);
            System.out.println("[selkie] Skija surface " + fbWidth[0] + "x" + fbHeight[0]);

            String source = new String(requireClasspath(QML_ENTRY), StandardCharsets.UTF_8);
            view = QmlView.withStockTypes(new QmlEngine());
            view.resources(Main::readClasspath);
            // The theme bridge must be registered BEFORE load(): qml4j's compiler rejects
            // an undeclared free identifier, and Theme.qml binds to "theme" eagerly.
            view.context("theme", theme);
            // QmlView.load(String source, String path): the FIRST argument is the QML
            // source text that must be parsed, the second is the unit path.
            //
            // The unit path must be "" for a classpath-rooted scene. qml4j resolves
            // "import '.'" by joining the unit path with "qmldir" (Loader.joinPath), so
            // passing "main.qml" makes it probe "main.qml/qmldir" and the Theme singleton
            // is never found ("unknown identifier: Theme"). With "" the probes are exactly
            // "qmldir" and "Theme.qml" - both served by the classpath ResourceLoader.
            view.load(source, "");
            System.out.println("[selkie] loaded QML entry '" + QML_ENTRY + "' ("
                    + source.length() + " chars) from the classpath; root=" + view.root());

            // First frame before showing the window: no white flash, and the window
            // is already showing content when the OS maps it.
            renderOnce(view, backend);
            reportThemeProof(view, theme);

            GLFW.glfwShowWindow(window);
            GLFW.glfwFocusWindow(window);
            System.out.println("[selkie] window shown: " + title);
            System.out.println("[selkie] READY");

            runLoop(view, backend, window, quitAfterMs, toggleAfterMs, theme);

            System.out.println("[selkie] loop finished after "
                    + (quitAfterMs > 0 ? quitAfterMs + " ms (--quit-after)" : "window close"));
        } catch (Throwable error) {
            System.err.println("[selkie] FATAL: " + error);
            error.printStackTrace(System.err);
            System.exit(2);
        } finally {
            if (view != null) view.dispose();
            if (backend != null) backend.dispose();
            if (directContext != null) directContext.close();
            if (window != MemoryUtil.NULL) {
                GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);
                GLFW.glfwDestroyWindow(window);
            }
            GLFW.glfwTerminate();
            GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
            if (callback != null) callback.free();
            System.out.println("[selkie] stopped cleanly");
        }
    }

    private static void runLoop(QmlView view, SkijaGlBackend backend, long window,
                                long quitAfterMs, long toggleAfterMs, ThemeBridge theme) {
        long startNanos = System.nanoTime();
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        long frames = 0L;
        boolean toggled = false;
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
            if (fbWidth[0] > 0 && fbHeight[0] > 0) {
                backend.resize(fbWidth[0], fbHeight[0]);
                view.renderFrame(backend);
                frames++;
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (toggleAfterMs > 0 && !toggled && elapsedMs >= toggleAfterMs) {
                toggled = true;
                theme.toggleDark();
                System.out.println("[selkie] toggleDark() after " + elapsedMs + " ms -> dark="
                        + theme.dark.peek());
                // Flush the queued binding re-evaluation, then re-read the live QML state.
                view.renderFrame(backend);
                reportThemeProof(view, theme);
            }
            if (quitAfterMs > 0 && elapsedMs >= quitAfterMs) {
                System.out.println("[selkie] rendered " + frames + " frames");
                return;
            }
            long remaining = FRAME_BUDGET_NANOS - ((System.nanoTime() - startNanos) % FRAME_BUDGET_NANOS);
            try {
                Thread.sleep(Math.max(1L, remaining / 1_000_000L));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void renderOnce(QmlView view, SkijaGlBackend backend) {
        view.renderFrame(backend);
    }

    /**
     * Proof block: compares what the running QML scene actually holds against what
     * {@link Md3eThemeGenerator} and {@link SpringBridge} say the values must be.
     * Written to stdout so a scripted run can diff it; never throws on a MISSING item
     * (it prints {@code <missing>} instead).
     */
    private static void reportThemeProof(QmlView view, ThemeBridge theme) {
        ColorScheme scheme = Md3eThemeGenerator.generate(theme.seed.peek(), theme.dark.peek());
        System.out.println("[proof] scheme seed=" + ColorScheme.hex(theme.seed.peek())
                + " dark=" + theme.dark.peek() + " roles=" + scheme.roles().size());

        Text schemeText = textItem(view, "txt-scheme");
        if (schemeText != null) {
            System.out.println("[proof] txt-scheme = " + schemeText.text.peek());
        }

        int matches = 0;
        for (String role : PROOF_ROLES) {
            Rectangle swatch = rectangleItem(view, "swatch-" + role);
            String actual = swatch == null ? "<missing>" : swatch.color.peek();
            String expected = ColorScheme.hex(scheme.role(role));
            boolean ok = expected.equalsIgnoreCase(actual);
            if (ok) matches++;
            System.out.println("[proof] swatch " + pad(role, 24) + " expected=" + expected
                    + " qmlColor=" + actual + " " + (ok ? "MATCH" : "MISMATCH"));
        }
        System.out.println("[proof] swatch matches " + matches + "/" + PROOF_ROLES.size());

        int springMatches = 0;
        for (String token : PROOF_SPRINGS) {
            Text text = textItem(view, "spring-" + token);
            String rendered = text == null ? "<missing>" : text.text.peek();
            String expected = expectedSpringText(token);
            boolean ok = expected.equals(rendered);
            if (ok) springMatches++;
            System.out.println("[proof] spring " + pad(token, 16)
                    + " java=[" + expected + "] ui=[" + rendered + "] " + (ok ? "MATCH" : "MISMATCH"));
        }
        System.out.println("[proof] spring matches " + springMatches + "/" + PROOF_SPRINGS.size());
    }

    /**
     * Rebuilds the expected spring line straight from {@link MotionTokens} constants, so
     * the comparison proves MotionTokens -&gt; SpringBridge -&gt; Theme.qml -&gt; rendered Text
     * end to end rather than trusting the bridge's own formatting.
     */
    private static String expectedSpringText(String token) {
        MotionTokens.SpringSpec spec = switch (token) {
            case "fastSpatial" -> MotionTokens.FAST_SPATIAL_SPEC;
            case "defaultSpatial" -> MotionTokens.DEFAULT_SPATIAL_SPEC;
            case "slowSpatial" -> MotionTokens.SLOW_SPATIAL_SPEC;
            case "fastEffects" -> MotionTokens.FAST_EFFECTS_SPEC;
            case "defaultEffects" -> MotionTokens.DEFAULT_EFFECTS_SPEC;
            case "slowEffects" -> MotionTokens.SLOW_EFFECTS_SPEC;
            default -> throw new IllegalArgumentException("Unknown spring token: " + token);
        };
        return String.format(Locale.ROOT, "%s  damping=%.2f  stiffness=%.1f  mass=%.1f",
                token, spec.dampingRatio(), spec.stiffness(), spec.mass());
    }

    private static Text textItem(QmlView view, String objectName) {
        Item item = view.findByObjectName(objectName);
        return item instanceof Text text ? text : null;
    }

    private static Rectangle rectangleItem(QmlView view, String objectName) {
        Item item = view.findByObjectName(objectName);
        return item instanceof Rectangle rectangle ? rectangle : null;
    }

    private static String pad(String value, int width) {
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < width) padded.append(' ');
        return padded.toString();
    }

    /**
     * qml4j's {@code ResourceLoader} contract: a missing resource is reported by
     * returning {@code null}, never by throwing. The loader probes resources it does
     * not own (e.g. {@code QtQuick/qmldir}, which the engine synthesises itself), so
     * throwing here aborts QML loading.
     */
    private static byte[] readClasspath(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream(normalized)) {
            if (traceResources) {
                System.out.println("[res] " + path + " -> " + (stream == null ? "null" : "hit"));
            }
            if (stream == null) return null;
            return stream.readAllBytes();
        } catch (IOException error) {
            System.err.println("[selkie] cannot read classpath resource '" + path + "': " + error);
            return null;
        }
    }

    /** For resources this host knows must exist (the QML entry point): fail loudly. */
    private static byte[] requireClasspath(String path) {
        byte[] bytes = readClasspath(path);
        if (bytes == null) {
            throw new IllegalStateException("Required classpath resource is missing: " + path);
        }
        return bytes;
    }

    private static boolean hasFlag(String[] args, String name) {
        for (String arg : args) {
            if (arg.equals(name)) return true;
        }
        return false;
    }

    private static String flag(String[] args, String prefix, String fallback) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) return arg.substring(prefix.length());
        }
        return fallback;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            String trimmed = value.trim();
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return (int) Long.parseLong(trimmed.substring(2), 16);
            }
            return (int) Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
