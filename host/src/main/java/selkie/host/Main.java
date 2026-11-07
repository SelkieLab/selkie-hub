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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Selkie Hub host: GLFW window + OpenGL 3.2 core context, Skija drawing into its
 * framebuffer, qml4j rendering QML into that surface, plus the three host services QML
 * depends on:
 *
 * <ul>
 *   <li><b>theme</b> - {@link ThemeBridge} as the context object {@code theme} and the
 *       {@code Theme} singleton (qml/Theme.qml);</li>
 *   <li><b>motion</b> - {@link MotionBridge} as {@code motion}: host-integrated MD3E
 *       springs, because qml4j has no SpringAnimation;</li>
 *   <li><b>input</b> - {@link InputDispatcher}: GLFW callbacks enqueue, the render thread
 *       dispatches pointer/key/wheel events into the view;</li>
 *   <li><b>data</b> - {@link PluginBridge}, {@link TokenBridge} and {@link StatusBridge} as
 *       {@code plugins} / {@code tokens} / {@code status}: real files from {@code data/},
 *       hot-reloaded on the render thread, plus the optional embedded {@link DevBackend}
 *       the status bridge polls.</li>
 * </ul>
 *
 * <p>Run with {@code gradlew run}. QML is loaded from the classpath (the Gradle build makes
 * {@code qml/} a resources root); its unit path is {@code ""} so {@code import "."} probes
 * {@code qmldir} / {@code Theme.qml} at the classpath root.
 *
 * <p>Flags (all optional):
 * <ul>
 *   <li>{@code --quit-after=<ms>} - exit after N ms.</li>
 *   <li>{@code --title=<text>} - window title (default {@code Selkie Hub - Stage 2a}).</li>
 *   <li>{@code --qml=<name>} - alternate QML entry (classpath resource, else a file path).</li>
 *   <li>{@code --dark}, {@code --seed=0xAARRGGBB} - initial scheme.</li>
 *   <li>{@code --toggle-after=<ms>} - call {@code theme.toggleDark()} once after N ms.</li>
 *   <li>{@code --move=x,y} - scripted pointer move on the first frame.</li>
 *   <li>{@code --click=x,y [--click-after=<ms>]} - scripted click (press+release).</li>
 *   <li>{@code --click-item=<objectName> [--click-after=<ms>]} - scripted click on an item's centre;</li>
 *   <li>{@code --keys=Tab@1000,Tab@1400,Enter@1800} - a timed key sequence (Tab, ShiftTab,
 *       Enter, Escape, Space, Up/Down/Left/Right, or a single character).</li>
 *   <li>{@code --clicks=<objectName>@<ms>,...} - a timed click sequence, so an acceptance run can
 *       walk several pages of one process (e.g. {@code --clicks=nav-1@3000,nav-3@9000}).</li>
 *   <li>{@code --resize=WxH [--resize-after=<ms>]} - resize the OS window.</li>
 *   <li>{@code --motion-trace} - print every animating spring channel each frame.</li>
 *   <li>{@code --trace-resources} - print every qml4j resource lookup.</li>
 *   <li>{@code --dev-backend=<port>} - start the embedded dev backend (JDK HttpServer,
 *       JSON) on that port and poll it for preflight/plugin health;</li>
 *   <li>{@code --data-root=<path>} - read the fixtures from this directory instead of the
 *       detected {@code data/}.</li>
 *   <li>{@code --ignore-close-requests} - log WM_CLOSE but keep the loop running. Acceptance
 *       runs use it on hosts where another window manager/watcher closes our window, which
 *       would otherwise end a scripted run before its last screenshot.</li>
 * </ul>
 */
public final class Main {

    private static final String DEFAULT_TITLE = "Selkie Hub - Stage 2a";
    private static final String DEFAULT_QML = "main.qml";
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 720;
    private static final long FRAME_BUDGET_NANOS = 16_666_667L;

    /** Port the dev backend listens on when {@code --dev-backend} names none. */
    private static final int DEFAULT_BACKEND_PORT = 7071;

    /**
     * Endpoint polled when no dev backend is started: the host still polls, finds nothing
     * listening and degrades to "backend unreachable" - which is the point of the
     * no-backend acceptance run.
     */
    private static final String DEFAULT_BACKEND_ENDPOINT = "http://127.0.0.1:" + DEFAULT_BACKEND_PORT;

