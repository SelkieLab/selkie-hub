package selkie.host;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The page-transition channel: defaultSpatial spring drives {@code progress} 0 -&gt; 1 and back. */
class PageTransitionTest {

    private static void advance(MotionBridge motion, long totalMs) {
        long remaining = totalMs;
        while (remaining > 0) {
            long step = Math.min(16L, remaining);
            motion.update(step * 1_000_000L);
            remaining -= step;
        }
    }

    @Test
    void pageInSpringsProgressTo1AndPageOutBackTo0() {
        MotionBridge motion = new MotionBridge();
        PageChannel page = motion.pageChannel();

        assertEquals(0.0, page.progress.peek(), 1e-9, "a new page channel starts at 0");
        motion.pageIn(page);
        advance(motion, 60);
        double mid = page.progress.peek();
        assertTrue(mid > 0.0 && mid < 1.2, "progress should be moving, was " + mid);

        advance(motion, 900);
        assertEquals(1.0, page.progress.peek(), 1e-3, "progress settles at 1.0");
        assertTrue(!page.isAnimating(), "the page spring settles");

        motion.pageOut(page);
        advance(motion, 900);
        assertEquals(0.0, page.progress.peek(), 1e-3, "pageOut springs back to 0");
    }

    @Test
    void pageJumpPlacesProgressImmediately() {
        MotionBridge motion = new MotionBridge();
        PageChannel page = motion.pageChannel();
        motion.pageJump(page, 1.0);
        assertEquals(1.0, page.progress.peek(), 1e-9);
        assertTrue(!page.isAnimating());
    }
}
