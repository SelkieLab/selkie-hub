package selkie.host;

import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.MouseEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Translates GLFW input into qml4j dispatch calls.
 *
 * <p>qml4j requires every dispatch to happen on the thread that owns the {@link QmlView}
 * (the render thread). GLFW callbacks are registered here and only <b>enqueue</b>; the
 * frame loop calls {@link #drain(QmlView, float)} at the start of every frame, on the
 * render thread, and the events are replayed in their original order.
 *
 * <p>Timestamps are the raw monotonic clock sampled when the event arrived
 * ({@code System.nanoTime()}) - the same clock {@code tickAnimations} uses. They are never
 * synthesised or re-based, so qml4j's tap/long-press/drag thresholds see real elapsed time.
 *
 * <p>Pointer coordinates are converted from window pixels to logical QML units by
 * dividing by the framebuffer/window scale (1.0 on a non-HiDPI desktop, and the QML root is
 * sized {@code framebuffer / scale}, so hit-testing lines up).
 */
final class InputDispatcher {

    private enum Kind { MOVE, PRESS, RELEASE, WHEEL, KEY }

    private record Event(Kind kind, float x, float y, float dx, float dy,
                         int button, int key, String text, boolean down, boolean shift,
                         long timeNanos) {
    }

    /** Set by --trace-input: logs every dispatched pointer/key event. */
    static boolean trace;

    private final Queue<Event> queue = new ConcurrentLinkedQueue<>();

    /** Cursor position in window pixels; written by the GLFW callback, read on click/scroll. */
    private volatile float cursorX;
    private volatile float cursorY;

    /** Last GLFW window handle, for synthetic (scripted) events. */
    private long injectedEvents;

    void install(long window) {
        GLFW.glfwSetCursorPosCallback(window, (handle, x, y) -> {
            cursorX = (float) x;
            cursorY = (float) y;
            queue.add(new Event(Kind.MOVE, (float) x, (float) y, 0, 0, 0, 0, null,
                    false, false, System.nanoTime()));
        });
        GLFW.glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
            if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) return;
            queue.add(new Event(action == GLFW.GLFW_PRESS ? Kind.PRESS : Kind.RELEASE,
                    cursorX, cursorY, 0, 0, mapButton(button), 0, null,
                    action == GLFW.GLFW_PRESS, false, System.nanoTime()));
        });
        GLFW.glfwSetScrollCallback(window, (handle, dx, dy) -> queue.add(
                new Event(Kind.WHEEL, cursorX, cursorY, (float) dx, (float) dy, 0, 0, null,
                        false, false, System.nanoTime())));
        GLFW.glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_RELEASE && mapKey(key, mods) == 0) return;
            boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
            queue.add(new Event(Kind.KEY, cursorX, cursorY, 0, 0, 0, mapKey(key, mods), null,
                    action != GLFW.GLFW_RELEASE, shift, System.nanoTime()));
        });
        GLFW.glfwSetCharCallback(window, (handle, codepoint) -> {
            String text = new String(Character.toChars(codepoint));
            queue.add(new Event(Kind.KEY, cursorX, cursorY, 0, 0, 0, 0, text,
                    true, false, System.nanoTime()));
        });
    }

    /** Scripted pointer: move first, then press and release - the shape a real click has. */
    void injectClick(float x, float y) {
        cursorX = x;
        cursorY = y;
        injectedEvents += 3;
        long now = System.nanoTime();
        queue.add(new Event(Kind.MOVE, x, y, 0, 0, 0, 0, null, false, false, now));
        queue.add(new Event(Kind.PRESS, x, y, 0, 0, MouseEvent.LEFT_BUTTON, 0, null, true, false, now));
        queue.add(new Event(Kind.RELEASE, x, y, 0, 0, MouseEvent.LEFT_BUTTON, 0, null, false, false, now));
    }

    /**
     * Scripted keyboard: one press + release. {@code key} is a {@code QmlView.KEY_*} constant
     * (0 for "plain text"), {@code text} is the character payload for text entry.
     */
    void injectKey(int key, String text, boolean shift) {
        injectedEvents += 2;
        long now = System.nanoTime();
        queue.add(new Event(Kind.KEY, cursorX, cursorY, 0, 0, 0, key, text, true, shift, now));
        queue.add(new Event(Kind.KEY, cursorX, cursorY, 0, 0, 0, key, null, false, shift, now));
    }

    /** Scripted pointer: hover only (used to prove moves do not trigger clicks). */
    void injectMove(float x, float y) {
        cursorX = x;
        cursorY = y;
        injectedEvents += 1;
        queue.add(new Event(Kind.MOVE, x, y, 0, 0, 0, 0, null, false, false, System.nanoTime()));
    }

    /** Reports how many events were injected programmatically (diagnostics). */
    long injectedCount() {
        return injectedEvents;
    }

    /**
     * Replays every queued event into the view, in order, on the calling (render) thread.
     *
     * @return number of events dispatched
     */
    int drain(QmlView view, float pointerScale) {
        int dispatched = 0;
        float scale = pointerScale <= 0f ? 1f : pointerScale;
        Event event;
        while ((event = queue.poll()) != null) {
            float x = event.x() / scale;
            float y = event.y() / scale;
            switch (event.kind()) {
                case MOVE -> view.dispatchPointerMove(x, y, event.timeNanos());
                case PRESS -> {
                    // Focus policy (Stage 5a): a pointer press drops the previous keyboard
                    // focus, and the press itself never ASSIGNS focus. A QML item that wants
                    // focus on click does so explicitly (e.g. TextInput's own handler).
                    view.clearFocus();
                    view.dispatchPointerDown(x, y, event.button(), event.timeNanos());
                }
                case RELEASE -> view.dispatchPointerUp(x, y, event.button(), event.timeNanos());
                case WHEEL -> view.dispatchWheel(x, y, event.dx(), event.dy());
                case KEY -> view.dispatchKey(event.key(), event.text(), event.down(), event.shift());
            }
            if (trace && event.kind() != Kind.MOVE) {
                System.out.println("[input] dispatch " + event.kind() + " at "
                        + String.format(java.util.Locale.ROOT, "%.1f,%.1f", x, y)
                        + (event.kind() == Kind.KEY ? " key=" + event.key() + " text=" + event.text() : "")
                        + " t=" + event.timeNanos());
            }
            dispatched++;
        }
        return dispatched;
    }

    int pending() {
        return queue.size();
    }

    private static int mapButton(int glfwButton) {
        return switch (glfwButton) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> MouseEvent.RIGHT_BUTTON;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MouseEvent.MIDDLE_BUTTON;
            default -> MouseEvent.LEFT_BUTTON;
        };
    }

    /** GLFW key -&gt; qml4j key code (0 = "not a key code, text arrives via the char callback"). */
    private static int mapKey(int glfwKey, int mods) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_BACKSPACE -> QmlView.KEY_BACKSPACE;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> QmlView.KEY_ENTER;
            case GLFW.GLFW_KEY_LEFT -> QmlView.KEY_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> QmlView.KEY_RIGHT;
            case GLFW.GLFW_KEY_HOME -> QmlView.KEY_HOME;
            case GLFW.GLFW_KEY_END -> QmlView.KEY_END;
            case GLFW.GLFW_KEY_UP -> QmlView.KEY_UP;
            case GLFW.GLFW_KEY_DOWN -> QmlView.KEY_DOWN;
            case GLFW.GLFW_KEY_ESCAPE -> QmlView.KEY_ESCAPE;
            case GLFW.GLFW_KEY_TAB -> (mods & GLFW.GLFW_MOD_SHIFT) != 0
                    ? QmlView.KEY_BACKTAB : QmlView.KEY_TAB;
            default -> 0;
        };
    }
}