    /** Swatches the Stage-1 demo scene draws; the host re-reads each resolved color. */
    private static final List<String> PROOF_ROLES = List.of(
            "primary", "primaryContainer", "secondary", "secondaryContainer",
            "tertiary", "tertiaryContainer", "error", "errorContainer",
            "surface", "surfaceVariant", "inverseSurface", "surfaceContainerHighest");

    private static final List<String> PROOF_SPRINGS = List.of(
            "fastSpatial", "defaultSpatial", "slowSpatial",
            "fastEffects", "defaultEffects", "slowEffects");

    /** Set by --trace-resources: logs every resource lookup qml4j makes. */
    private static boolean traceResources;

    /** Set by --ignore-close-requests: a WM_CLOSE is logged but does not end the loop. */
    private static boolean ignoreCloseRequests;

    private Main() {
    }

    /** One entry of {@code --clicks}: click the item named {@code objectName} at {@code atMs}. */
    private record ScriptedClick(String objectName, long atMs) {
    }

    /** One entry of {@code --keys}: press {@code name} (Tab/ShiftTab/Enter/...) at {@code atMs}. */
    private record ScriptedKey(String name, long atMs) {
    }

    /** Everything the frame loop and the scripted runs need, parsed once from argv. */
    private record Options(String title, String qml, long quitAfterMs, long toggleAfterMs,
                           boolean startDark, int seedArgb, boolean traceMotion,
                           boolean hasMove, float moveX, float moveY,
                           boolean hasClick, float clickX, float clickY, long clickAfterMs,
                           boolean hasResize, int resizeW, int resizeH, long resizeAfterMs,
                           String clickItem, int devBackendPort, String dataRoot,
                           List<ScriptedClick> clicks, List<ScriptedKey> keys) {
    }

