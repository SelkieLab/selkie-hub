package selkie.host;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;
import selkie.theme.MotionTokens;

/**
 * One host-driven spring, exposed to QML as a reactive {@code value}.
 *
 * <p>qml4j has no {@code SpringAnimation} (verified in the recon), so motion is integrated
 * here, on the render thread, and QML simply binds to {@link #value}:
 *
 * <pre>{@code
 * property var ch: motion.channel("fastSpatial")
 * x: baseX + ch.value * travel
 * }</pre>
 *
 * <p>The physical parameters are always taken from {@link MotionTokens}; nothing about
 * damping or stiffness is hard-coded in QML or duplicated here.
 */
public final class SpringChannel extends QObject {

    /** Current position of the spring, 0-derived: the host writes, QML binds. */
    public final Property<Double> value = new Property<>(0.0);

    /**
     * Diagnostics only (not part of the motion contract): the extreme value reached since the
     * last {@code motion.to(...)}. It lets a QML label show "this spring overshot by X"
     * without any QML-side math, and it is what the acceptance run reads back.
     */
    public final Property<Double> peak = new Property<>(0.0);

    private final String tokenName;
    private final int serial;
    private final MotionTokens.SpringSpec spec;
    private final double dampingCoefficient;

    /** Position the spring is heading to (in the same units as {@link #value}). */
    private double target;
    private double velocity;
    private boolean running;

    SpringChannel(String tokenName, MotionTokens.SpringSpec spec, int serial) {
        this.tokenName = tokenName;
        this.serial = serial;
        this.spec = spec;
        // c = 2 * zeta * sqrt(k * m) - the standard viscous damping coefficient.
        this.dampingCoefficient = 2.0 * spec.dampingRatio()
                * Math.sqrt(spec.stiffness() * spec.mass());
    }

    /** The MD3E token this channel is bound to, e.g. {@code fastSpatial}. */
    public String tokenName() {
        return tokenName;
    }

    /** Creation index within the bridge - keeps trace lines distinguishable. */
    public int serial() {
        return serial;
    }

    /** Numeric parameters, for diagnostics and host-side math. */
    public double dampingRatio() {
        return spec.dampingRatio();
    }

    public double stiffness() {
        return spec.stiffness();
    }

    public double mass() {
        return spec.mass();
    }

    public double target() {
        return target;
    }

    public double velocity() {
        return velocity;
    }

    public boolean running() {
        return running;
    }

    /** Retarget: keeps the current position and velocity, so an in-flight motion bends. */
    void retarget(double newTarget) {
        this.target = newTarget;
        this.running = true;
        this.peak.set(value.peek());
    }

    /** Snap instantly (no animation, no overshoot). */
    void jumpTo(double newTarget) {
        this.target = newTarget;
        this.velocity = 0.0;
        this.running = false;
        value.set(newTarget);
        peak.set(newTarget);
    }

    /**
     * Advance the spring by {@code dtSeconds} using semi-implicit Euler at a fixed 1 ms
     * substep. The value is published to QML once per call (not once per substep).
     */
    void advance(double dtSeconds) {
        if (!running) return;
        final double substep = 0.001;
        double x = value.peek();
        double v = velocity;
        double remaining = dtSeconds;
        while (remaining > 0.0) {
            double h = Math.min(substep, remaining);
            remaining -= h;
            double acceleration = (-spec.stiffness() * (x - target) - dampingCoefficient * v)
                    / spec.mass();
            v += acceleration * h;
            double next = x + v * h;
            if (spec.dampingRatio() >= 1.0) {
                // Critically/over-damped: analytic motion cannot cross the target, and the
                // discrete integrator must not either - otherwise an "effects" token would
                // visually overshoot. Clamp the crossing to the target.
                if ((x - target) * (next - target) < 0.0) {
                    next = target;
                    v = 0.0;
                }
            }
            x = next;
            if (Math.abs(x - target) < 1e-4 && Math.abs(v) < 1e-3) {
                x = target;
                v = 0.0;
                running = false;
                break;
            }
        }
        this.velocity = v;
        value.set(x);
        double extreme = peak.peek();
        if (x > extreme) peak.set(x);
    }
}
