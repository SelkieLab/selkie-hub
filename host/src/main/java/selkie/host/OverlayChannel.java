package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

/**
 * Host-driven enter/exit timeline for an overlay (toast, sheet, popover).
 *
 * <p>qml4j has no controllable animation timing (no SpringAnimation, no usable
 * NumberAnimation sequencing), so the overlay transition is integrated here, on the render
 * thread, and QML only binds to it:
 *
 * <pre>{@code
 * property var toast: motion.overlay("toast")
 * visible: toast.visible
 * opacity: toast.opacity
 * scale: toast.scale
 * }</pre>
 *
 * <p>Timeline (QPlayer spec, fixed - do not tune per call site):
 * <ul>
 *   <li>show: scale {@value #SCALE_FROM} -&gt; {@value #SCALE_TO} over {@value #SHOW_SCALE_MS} ms,
 *       opacity 0 -&gt; 1 over {@value #SHOW_FADE_MS} ms (both start together);</li>
 *   <li>hide: scale 1 -&gt; {@value #SCALE_FROM} and opacity 1 -&gt; 0 over {@value #HIDE_MS} ms,
 *       then {@code visible} flips to false.</li>
 * </ul>
 *
 * <p>Easing is ease-out-cubic (fast start, gentle settle) - a monotone curve, so a test can
 * assert endpoints and ordering without depending on frame timing.
 */
public final class OverlayChannel extends QObject {

    /** Scale at the start of the enter transition. */
    public static final double SCALE_FROM = 0.92;
    /** Scale when fully shown. */
    public static final double SCALE_TO = 1.0;
    /** Enter: share of the timeline spent scaling. */
    public static final long SHOW_SCALE_MS = 200L;
    /** Enter: share of the timeline spent fading in. */
    public static final long SHOW_FADE_MS = 150L;
    /** Exit: scale + fade duration (both run for the whole 150 ms). */
    public static final long HIDE_MS = 150L;

    private enum Phase { HIDDEN, SHOWING, SHOWN, HIDING }

    /** Scale the overlay should be drawn at (0.92 = dismissed, 1.0 = fully shown). */
    public final Property<Double> scale = new Property<>(SCALE_FROM);
    /** Opacity the overlay should be drawn at. */
    public final Property<Double> opacity = new Property<>(0.0);
    /** True while the overlay is part of the scene (immediately on show, false after hide ends). */
    public final Property<Boolean> visible = new Property<>(false);

    private final String name;
    private Phase phase = Phase.HIDDEN;
    private double elapsedMs;

    OverlayChannel(String name) {
        this.name = name;
    }

    /** The name this overlay was created with (diagnostics / logs). */
    public String name() {
        return name;
    }

    /** True while the enter or exit timeline is running. */
    public boolean isAnimating() {
        return phase == Phase.SHOWING || phase == Phase.HIDING;
    }

    /** Begin (or restart) the enter timeline: visible at once, scale 0.92 -&gt; 1, fade in. */
    public void show() {
        phase = Phase.SHOWING;
        elapsedMs = 0.0;
        visible.set(Boolean.TRUE);
        scale.set(SCALE_FROM);
        opacity.set(0.0);
    }

    /** Begin the exit timeline: scale -&gt; 0.92, fade to 0, then visible = false. */
    public void hide() {
        if (phase == Phase.HIDDEN) return;
        phase = Phase.HIDING;
        elapsedMs = 0.0;
        scale.set(SCALE_TO);
        opacity.set(1.0);
    }

    /** Called once per frame by {@link MotionBridge#update(long)}. */
    void tick(double deltaMs) {
        if (phase != Phase.SHOWING && phase != Phase.HIDING) return;
        elapsedMs += deltaMs;
        if (phase == Phase.SHOWING) {
            double scaleT = clamp01(elapsedMs / SHOW_SCALE_MS);
            double fadeT = clamp01(elapsedMs / SHOW_FADE_MS);
            scale.set(lerp(SCALE_FROM, SCALE_TO, easeOutCubic(scaleT)));
            opacity.set(lerp(0.0, 1.0, easeOutCubic(fadeT)));
            if (elapsedMs >= Math.max(SHOW_SCALE_MS, SHOW_FADE_MS)) {
                phase = Phase.SHOWN;
                scale.set(SCALE_TO);
                opacity.set(1.0);
                visible.set(Boolean.TRUE);
            }
        } else {
            double t = easeOutCubic(clamp01(elapsedMs / HIDE_MS));
            scale.set(lerp(SCALE_TO, SCALE_FROM, t));
            opacity.set(lerp(1.0, 0.0, t));
            if (elapsedMs >= HIDE_MS) {
                phase = Phase.HIDDEN;
                scale.set(SCALE_FROM);
                opacity.set(0.0);
                visible.set(Boolean.FALSE);
            }
        }
    }

    private static double clamp01(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double easeOutCubic(double t) {
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
    }
}