    public static void main(String[] args) {
        // Startup phase timing. The packaged build is judged on cold start, so each phase
        // reports its own wall time instead of one opaque number.
        long bootNanos = System.nanoTime();
        Options options = parseOptions(args);
        traceResources = hasFlag(args, "--trace-resources");
        ignoreCloseRequests = hasFlag(args, "--ignore-close-requests");
        InputDispatcher.trace = hasFlag(args, "--trace-input");
        String title = options.title();
        long quitAfterMs = options.quitAfterMs();
        long toggleAfterMs = options.toggleAfterMs();

        // Rhino in interpreter mode: the reference qml4j desktop host does the same so
        // QML bindings/handlers behave identically on a stock JVM (no bytecode mode).
        System.setProperty("qml4j.rhino.opt", "-1");

        System.out.println("[selkie] starting host: java " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");

        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);

        ThemeBridge theme = new ThemeBridge(options.seedArgb(), options.startDark());
        MotionBridge motion = new MotionBridge();
        InputDispatcher input = new InputDispatcher();

        // --- Stage-4 data layer: real files under data/, no network on the render thread ---
        Path dataRoot = DataPaths.resolve(options.dataRoot());
        PluginBridge plugins = new PluginBridge(dataRoot);
        TokenBridge tokens = new TokenBridge(dataRoot);
        DevBackend devBackend = null;
        String endpoint = DEFAULT_BACKEND_ENDPOINT;
        if (options.devBackendPort() > 0) {
            try {
                devBackend = DevBackend.start(options.devBackendPort(), dataRoot);
                endpoint = devBackend.baseUrl();
                System.out.println("[dev-backend] serving " + endpoint + " from " + dataRoot);
            } catch (IOException error) {
                System.err.println("[dev-backend] cannot listen on port "
                        + options.devBackendPort() + ": " + error
                        + " - the UI will report the preflight as unreachable");
            }
        } else {
            System.out.println("[dev-backend] not started (no --dev-backend); polling " + endpoint
                    + " anyway, so the UI must degrade to \"" + StatusBridge.UNREACHABLE + "\"");
        }
        StatusBridge status = new StatusBridge(new BackendPoller(endpoint));
        DataLayer data = new DataLayer(plugins, tokens, status);

        long window = MemoryUtil.NULL;
        DirectContext directContext = null;
        SkijaGlBackend backend = null;
        QmlView view = null;
        try {
            long afterDataNanos = System.nanoTime();
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
            input.install(window);
            // GLFW sets the close flag before invoking this callback, so clearing it here is
            // what vetoes the request. Logged either way: a spurious WM_CLOSE from another
            // process is otherwise indistinguishable from a user closing the window.
            GLFW.glfwSetWindowCloseCallback(window, handle -> {
                System.out.println("[selkie] window close request"
                        + (ignoreCloseRequests ? " ignored (--ignore-close-requests)" : " accepted"));
                if (ignoreCloseRequests) {
                    GLFW.glfwSetWindowShouldClose(handle, false);
                }
            });

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

            long afterGlfwNanos = System.nanoTime();
            directContext = DirectContext.makeGL();
            backend = new SkijaGlBackend(window, directContext);

            int[] fbWidth = new int[1];
            int[] fbHeight = new int[1];
            GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
            backend.init(fbWidth[0], fbHeight[0]);
            System.out.println("[selkie] Skija surface " + fbWidth[0] + "x" + fbHeight[0]);

            String source = loadQmlSource(options.qml());
            view = QmlView.withStockTypes(new QmlEngine());
            view.resources(Main::readClasspath);
            // Both bridges must be registered BEFORE load(): qml4j's compiler rejects an
            // undeclared free identifier, and Theme.qml binds to "theme" eagerly.
            view.context("theme", theme);
            view.context("motion", motion);
            // Stage-4 data bridges: QML resolves plugins/tokens/status as free identifiers,
            // so they must exist before load() just like theme/motion.
            view.context("plugins", plugins);
            view.context("tokens", tokens);
            view.context("status", status);
            // QmlView.load(String source, String path): the FIRST argument is the QML
            // source text that must be parsed, the second is the unit path.
            //
            // The unit path must be "" for a classpath-rooted scene. qml4j resolves
            // "import '.'" by joining the unit path with "qmldir" (Loader.joinPath), so
            // passing "main.qml" makes it probe "main.qml/qmldir" and the Theme singleton
            // is never found ("unknown identifier: Theme"). With "" the probes are exactly
            // "qmldir" and "Theme.qml" - both served by the classpath ResourceLoader.
            long afterSkijaNanos = System.nanoTime();
            view.load(source, "");
            System.out.println("[selkie] loaded QML entry '" + options.qml() + "' ("
                    + source.length() + " chars); root=" + view.root());

            // Focus policy evidence: every focus change prints one [focus] line with the
            // objectName of the item gaining focus (and the one losing it).
            view.setFocusListener(Main::reportFocusChange);

            // Size the QML root to the framebuffer so narrow layouts can react to resizes.
            applyRootSize(view, fbWidth[0], fbHeight[0], pointerScale(window, fbWidth[0]));

            renderOnce(view, backend);
            reportThemeProof(view, theme);
            // What the QML bindings were handed at load time (revisions included).
            data.report();

            GLFW.glfwShowWindow(window);
            GLFW.glfwFocusWindow(window);
            System.out.println("[selkie] window shown: " + title);
            long readyNanos = System.nanoTime();
            System.out.println(String.format(java.util.Locale.ROOT,
                    "[boot] data+jvm=%dms glfw=%dms skija=%dms qml=%dms total=%dms",
                    (afterDataNanos - bootNanos) / 1_000_000,
                    (afterGlfwNanos - afterDataNanos) / 1_000_000,
                    (afterSkijaNanos - afterGlfwNanos) / 1_000_000,
                    (readyNanos - afterSkijaNanos) / 1_000_000,
                    (readyNanos - bootNanos) / 1_000_000));
            System.out.println("[selkie] READY");

            runLoop(view, backend, window, options, theme, motion, input, data);

            System.out.println("[selkie] loop finished after "
                    + (quitAfterMs > 0 ? quitAfterMs + " ms (--quit-after)" : "window close"));
            // Final state of the data layer, including every revision bumped by hot reload.
            data.report();
        } catch (Throwable error) {
            System.err.println("[selkie] FATAL: " + error);
            error.printStackTrace(System.err);
            System.exit(2);
        } finally {
            data.close();
            if (devBackend != null) devBackend.close();
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

    private static void runLoop(QmlView view, SkijaGlBackend backend, long window, Options options,
                                ThemeBridge theme, MotionBridge motion, InputDispatcher input,
                                DataLayer data) {
        long startNanos = System.nanoTime();
        long lastNanos = startNanos;
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        int[] winWidth = new int[1];
        int[] winHeight = new int[1];
        long frames = 0L;
        boolean toggled = false;
        boolean reportedToggle = false;
        boolean clicked = false;
        int nextScriptedClick = 0;
        int nextScriptedKey = 0;
        boolean moved = false;
        boolean resized = false;

        int lastFbW = backend.width();
        int lastFbH = backend.height();
        // Recorded so the acceptance log says *why* a run stopped (a closed window looks
        // identical to an expired --quit-after in the old single summary line).
        String exitReason = "window close requested";

        while (!GLFW.glfwWindowShouldClose(window)) {
            // GLFW callbacks fire inside pollEvents; they only enqueue. Everything is
            // dispatched below, on this thread.
            GLFW.glfwPollEvents();

            // Stage-4 data layer, once per frame on the render thread: stat the manifests and
            // tokens.json (re-read only when they changed) and drain whatever the HTTP poller
            // queued. This is what makes an on-disk edit show up without a restart.
            data.tick();

            long now = System.nanoTime();
            long deltaNanos = now - lastNanos;
            lastNanos = now;
            long elapsedMs = (now - startNanos) / 1_000_000L;

            GLFW.glfwGetFramebufferSize(window, fbWidth, fbHeight);
            GLFW.glfwGetWindowSize(window, winWidth, winHeight);
            float scale = winWidth[0] > 0 ? fbWidth[0] / (float) winWidth[0] : 1f;
            if (fbWidth[0] == 0 || fbHeight[0] == 0) {
                // Minimised/iconified windows report a 0x0 framebuffer on Windows. Restore
                // instead of collapsing the QML root to 0x0 (scripted runs must not be
                // derailed by another window taking the foreground).
                GLFW.glfwRestoreWindow(window);
                GLFW.glfwFocusWindow(window);
                lastNanos = System.nanoTime();
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    exitReason = "interrupted while waiting for the window to un-minimise";
                    break;
                }
                continue;
            }
            if (fbWidth[0] != lastFbW || fbHeight[0] != lastFbH) {
                lastFbW = fbWidth[0];
                lastFbH = fbHeight[0];
                backend.resize(fbWidth[0], fbHeight[0]);
                applyRootSize(view, fbWidth[0], fbHeight[0], scale);
                System.out.println("[selkie] resize -> framebuffer " + fbWidth[0] + "x" + fbHeight[0]
                        + " (window " + winWidth[0] + "x" + winHeight[0]
                        + ", root " + fmt(view.root().width.peek()) + "x"
                        + fmt(view.root().height.peek()) + ")");
            }

            // --- scripted input (deterministic acceptance runs) ---
            if (options.hasMove() && !moved) {
                moved = true;
                input.injectMove(options.moveX(), options.moveY());
                System.out.println("[input] scripted move -> " + options.moveX() + "," + options.moveY());
            }
            if (options.clickItem() != null && !clicked && elapsedMs >= options.clickAfterMs()) {
                clicked = true;
                Item target = view.findByObjectName(options.clickItem());
                if (target == null) {
                    System.err.println("[input] --click-item: no item named '" + options.clickItem() + "'");
                } else {
                    float[] center = absoluteCenter(target);
                    input.injectClick(center[0], center[1]);
                    System.out.println("[input] scripted click on '" + options.clickItem() + "'' centre -> "
                            + String.format(Locale.ROOT, "%.1f,%.1f", center[0], center[1])
                            + " at " + elapsedMs + " ms");
                }
            }
            if (nextScriptedClick < options.clicks().size()
                    && elapsedMs >= options.clicks().get(nextScriptedClick).atMs()) {
                ScriptedClick scripted = options.clicks().get(nextScriptedClick);
                nextScriptedClick++;
                Item target = view.findByObjectName(scripted.objectName());
                if (target == null) {
                    System.err.println("[input] --clicks: no item named '" + scripted.objectName() + "'");
                } else {
                    float[] center = absoluteCenter(target);
                    input.injectClick(center[0], center[1]);
                    System.out.println("[input] scripted click #" + nextScriptedClick + " on '"
                            + scripted.objectName() + "' -> "
                            + String.format(Locale.ROOT, "%.1f,%.1f", center[0], center[1])
                            + " at " + elapsedMs + " ms");
                }
            }
            if (nextScriptedKey < options.keys().size()
                    && elapsedMs >= options.keys().get(nextScriptedKey).atMs()) {
                ScriptedKey key = options.keys().get(nextScriptedKey);
                nextScriptedKey++;
                int code = keyCode(key.name());
                String text = keyText(key.name());
                boolean shift = keyShift(key.name());
                input.injectKey(code, text, shift);
                System.out.println("[input] scripted key #" + nextScriptedKey + " '" + key.name()
                        + "' (code=" + code + ", text=" + (text == null ? "null" : "'" + text + "'")
                        + ", shift=" + shift + ") at " + elapsedMs + " ms");
            }
            if (options.hasClick() && !clicked && elapsedMs >= options.clickAfterMs()) {
                clicked = true;
                input.injectClick(options.clickX(), options.clickY());
                System.out.println("[input] scripted click -> " + options.clickX() + "," + options.clickY()
                        + " at " + elapsedMs + " ms");
            }
            if (options.hasResize() && !resized && elapsedMs >= options.resizeAfterMs()) {
                resized = true;
                GLFW.glfwSetWindowSize(window, options.resizeW(), options.resizeH());
                System.out.println("[input] scripted resize -> " + options.resizeW() + "x"
                        + options.resizeH() + " at " + elapsedMs + " ms");
            }
            if (options.toggleAfterMs() > 0 && !toggled && elapsedMs >= options.toggleAfterMs()) {
                toggled = true;
                theme.toggleDark();
                System.out.println("[selkie] toggleDark() after " + elapsedMs + " ms -> dark="
                        + theme.dark.peek());
            }

            // --- dispatch queued input, then advance host springs ---
            int dispatched = input.drain(view, scale);
            motion.update(deltaNanos);

            if (options.traceMotion()) {
                traceMotion(view, motion, elapsedMs);
            }
            // Overlays/pages animate whether or not --motion-trace is on; the trace only
            // decides whether the per-frame numbers are printed.
            if (toggled && !reportedToggle) {
                reportedToggle = true;
                view.renderFrame(backend);
                reportThemeProof(view, theme);
            }

            if (fbWidth[0] > 0 && fbHeight[0] > 0) {
                backend.resize(fbWidth[0], fbHeight[0]);
                view.renderFrame(backend);
                frames++;
            }

            if (options.quitAfterMs() > 0 && elapsedMs >= options.quitAfterMs()) {
                System.out.println("[selkie] rendered " + frames + " frames, dispatched "
                        + dispatched + " input event(s) this frame, injected "
                        + input.injectedCount() + " scripted event(s), motion channels="
                        + motion.channelCount());
                if (options.traceMotion()) {
                    for (SpringChannel channel : motion.allChannels()) {
                        System.out.println("[motion] final " + channel.tokenName() + "#"
                                + channel.serial() + "="
                                + String.format(Locale.ROOT, "%.5f", channel.value.peek()));
                    }
                }
                exitReason = "--quit-after=" + options.quitAfterMs();
                break;
            }
            long remaining = FRAME_BUDGET_NANOS - ((System.nanoTime() - startNanos) % FRAME_BUDGET_NANOS);
            try {
                Thread.sleep(Math.max(1L, remaining / 1_000_000L));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                exitReason = "interrupted during frame pacing";
                break;
            }
        }
        System.out.println("[selkie] loop exit: " + exitReason + " after " + frames
                + " frame(s), " + (System.nanoTime() - startNanos) / 1_000_000L + " ms");
    }

    /**
     * The three Stage-4 data bridges, created once and ticked once per frame on the render
     * thread.
     *
     * <p>Order matters: {@link StatusBridge#tick()} drains whatever the HTTP worker queued,
     * and the newest health map is then pushed into {@link PluginBridge} so the plugin rows
     * carry the backend's verdict. Nothing here does network I/O itself.
     */
    private record DataLayer(PluginBridge plugins, TokenBridge tokens, StatusBridge status) {

        /** One frame of data work: file stats, conditional re-reads, queue drains. */
        void tick() {
            plugins.tick();
            tokens.tick();
            Map<String, PluginHealth> health = status.tick();
            if (!health.isEmpty()) {
                plugins.applyHealth(health);
            }
        }

        /** Evidence block: exactly what the QML bindings are holding right now. */
        void report() {
            System.out.println("[data] plugins revision=" + plugins.revision.peek()
                    + " count=" + plugins.count.peek()
                    + " official=" + plugins.officialItems.peek().size()
                    + " thirdParty=" + plugins.thirdPartyItems.peek().size()
                    + " source=" + plugins.source.peek()
                    + " dir=" + plugins.pluginsDirectory());
            StringBuilder detail = new StringBuilder();
            for (PluginInfo plugin : plugins.items.peek()) {
                detail.append(System.lineSeparator()).append("[data]   ").append(plugin.id)
                        .append(" name=\"").append(plugin.name).append("\"")
                        .append(" version=").append(plugin.version)
                        .append(" official=").append(plugin.official)
                        .append(" health=").append(plugin.health)
                        .append(" latency=").append(plugin.latencyMs).append("ms")
                        .append(" provides=\"").append(plugin.providesText).append("\"")
                        .append(" permissions=\"").append(plugin.permissionsText).append("\"")
                        .append(" token_policy=").append(plugin.tokenPolicy);
            }
            if (detail.length() > 0) {
                System.out.println(detail);
            }
            System.out.println("[data] tokens revision=" + tokens.revision.peek()
                    + " source=" + tokens.source.peek()
                    + " used=" + fmt(tokens.used.peek()) + "/" + fmt(tokens.total.peek())
                    + " persona=" + fmt(tokens.persona.peek())
                    + " recall=" + fmt(tokens.recall.peek())
                    + " tools=" + fmt(tokens.tools.peek())
                    + " history=" + fmt(tokens.history.peek())
                    + " file=" + tokens.file());
            System.out.println("[status] preflightOk=" + status.preflightOk.peek()
                    + " text=\"" + status.preflightText.peek() + "\""
                    + " endpoint=" + status.endpointUrl()
                    + " polls=" + status.pollCount.peek());
        }

        /** Stops the background poller; the dev backend is closed by main(). */
        void close() {
            status.close();
        }
    }

    private static void traceMotion(QmlView view, MotionBridge motion, long elapsedMs) {
        StringBuilder line = new StringBuilder("[motion] t=").append(elapsedMs);
        boolean any = false;
        for (SpringChannel channel : motion.allChannels()) {
            if (!channel.running() && Math.abs(channel.velocity()) < 1e-9) continue;
            line.append(' ').append(channel.tokenName()).append('#').append(channel.serial())
                    .append('=').append(String.format(Locale.ROOT, "%.5f", channel.value.peek()));
            any = true;
        }
        for (OverlayChannel overlay : motion.allOverlays()) {
            if (!overlay.isAnimating()) continue;
            line.append(" overlay:").append(overlay.name())
                    .append(" scale=").append(String.format(Locale.ROOT, "%.3f", overlay.scale.peek()))
                    .append(" opacity=").append(String.format(Locale.ROOT, "%.3f", overlay.opacity.peek()))
                    .append(" visible=").append(overlay.visible.peek());
            any = true;
        }
        if (!any) return;
        // The QML-side proof: the marker's resolved x comes from a binding on ch.value,
        // so it lags the channel by at most one frame and shows what was actually drawn.
        Item spatial = view.findByObjectName("spatial-marker");
        Item effects = view.findByObjectName("effects-marker");
        if (spatial != null) {
            line.append(" spatialX=").append(String.format(Locale.ROOT, "%.1f", spatial.x.peek().doubleValue()));
        }
        if (effects != null) {
            line.append(" effectsX=").append(String.format(Locale.ROOT, "%.1f", effects.x.peek().doubleValue()));
        }
        System.out.println(line);
    }

    /**
     * Keeps the QML root the same size as the framebuffer (in logical units), so layouts
     * with breakpoints (e.g. collapse under 600 px) actually see the new width.
     */
    private static void applyRootSize(QmlView view, int framebufferWidth, int framebufferHeight,
                                      float scale) {
        Item root = view.root();
        if (root == null) return;
        float logicalScale = scale <= 0f ? 1f : scale;
        root.width.set((double) (framebufferWidth / logicalScale));
        root.height.set((double) (framebufferHeight / logicalScale));
    }

    /**
     * One line per focus change - the acceptance log for the Tab traversal.
     * qml4j calls {@code onFocusChanged(newFocus, oldFocus)}: the NEW focus is the first arg.
     */
    private static void reportFocusChange(Item newFocus, Item oldFocus) {
        System.out.println("[focus] " + focusName(oldFocus) + " -> " + focusName(newFocus));
    }

    private static String focusName(Item item) {
        if (item == null) return "<none>";
        String name = item.objectName.peek();
        if (name == null || name.isBlank()) return "<" + item.getClass().getSimpleName() + ">";
        return name;
    }

    /** Absolute centre of an item in logical units, by summing the parent chain. */
    private static float[] absoluteCenter(Item item) {
        double x = item.x.peek().doubleValue();
        double y = item.y.peek().doubleValue();
        Item parent = item.parent.peek();
        while (parent != null) {
            x += parent.x.peek().doubleValue();
            y += parent.y.peek().doubleValue();
            parent = parent.parent.peek();
        }
        double width = item.width.peek().doubleValue();
        double height = item.height.peek().doubleValue();
        return new float[] { (float) (x + width / 2), (float) (y + height / 2) };
    }

    private static float pointerScale(long window, int framebufferWidth) {
        int[] winWidth = new int[1];
        int[] winHeight = new int[1];
        GLFW.glfwGetWindowSize(window, winWidth, winHeight);
        return winWidth[0] > 0 ? framebufferWidth / (float) winWidth[0] : 1f;
    }

    private static String fmt(Object value) {
        return value instanceof Number number
                ? String.format(Locale.ROOT, "%.0f", number.doubleValue())
                : String.valueOf(value);
    }

    private static void renderOnce(QmlView view, SkijaGlBackend backend) {
        view.renderFrame(backend);
    }

    // ------------------------------------------------------------------
    // QML loading
    // ------------------------------------------------------------------

    /**
     * Loads the QML entry: a classpath resource first (the shipping path), then a plain file
     * path (handy for scratch scenes that must not live in the repo).
     */
    private static String loadQmlSource(String entry) throws IOException {
        byte[] fromClasspath = readClasspath(entry);
        if (fromClasspath != null) {
            System.out.println("[selkie] QML entry resolved from classpath: " + entry);
            return new String(fromClasspath, StandardCharsets.UTF_8);
        }
        Path path = Path.of(entry);
        if (Files.isReadable(path)) {
            System.out.println("[selkie] QML entry resolved from file: " + path.toAbsolutePath());
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("QML entry not found on the classpath or as a file: " + entry);
    }

    /**
     * Proof block: compares what the running QML scene actually holds against what
     * {@link Md3eThemeGenerator} and {@link SpringBridge} say the values must be. Scenes that
     * do not contain the Stage-1 demo items simply skip it.
     */
    private static void reportThemeProof(QmlView view, ThemeBridge theme) {
        ColorScheme scheme = Md3eThemeGenerator.generate(theme.seed.peek(), theme.dark.peek());
        int found = 0;
        int matches = 0;
        StringBuilder detail = new StringBuilder();
        for (String role : PROOF_ROLES) {
            Rectangle swatch = rectangleItem(view, "swatch-" + role);
            if (swatch == null) continue;
            found++;
            String actual = swatch.color.peek();
            String expected = ColorScheme.hex(scheme.role(role));
            boolean ok = expected.equalsIgnoreCase(actual);
            if (ok) matches++;
            detail.append(System.lineSeparator()).append(String.format(Locale.ROOT,
                    "[proof] swatch %-24s expected=%s qmlColor=%s %s",
                    role, expected, actual, ok ? "MATCH" : "MISMATCH"));
        }
        if (found == 0) {
            System.out.println("[proof] scene has no Stage-1 swatch items - visual theme proof skipped");
        } else {
            System.out.println("[proof] scheme seed=" + ColorScheme.hex(theme.seed.peek())
                    + " dark=" + theme.dark.peek() + " roles=" + scheme.roles().size()
                    + detail);
            System.out.println("[proof] swatch matches " + matches + "/" + found);
        }

        int springFound = 0;
        int springMatches = 0;
        StringBuilder springDetail = new StringBuilder();
        for (String token : PROOF_SPRINGS) {
            Text text = textItem(view, "spring-" + token);
            if (text == null) continue;
            springFound++;
            String rendered = text.text.peek();
            String expected = expectedSpringText(token);
            boolean ok = expected.equals(rendered);
            if (ok) springMatches++;
            springDetail.append(System.lineSeparator()).append(String.format(Locale.ROOT,
                    "[proof] spring %-16s java=[%s] ui=[%s] %s",
                    token, expected, rendered, ok ? "MATCH" : "MISMATCH"));
        }
        if (springFound == 0) {
            System.out.println("[proof] scene has no Stage-1 spring labels - spring label proof skipped");
        } else {
            System.out.println(springDetail);
            System.out.println("[proof] spring matches " + springMatches + "/" + springFound);
        }
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

    // ------------------------------------------------------------------
    // Classpath resources
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Argument parsing
    // ------------------------------------------------------------------

    private static Options parseOptions(String[] args) {
        String title = flag(args, "--title=", DEFAULT_TITLE);
        String qml = flag(args, "--qml=", DEFAULT_QML);
        long quitAfterMs = parseLong(flag(args, "--quit-after=", "0"), 0L);
        long toggleAfterMs = parseLong(flag(args, "--toggle-after=", "0"), 0L);
        boolean startDark = hasFlag(args, "--dark");
        int seedArgb = parseInt(flag(args, "--seed=", ""), ThemeBridge.DEFAULT_SEED);
        boolean traceMotion = hasFlag(args, "--motion-trace");

        float[] move = parsePair(flag(args, "--move=", ""));
        float[] click = parsePair(flag(args, "--click=", ""));
        long clickAfterMs = parseLong(flag(args, "--click-after=", "0"), 0L);
        int[] resize = parseSize(flag(args, "--resize=", ""));
        long resizeAfterMs = parseLong(flag(args, "--resize-after=", "0"), 0L);

        // Stage 4: --dev-backend=<port> (absent/-1 = do not start one) and --data-root=<path>.
        int devBackendPort = parseInt(flag(args, "--dev-backend=", ""), -1);
        String dataRoot = flag(args, "--data-root=", null);

        return new Options(title, qml, quitAfterMs, toggleAfterMs, startDark, seedArgb, traceMotion,
                move != null, move != null ? move[0] : 0f, move != null ? move[1] : 0f,
                click != null, click != null ? click[0] : 0f, click != null ? click[1] : 0f, clickAfterMs,
                resize != null, resize != null ? resize[0] : 0, resize != null ? resize[1] : 0, resizeAfterMs,
                flag(args, "--click-item=", null), devBackendPort, dataRoot,
                parseClicks(flag(args, "--clicks=", "")),
                parseKeys(flag(args, "--keys=", "")));
    }

    /**
     * Parses {@code --clicks=nav-1@3000,nav-3@9000} into an ordered click sequence. A missing
     * {@code @ms} means "as early as possible"; a malformed entry is skipped rather than
     * aborting the run.
     */
    /**
     * Parses {@code --keys=Tab@1000,Tab@1400,Enter@1800} into an ordered key sequence.
     * A missing {@code @ms} means "as early as possible"; malformed entries are skipped.
     */
    private static List<ScriptedKey> parseKeys(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<ScriptedKey> keys = new ArrayList<>();
        for (String chunk : value.split(",")) {
            String entry = chunk.trim();
            if (entry.isEmpty()) continue;
            String name = entry;
            long atMs = 0L;
            int at = entry.lastIndexOf('@');
            if (at > 0) {
                name = entry.substring(0, at).trim();
                atMs = parseLong(entry.substring(at + 1), 0L);
            }
            if (!name.isEmpty()) keys.add(new ScriptedKey(name, atMs));
        }
        keys.sort(Comparator.comparingLong(ScriptedKey::atMs));
        return keys;
    }

    /** {@code --keys} name -&gt; qml4j key code (0 = text payload only). */
    private static int keyCode(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "tab" -> QmlView.KEY_TAB;
            case "shifttab", "shift+tab", "backtab" -> QmlView.KEY_BACKTAB;
            case "enter", "return" -> QmlView.KEY_ENTER;
            case "escape", "esc" -> QmlView.KEY_ESCAPE;
            case "backspace" -> QmlView.KEY_BACKSPACE;
            case "up" -> QmlView.KEY_UP;
            case "down" -> QmlView.KEY_DOWN;
            case "left" -> QmlView.KEY_LEFT;
            case "right" -> QmlView.KEY_RIGHT;
            case "home" -> QmlView.KEY_HOME;
            case "end" -> QmlView.KEY_END;
            default -> 0;
        };
    }

    /** {@code --keys} name -&gt; text payload (Space and single characters). */
    private static String keyText(String name) {
        if (name.equalsIgnoreCase("space")) return " ";
        if (name.length() == 1) return name;
        return null;
    }

    /** Shift+Tab is its own key code; real Shift+Tab also reports shift = true. */
    private static boolean keyShift(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals("shifttab") || lower.equals("shift+tab") || lower.equals("backtab");
    }

    private static List<ScriptedClick> parseClicks(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<ScriptedClick> clicks = new ArrayList<>();
        for (String chunk : value.split(",")) {
            String entry = chunk.trim();
            if (entry.isEmpty()) continue;
            String objectName = entry;
            long atMs = 0L;
            int at = entry.lastIndexOf('@');
            if (at > 0) {
                objectName = entry.substring(0, at).trim();
                atMs = parseLong(entry.substring(at + 1), 0L);
            }
            if (!objectName.isEmpty()) clicks.add(new ScriptedClick(objectName, atMs));
        }
        return List.copyOf(clicks);
    }

    private static float[] parsePair(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split(",");
        if (parts.length != 2) return null;
        try {
            return new float[] { Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()) };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int[] parseSize(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.toLowerCase(Locale.ROOT).split("x");
        if (parts.length != 2) return null;
        try {
            return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
        } catch (NumberFormatException ignored) {
            return null;
        }
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
