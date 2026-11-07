package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import selkie.theme.MotionTokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Host-driven MD3E spring motion, registered as the QML context object {@code motion}
 * <b>before</b> {@code view.load(...)}.
 *
 * <p>Contract (fixed by the Lead, do not change):
 * <ul>
 *   <li>{@code motion.channel(String tokenName)} -&gt; a <b>new</b> {@link SpringChannel}
 *       with a reactive {@code Property<Double> value}. Every call creates an independent
 *       channel, so two switches, a tab indicator and a card can all animate at the same
 *       time on their own state - the token only selects the physics
 *       ({@link MotionTokens}), it is not a shared instance;</li>
 *   <li>{@code motion.to(ch, target)} - retarget, interruptible;</li>
 *   <li>{@code motion.jump(ch, target)} - set immediately, no animation.</li>
 * </ul>
 *
 * <p>Parameters always come from {@link MotionTokens}; the six MD3E token names are
 * {@code fastSpatial, defaultSpatial, slowSpatial, fastEffects, defaultEffects, slowEffects},
 * and the plan's {@code standard*} names are accepted as aliases.
 *
 * <p>Threading: {@link #update(long)} and every QML-visible write happen on the render
 * thread inside the frame loop, which is the only thread allowed to touch the view.
 * ({@link #channel(String)} is called while QML components are created, which is also the
 * render thread during {@code load()} / binding evaluation.)
 */
public final class MotionBridge extends QObject {

    private final Map<String, MotionTokens.SpringSpec> specs;

    /** Every channel ever handed to QML, in creation order (identity semantics). */
    private final List<SpringChannel> channels = new ArrayList<>();
    private final Set<SpringChannel> owned = Collections.newSetFromMap(new IdentityHashMap<>());

    private int nextSerial = 1;

    public MotionBridge() {
        Map<String, MotionTokens.SpringSpec> map = new LinkedHashMap<>();
        map.put("fastSpatial", MotionTokens.FAST_SPATIAL_SPEC);
        map.put("defaultSpatial", MotionTokens.DEFAULT_SPATIAL_SPEC);
        map.put("slowSpatial", MotionTokens.SLOW_SPATIAL_SPEC);
        map.put("fastEffects", MotionTokens.FAST_EFFECTS_SPEC);
        map.put("defaultEffects", MotionTokens.DEFAULT_EFFECTS_SPEC);
        map.put("slowEffects", MotionTokens.SLOW_EFFECTS_SPEC);
        // Plan-named aliases: the standard scheme shares the effects specs and uses
        // critically damped spatial springs (MotionTokens.standard()).
        map.put("standardFastSpatial", MotionTokens.standard().get("fastSpatialSpec"));
        map.put("standardDefaultSpatial", MotionTokens.standard().get("defaultSpatialSpec"));
        map.put("standardSlowSpatial", MotionTokens.standard().get("slowSpatialSpec"));
        this.specs = Collections.unmodifiableMap(map);
    }

    /**
     * QML entry point: {@code motion.channel("fastSpatial")}.
     *
     * <p>Returns a fresh, independent channel every time. Call it once per animated
     * property (e.g. one per component instance) and keep it in a QML {@code property var}.
     */
    public SpringChannel channel(String tokenName) {
        MotionTokens.SpringSpec spec = specs.get(tokenName);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown motion token: " + tokenName
                    + " (expected one of " + specs.keySet() + ")");
        }
        SpringChannel channel = new SpringChannel(tokenName, spec, nextSerial++);
        channels.add(channel);
        owned.add(channel);
        return channel;
    }

    /** QML entry point: start/redirect a spring. Interruptible by design. */
    public void to(SpringChannel channel, double target) {
        requireOwned(channel);
        channel.retarget(target);
    }

    /** QML entry point: place the value immediately (used for resets/initial layout). */
    public void jump(SpringChannel channel, double target) {
        requireOwned(channel);
        channel.jumpTo(target);
    }

    /** True while any channel is still moving (handy for frame pacing / diagnostics). */
    public boolean isAnimating() {
        for (SpringChannel channel : snapshot()) {
            if (channel.running()) return true;
        }
        return false;
    }

    /** Advances every running channel; called once per frame from the render loop. */
    public void update(long deltaNanos) {
        if (deltaNanos <= 0L) return;
        double seconds = deltaNanos / 1_000_000_000.0;
        // Never integrate a long stall (window drag, breakpoint) in a single jump.
        if (seconds > 0.1) seconds = 0.1;
        for (SpringChannel channel : snapshot()) {
            if (channel.running()) channel.advance(seconds);
        }
    }

    /** Every channel handed out so far, in creation order (used by --motion-trace). */
    public Collection<SpringChannel> allChannels() {
        return snapshot();
    }

    /** Number of channels QML has created - a cheap leak/duplication check. */
    public int channelCount() {
        return channels.size();
    }

    private List<SpringChannel> snapshot() {
        // QML may create a channel from a binding while a frame is in flight; iterate a copy.
        return new ArrayList<>(channels);
    }

    private void requireOwned(SpringChannel channel) {
        if (channel == null || !owned.contains(channel)) {
            throw new IllegalArgumentException("Channel was not created by this motion bridge: " + channel);
        }
    }

    /** One trace line per frame listing the channels that are actually moving. */
    void trace(long elapsedMs) {
        StringBuilder line = new StringBuilder("[motion] t=").append(elapsedMs);
        boolean any = false;
        for (SpringChannel channel : snapshot()) {
            if (!channel.running()) continue;
            line.append(' ').append(channel.tokenName()).append('#').append(channel.serial())
                    .append('=').append(String.format(Locale.ROOT, "%.5f", channel.value.peek()));
            any = true;
        }
        if (any) System.out.println(line);
    }
}
