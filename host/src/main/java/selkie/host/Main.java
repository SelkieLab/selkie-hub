package selkie.host;

import io.github.humbleui.skija.DirectContext;
import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.render.QmlView;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Stage 0 host: an OS window (GLFW + OpenGL 3.2 core) with Skija drawing into its
 * framebuffer, and qml4j rendering {@code main.qml} into that Skia surface.
 *
 * <p>Run it with {@code gradlew run} (or {@code java -cp <runtimeClasspath> selkie.host.Main}).
 * The QML entry point is the classpath resource {@code main.qml} (the Gradle
 * build makes the project's {@code qml/} directory a resources root).
 *
 * <p>Optional flags:
 * <ul>
 *   <li>{@code --quit-after=<ms>} - close the window after N milliseconds (used for
 *       scripted smoke runs and screenshots).</li>
 *   <li>{@code --title=<text>} - override the window title (a screenshot helper can
 *       then find the window by title).</li>
 * </ul>
 */
public final class Main {

    private static final String DEFAULT_TITLE = "Selkie Hub - Stage 0";
    private static final String QML_ENTRY = "main.qml";
    private static final int INITIAL_WIDTH = 960;
    private static final int INITIAL_HEIGHT = 640;
    private static final long FRAME_BUDGET_NANOS = 16_666_667L;

    private Main() {
    }

    public static void main(String[] args) {
        String title = flag(args, "--title=", DEFAULT_TITLE);
        long quitAfterMs = parseLong(flag(args, "--quit-after=", "0"), 0L);

        // Rhino in interpreter mode: the reference qml4j desktop host does the same so
        // QML bindings/handlers behave identically on a stock JVM (no bytecode mode).
        System.setProperty("qml4j.rhino.opt", "-1");

        System.out.println("[selkie] starting host: java " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");

        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);

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
            // Keep the window above the user's other windows: Stage 0 is verified by
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
            // QmlView.load(String source, String path): the FIRST argument is the QML
            // source text that must be parsed, the second is the unit path used for
            // relative imports, error messages and generated class names.
            view.load(source, QML_ENTRY);
            System.out.println("[selkie] loaded QML entry '" + QML_ENTRY + "' ("
                    + source.length() + " chars) from the classpath; root=" + view.root());

            // First frame before showing the window: no white flash, and the window
            // is already showing content when the OS maps it.
            renderOnce(view, backend);

            GLFW.glfwShowWindow(window);
            GLFW.glfwFocusWindow(window);
            System.out.println("[selkie] window shown: " + title);
            System.out.println("[selkie] READY");

            runLoop(view, backend, window, quitAfterMs);

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

    private static void runLoop(QmlView view, SkijaGlBackend backend, long window, long quitAfterMs) {
        long startNanos = System.nanoTime();
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        long frames = 0L;
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
            if (fbWidth[0] > 0 && fbHeight[0] > 0) {
                backend.resize(fbWidth[0], fbHeight[0]);
                view.renderFrame(backend);
                frames++;
            }
            if (quitAfterMs > 0 && (System.nanoTime() - startNanos) / 1_000_000L >= quitAfterMs) {
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
     * qml4j's {@code ResourceLoader} contract: a missing resource is reported by
     * returning {@code null}, never by throwing. The loader probes resources it does
     * not own (e.g. {@code QtQuick/qmldir}, which the engine synthesises itself), so
     * throwing here aborts QML loading.
     */
    private static byte[] readClasspath(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream(normalized)) {
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
}
