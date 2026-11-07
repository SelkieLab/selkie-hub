package selkie.host;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.timer_err.qml4j.render.SurfaceBackend;
import org.lwjgl.glfw.GLFW;

/**
 * Skija {@link SurfaceBackend} over a GLFW OpenGL context: qml4j renders into the
 * window's default framebuffer through a Skija {@code DirectContext}.
 *
 * <p>Every method must be called on the thread that made the GL context current
 * (the host render loop thread). This is a deliberately minimal port of the
 * qml4j desktop demo's {@code GlfwSurfaceBackend}.
 */
public final class SkijaGlBackend implements SurfaceBackend {

    private final long window;
    private final DirectContext context;
    private BackendRenderTarget target;
    private Surface surface;
    private int width;
    private int height;
    private boolean disposed;

    public SkijaGlBackend(long window, DirectContext context) {
        this.window = window;
        this.context = context;
    }

    @Override
    public void init(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Surface size must be positive: " + w + "x" + h);
        }
        this.width = w;
        this.height = h;
        rebuildSurface();
    }

    @Override
    public Canvas acquireCanvas() {
        // Clear through Skija, not glClear: Skija's DirectContext cannot see a raw
        // glClear, which drops the frame's first draw and leaves black.
        Canvas canvas = surface.getCanvas();
        canvas.clear(0xFF000000);
        return canvas;
    }

    @Override
    public DirectContext recordingContext() {
        return context;
    }

    @Override
    public void present() {
        // Submit pending Skia work before handing the back buffer to GLFW.
        context.flushAndSubmit(surface);
        GLFW.glfwSwapBuffers(window);
    }

    @Override
    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (w == width && h == height) return;
        width = w;
        height = h;
        rebuildSurface();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (target != null) {
            target.close();
            target = null;
        }
    }

    private void rebuildSurface() {
        if (surface != null) surface.close();
        if (target != null) target.close();
        target = BackendRenderTarget.makeGL(width, height, 0, 8, 0, FramebufferFormat.GR_GL_RGBA8);
        surface = Surface.wrapBackendRenderTarget(
                context,
                target,
                SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888,
                ColorSpace.getSRGB());
    }
}
