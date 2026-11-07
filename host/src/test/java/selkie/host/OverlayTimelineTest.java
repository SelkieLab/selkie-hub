package selkie.host;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The host-driven overlay timeline (Stage 5a). No QML: the channel is integrated directly,
 * so the assertions are about the numbers the QML bindings would read.
 *
 * <p>Spec: show = 200 ms scale (0.92 -&gt; 1.0) + 150 ms fade (0 -&gt; 1); hide = 150 ms back to
 * 0.92 / 0, then {@code visible = false}.
 */
class OverlayTimelineTest {

    private static final long FRAME_MS = 16L;
    private static final long FRAME_NANOS = FRAME_MS * 1_000_000L;

    private static void advance(MotionBridge motion, long totalMs) {
        long remaining = totalMs;
        while (remaining > 0) {
            long step = Math.min(FRAME_MS, remaining);
            motion.update(step * 1_000_000L);
            remaining -= step;
        }
    }

    @Test
    void showInterpolatesScaleAndOpacityThenSettlesShown() {
        MotionBridge motion = new MotionBridge();
        OverlayChannel overlay = motion.overlay("toast");

        // Dismissed state before anything happens.
        assertFalse(overlay.visible.peek(), "a fresh overlay is not visible");
        assertEquals(OverlayChannel.SCALE_FROM, overlay.scale.peek(), 1e-9);

        motion.showOverlay(overlay);
        assertTrue(overlay.visible.peek(), "showOverlay makes it visible on the same frame");
        assertEquals(OverlayChannel.SCALE_FROM, overlay.scale.peek(), 1e-9);

        // Mid-flight: both properties must be strictly between the endpoints.
        advance(motion, 80);
        double midScale = overlay.scale.peek();
        double midOpacity = overlay.opacity.peek();
        assertTrue(midScale > OverlayChannel.SCALE_FROM && midScale < OverlayChannel.SCALE_TO,
                "scale mid-flight should be between 0.92 and 1.0, was " + midScale);
        assertTrue(midOpacity > 0.0 && midOpacity < 1.0,
                "opacity mid-flight should be between 0 and 1, was " + midOpacity);
        assertTrue(overlay.isAnimating(), "the enter timeline is still running at 80 ms");

        // 200 ms is the longest leg (scale); after it the overlay is fully shown.
        advance(motion, 200);
        assertEquals(1.0, overlay.scale.peek(), 1e-9, "scale settles at 1.0");
        assertEquals(1.0, overlay.opacity.peek(), 1e-9, "opacity settles at 1.0");
        assertTrue(overlay.visible.peek());
        assertFalse(overlay.isAnimating(), "the enter timeline is done after 280 ms total");
    }

    @Test
    void hideFadesOutScalesBackAndClearsVisible() {
        MotionBridge motion = new MotionBridge();
        OverlayChannel overlay = motion.overlay("toast");
        motion.showOverlay(overlay);
        advance(motion, 400);
        assertEquals(1.0, overlay.opacity.peek(), 1e-9, "precondition: fully shown");

        motion.hideOverlay(overlay);
        advance(motion, 75);
        assertTrue(overlay.opacity.peek() > 0.0 && overlay.opacity.peek() < 1.0,
                "opacity should be fading, was " + overlay.opacity.peek());
        assertTrue(overlay.visible.peek(), "still visible while the exit timeline runs");

        advance(motion, 150);
        assertFalse(overlay.visible.peek(), "visible must be false once the exit timeline ends");
        assertEquals(0.0, overlay.opacity.peek(), 1e-9);
        assertEquals(OverlayChannel.SCALE_FROM, overlay.scale.peek(), 1e-9,
                "the overlay returns to its 0.92 resting scale");
        assertFalse(overlay.isAnimating());
    }

    @Test
    void showRestartsFromTheCanonicalStartAndHideIsIdempotentWhenHidden() {
        MotionBridge motion = new MotionBridge();
        OverlayChannel overlay = motion.overlay("toast");
        motion.showOverlay(overlay);
        advance(motion, 400);

        motion.showOverlay(overlay);
        assertEquals(OverlayChannel.SCALE_FROM, overlay.scale.peek(), 1e-9,
                "a re-show restarts the enter timeline from 0.92");
        assertEquals(0.0, overlay.opacity.peek(), 1e-9);

        motion.hideOverlay(overlay);
        advance(motion, 200);
        assertFalse(overlay.visible.peek());
        motion.hideOverlay(overlay); // already hidden - must not resurrect it
        assertFalse(overlay.visible.peek(), "hide on a hidden overlay is a no-op");
    }
}
