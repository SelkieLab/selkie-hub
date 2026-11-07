package selkie.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MD3E (Material Design 3 Expressive) motion tokens.
 *
 * <p>Expressive motion replaces duration/easing curves with <b>spring physics</b>:
 * {@code spring(dampingRatio, stiffness)} with mass = 1 (androidx.compose.material3
 * {@code MotionScheme}). A spring specifies feel, not time, so the same token adapts to
 * window size and device class.
 *
 * <p>Two spec families, and the distinction matters for building a real UI:
 * <ul>
 *   <li><b>Spatial</b> specs animate position/orientation/size/shape. They intentionally
 *       overshoot (bouncy, dampingRatio 0.6) and settle back.</li>
 *   <li><b>Effects</b> specs animate color/opacity only. They must <em>never</em> overshoot:
 *       an overshooting color would flash past its target, so dampingRatio is 1.0
 *       (critically damped).</li>
 * </ul>
 */
public final class MotionTokens {

    private MotionTokens() {
    }

    /** MD3E damping ratio constants (bounciness presets). */
    public static final class DampingRatio {
        /** No bounce; critically damped. Required for color/opacity. */
        public static final double NO_BOUNCY = 1.0;
        /** Low bounce. */
        public static final double LOW_BOUNCY = 0.75;
        /** Medium bounce. */
        public static final double MEDIUM_BOUNCY = 0.5;
        /** High bounce. */
        public static final double HIGH_BOUNCY = 0.2;

        private DampingRatio() {
        }
    }

    /**
     * A spring token: {@code dampingRatio} is the primary field because that is what the
     * MD3E spec defines ({@code spring(dampingRatio, stiffness)}); {@code mass} is fixed at 1
     * in Compose but is kept explicit so the raw damping coefficient can be derived.
     *
     * @param dampingRatio ratio c / (2 * sqrt(k * m)); 1.0 = critically damped, &lt; 1 = bouncy
     * @param stiffness    spring constant, i.e. MD3E "speed"; higher is faster
     * @param mass         oscillating mass, always 1 for MD3E tokens
     */
    public record SpringSpec(double dampingRatio, double stiffness, double mass) {

        public SpringSpec {
            if (!(dampingRatio > 0.0) || Double.isNaN(dampingRatio)) {
                throw new IllegalArgumentException("dampingRatio must be > 0: " + dampingRatio);
            }
            if (!(stiffness > 0.0) || Double.isNaN(stiffness)) {
                throw new IllegalArgumentException("stiffness must be > 0: " + stiffness);
            }
            if (!(mass > 0.0) || Double.isNaN(mass)) {
                throw new IllegalArgumentException("mass must be > 0: " + mass);
            }
        }

        /** MD3E default: mass 1, as Compose's {@code spring(dampingRatio, stiffness)}. */
        public SpringSpec(double dampingRatio, double stiffness) {
            this(dampingRatio, stiffness, 1.0);
        }

        /**
         * Raw damping coefficient {@code c}, derived from the spec's ratio:
         * {@code c = 2 * dampingRatio * sqrt(stiffness * mass)}.
         * Springs only ever specify the ratio; this is the value a physics integrator needs.
         */
        public double damping() {
            return 2.0 * dampingRatio * Math.sqrt(stiffness * mass);
        }

        /** Undamped natural angular frequency, rad/s. */
        public double naturalFrequency() {
            return Math.sqrt(stiffness / mass);
        }

        /** Damped angular frequency, rad/s (0 when critically or over damped). */
        public double dampedFrequency() {
            double wn = naturalFrequency();
            double z = dampingRatio;
            if (z >= 1.0) {
                return 0.0;
            }
            return wn * Math.sqrt(1.0 - z * z);
        }

        /** True when this spring is allowed to overshoot its target (spatial specs). */
        public boolean overshoots() {
            return dampingRatio < 1.0;
        }

        /** Ideal step response value (0 = start, 1 = target) after {@code timeSeconds}. */
        public double valueAt(double timeSeconds) {
            double wn = naturalFrequency();
            double z = dampingRatio;
            if (timeSeconds <= 0.0) {
                return 0.0;
            }
            if (z < 1.0) {
                double wd = wn * Math.sqrt(1.0 - z * z);
                double envelope = Math.exp(-z * wn * timeSeconds);
                return 1.0 - envelope * (Math.cos(wd * timeSeconds) + (z * wn / wd) * Math.sin(wd * timeSeconds));
            }
            if (Math.abs(z - 1.0) < 1e-9) {
                return 1.0 - Math.exp(-wn * timeSeconds) * (1.0 + wn * timeSeconds);
            }
            double root = wn * Math.sqrt(z * z - 1.0);
            double s1 = -z * wn + root;
            double s2 = -z * wn - root;
            return 1.0 - (s2 * Math.exp(s1 * timeSeconds) - s1 * Math.exp(s2 * timeSeconds)) / (s2 - s1);
        }

