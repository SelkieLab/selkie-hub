package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

/**
 * The page-transition channel: {@code progress} runs 0 -&gt; 1 (defaultSpatial spring) when a
 * new page is shown, so QML can fade/slide content in without any animation logic of its own.
 *
 * <pre>{@code
 * property var page: motion.pageChannel()
 * opacity: page.progress
 * Component.onCompleted: motion.pageIn(page)
 * }</pre>
 *
 * <p>{@link #progress} <b>is</b> the underlying spring's value (the same Property instance),
 * so bindings re-run exactly when the spring moves and no per-frame copy is needed.
 */
public final class PageChannel extends QObject {

    /** 0.0 at the start of a transition, 1.0 once the new page has fully arrived. */
    public final Property<Double> progress;

    /** The spring that integrates {@code progress} - {@code defaultSpatial} physics. */
    final SpringChannel spring;

    PageChannel(SpringChannel spring) {
        this.spring = spring;
        this.progress = spring.value;
    }

    /** True while the page transition is still running. */
    public boolean isAnimating() {
        return spring.running();
    }
}
