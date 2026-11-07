package selkie.theme;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

import java.util.Locale;

/**
 * The six MD3E spring tokens, shaped for QML.
 *
 * <p>qml4j has no {@code SpringAnimation} and no spring easing, so these tokens are
 * <em>data</em>: the host (or a later stage) drives motion itself from the numbers here.
 * QML reads them as {@code Theme.spring.fastSpatial} (a ready-made display string) or
 * {@code Theme.spring.fastSpatialStiffness} (a number it can feed into its own math).
 *
 * <p>Naming: the canonical tokens keep their MD3E/Compose names
 * ({@code fastSpatial}, {@code defaultSpatial}, {@code slowSpatial}, {@code fastEffects},
 * {@code defaultEffects}, {@code slowEffects}). The design plan also refers to the
 * no-overshoot scheme as {@code standard*}, so each token has a {@code standard*} alias
 * sourced from {@link MotionTokens#standard()} (identical to the expressive token except
 * that the spatial springs are critically damped).
 */
public final class SpringBridge extends QObject {

    /** Number of spring tokens in a scheme. */
    public final Property<Integer> count = new Property<>(6);

    // ---- canonical MD3E expressive tokens, formatted for display ----
    public final Property<String> fastSpatial =
            token("fastSpatial", MotionTokens.FAST_SPATIAL_SPEC);
    public final Property<String> defaultSpatial =
            token("defaultSpatial", MotionTokens.DEFAULT_SPATIAL_SPEC);
    public final Property<String> slowSpatial =
            token("slowSpatial", MotionTokens.SLOW_SPATIAL_SPEC);
    public final Property<String> fastEffects =
            token("fastEffects", MotionTokens.FAST_EFFECTS_SPEC);
    public final Property<String> defaultEffects =
            token("defaultEffects", MotionTokens.DEFAULT_EFFECTS_SPEC);
    public final Property<String> slowEffects =
            token("slowEffects", MotionTokens.SLOW_EFFECTS_SPEC);

    // ---- the same six tokens as numbers, for host-driven spring math ----
    public final Property<Double> fastSpatialDampingRatio = num(MotionTokens.FAST_SPATIAL_SPEC.dampingRatio());
    public final Property<Double> fastSpatialStiffness = num(MotionTokens.FAST_SPATIAL_SPEC.stiffness());
    public final Property<Double> fastSpatialMass = num(MotionTokens.FAST_SPATIAL_SPEC.mass());
    public final Property<Double> defaultSpatialDampingRatio = num(MotionTokens.DEFAULT_SPATIAL_SPEC.dampingRatio());
    public final Property<Double> defaultSpatialStiffness = num(MotionTokens.DEFAULT_SPATIAL_SPEC.stiffness());
    public final Property<Double> defaultSpatialMass = num(MotionTokens.DEFAULT_SPATIAL_SPEC.mass());
    public final Property<Double> slowSpatialDampingRatio = num(MotionTokens.SLOW_SPATIAL_SPEC.dampingRatio());
    public final Property<Double> slowSpatialStiffness = num(MotionTokens.SLOW_SPATIAL_SPEC.stiffness());
    public final Property<Double> slowSpatialMass = num(MotionTokens.SLOW_SPATIAL_SPEC.mass());
    public final Property<Double> fastEffectsDampingRatio = num(MotionTokens.FAST_EFFECTS_SPEC.dampingRatio());
    public final Property<Double> fastEffectsStiffness = num(MotionTokens.FAST_EFFECTS_SPEC.stiffness());
    public final Property<Double> fastEffectsMass = num(MotionTokens.FAST_EFFECTS_SPEC.mass());
    public final Property<Double> defaultEffectsDampingRatio = num(MotionTokens.DEFAULT_EFFECTS_SPEC.dampingRatio());
    public final Property<Double> defaultEffectsStiffness = num(MotionTokens.DEFAULT_EFFECTS_SPEC.stiffness());
    public final Property<Double> defaultEffectsMass = num(MotionTokens.DEFAULT_EFFECTS_SPEC.mass());
    public final Property<Double> slowEffectsDampingRatio = num(MotionTokens.SLOW_EFFECTS_SPEC.dampingRatio());
    public final Property<Double> slowEffectsStiffness = num(MotionTokens.SLOW_EFFECTS_SPEC.stiffness());
    public final Property<Double> slowEffectsMass = num(MotionTokens.SLOW_EFFECTS_SPEC.mass());

    // ---- "standard" (no-overshoot) aliases named by the design plan ----
    public final Property<String> standardFastSpatial =
            token("standardFastSpatial", MotionTokens.standard().get("fastSpatialSpec"));
    public final Property<String> standardDefaultSpatial =
            token("standardDefaultSpatial", MotionTokens.standard().get("defaultSpatialSpec"));
    public final Property<String> standardSlowSpatial =
            token("standardSlowSpatial", MotionTokens.standard().get("slowSpatialSpec"));
    public final Property<String> standardFastEffects =
            token("standardFastEffects", MotionTokens.standard().get("fastEffectsSpec"));
    public final Property<String> standardDefaultEffects =
            token("standardDefaultEffects", MotionTokens.standard().get("defaultEffectsSpec"));
    public final Property<String> standardSlowEffects =
            token("standardSlowEffects", MotionTokens.standard().get("slowEffectsSpec"));

    /** {@code name  damping=..  stiffness=..  mass=..} - one line, no QML-side formatting. */
    static Property<String> token(String label, MotionTokens.SpringSpec spec) {
        return new Property<>(String.format(Locale.ROOT, "%s  damping=%.2f  stiffness=%.1f  mass=%.1f",
                label, spec.dampingRatio(), spec.stiffness(), spec.mass()));
    }

    private static Property<Double> num(double value) {
        return new Property<>(value);
    }
}