        /**
         * Time until the step response stays within {@code tolerance} of the target.
         * There is no duration token in MD3E - this is derived, for tests and for tooling
         * that needs a finite duration (e.g. a QML animation harness or a screenshot wait).
         */
        public double settleDurationMs(double tolerance) {
            if (tolerance <= 0.0 || tolerance >= 1.0) {
                throw new IllegalArgumentException("tolerance must be in (0,1): " + tolerance);
            }
            double lastViolation = 0.0;
            for (int ms = 1; ms <= 10000; ms++) {
                double value = valueAt(ms / 1000.0);
                if (Math.abs(value - 1.0) > tolerance) {
                    lastViolation = ms;
                }
            }
            return lastViolation;
        }

        /** Settle time at the MD3E-typical 0.1% tolerance. */
        public double settleDurationMs() {
            return settleDurationMs(0.001);
        }
    }

    // ------------------------------------------------------------------
    // The six MD3E motion scheme specs (MotionScheme.expressive() values)
    // ------------------------------------------------------------------

    /** Color/opacity: switch handle color change. */
    public static final SpringSpec FAST_EFFECTS_SPEC = new SpringSpec(DampingRatio.NO_BOUNCY, 3800.0);
    /** Color/opacity: content opacity within a navigation rail. */
    public static final SpringSpec DEFAULT_EFFECTS_SPEC = new SpringSpec(DampingRatio.NO_BOUNCY, 1600.0);
    /** Color/opacity: full-screen content refresh. */
    public static final SpringSpec SLOW_EFFECTS_SPEC = new SpringSpec(DampingRatio.NO_BOUNCY, 800.0);

    /** Position/size/shape: switches, buttons and other small components. */
    public static final SpringSpec FAST_SPATIAL_SPEC = new SpringSpec(0.6, 1400.0);
    /** Position/size/shape: partial-screen coverage - bottom sheets, expanded navigation rails. */
    public static final SpringSpec DEFAULT_SPATIAL_SPEC = new SpringSpec(0.6, 700.0);
    /** Position/size/shape: full-screen transitions. */
    public static final SpringSpec SLOW_SPATIAL_SPEC = new SpringSpec(0.6, 300.0);

    private static final Map<String, SpringSpec> EXPRESSIVE;
    private static final Map<String, SpringSpec> STANDARD;

    static {
        Map<String, SpringSpec> expressive = new LinkedHashMap<>();
        expressive.put("fastEffectsSpec", FAST_EFFECTS_SPEC);
        expressive.put("defaultEffectsSpec", DEFAULT_EFFECTS_SPEC);
        expressive.put("slowEffectsSpec", SLOW_EFFECTS_SPEC);
        expressive.put("fastSpatialSpec", FAST_SPATIAL_SPEC);
        expressive.put("defaultSpatialSpec", DEFAULT_SPATIAL_SPEC);
        expressive.put("slowSpatialSpec", SLOW_SPATIAL_SPEC);
        EXPRESSIVE = Collections.unmodifiableMap(expressive);

        // Standard: same speeds, no overshoot anywhere - "subtle, professional" motion.
        // Effects are already critically damped, so only the spatial specs change.
        Map<String, SpringSpec> standard = new LinkedHashMap<>();
        standard.put("fastEffectsSpec", FAST_EFFECTS_SPEC);
        standard.put("defaultEffectsSpec", DEFAULT_EFFECTS_SPEC);
        standard.put("slowEffectsSpec", SLOW_EFFECTS_SPEC);
        standard.put("fastSpatialSpec", new SpringSpec(DampingRatio.NO_BOUNCY, FAST_SPATIAL_SPEC.stiffness()));
        standard.put("defaultSpatialSpec", new SpringSpec(DampingRatio.NO_BOUNCY, DEFAULT_SPATIAL_SPEC.stiffness()));
        standard.put("slowSpatialSpec", new SpringSpec(DampingRatio.NO_BOUNCY, SLOW_SPATIAL_SPEC.stiffness()));
        STANDARD = Collections.unmodifiableMap(standard);
    }

    /** {@code MotionScheme.expressive()} - Material's recommended default (bouncy spatial). */
    public static Map<String, SpringSpec> expressive() {
        return EXPRESSIVE;
    }

    /** {@code MotionScheme.standard()} - less bounce, no overshoot. */
    public static Map<String, SpringSpec> standard() {
        return STANDARD;
    }

    /** Six specs of the MD3E motion scheme, keyed by their Compose token names. */
    public static Map<String, SpringSpec> tokens() {
        return EXPRESSIVE;
    }

    /** True when every effects (color/opacity) spec is critically damped or stiffer. */
    public static boolean effectsSpecsNeverOvershoot() {
        for (Map.Entry<String, SpringSpec> entry : EXPRESSIVE.entrySet()) {
            if (isEffectsToken(entry.getKey()) && entry.getValue().overshoots()) {
                return false;
            }
        }
        return true;
    }

    /** Token-name based classification: effects tokens animate color/opacity, spatial ones geometry. */
    public static boolean isEffectsToken(String tokenName) {
        String lower = tokenName.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("effects");
    }
}
