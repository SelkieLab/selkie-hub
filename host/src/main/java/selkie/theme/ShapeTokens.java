package selkie.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MD3E shape (corner radius) tokens in pixels.
 *
 * <p>Provenance: {@code references/design-tokens.md} section 3 "Shape Scale" - "8 shape sizes in
 * the 1.5.0 line (M3's 5 + 3 M3E sizes), from square to fully rounded. Values verified against
 * {@code ShapeTokens.kt} in {@code androidx-main}". The full scale is:
 * <pre>
 *   extraSmall 4, small 8, medium 12, large 16,
 *   largeIncreased 20 [M3E], extraLarge 28, extraLargeIncreased 32 [M3E], extraExtraLarge 48 [M3E]
 * </pre>
 *
 * <p>The QML contract exposes a 6-step subset of that scale plus two sentinels; the M3E-only
 * intermediate sizes are kept here as constants so a component that needs 20/32/48 does not
 * hardcode a number (they are simply not part of the frozen QML name set yet).
 *
 * <p>{@link #RADIUS_FULL} is <b>not</b> an MD3E radius: MD3 expresses pills/circles as
 * {@code CircleShape}/{@code RoundedCornerShape(50%)}, which QML's {@code radius} cannot say.
 * 999 is a sentinel meaning "as round as the bounds allow" - components either use it directly
 * on a {@code Rectangle} or clamp it with {@link #resolve(double, double, double)}.
 */
public final class ShapeTokens {

    /** Square corners. */
    public static final int RADIUS_NONE = 0;
    /** M3E {@code extraSmall}: 4dp - text field corners, small elements. */
    public static final int RADIUS_EXTRA_SMALL = 4;
    /** M3E {@code small}: 8dp - chips, small buttons, tooltips. */
    public static final int RADIUS_SMALL = 8;
    /** M3E {@code medium}: 12dp - cards, dialogs, small FAB. */
    public static final int RADIUS_MEDIUM = 12;
    /** M3E {@code large}: 16dp - FAB, large cards, sheets. */
    public static final int RADIUS_LARGE = 16;
    /** M3E-only {@code largeIncreased}: 20dp - between large and extraLarge. */
    public static final int RADIUS_LARGE_INCREASED = 20;
    /** M3E {@code extraLarge}: 28dp - extended FAB, large containers, search bar. */
    public static final int RADIUS_EXTRA_LARGE = 28;
    /** M3E-only {@code extraLargeIncreased}: 32dp - between extraLarge and extraExtraLarge. */
    public static final int RADIUS_EXTRA_LARGE_INCREASED = 32;
    /** M3E-only {@code extraExtraLarge}: 48dp - near-circular containers. */
    public static final int RADIUS_EXTRA_EXTRA_LARGE = 48;

    /**
     * Sentinel for pill/circle corners: "clamp to half the shorter side". Not an MD3E radius
     * value; large enough that no widget in this app is taller or wider than twice it.
     */
    public static final int RADIUS_FULL = 999;

    /** Number of shape tokens in the frozen QML contract. */
    public static final int CONTRACT_COUNT = 7;

    private static final Map<String, Integer> TOKENS;

    static {
        Map<String, Integer> tokens = new LinkedHashMap<>();
        tokens.put("shapeNone", RADIUS_NONE);
        tokens.put("shapeXs", RADIUS_EXTRA_SMALL);
        tokens.put("shapeS", RADIUS_SMALL);
        tokens.put("shapeM", RADIUS_MEDIUM);
        tokens.put("shapeL", RADIUS_LARGE);
        tokens.put("shapeXl", RADIUS_EXTRA_LARGE);
        tokens.put("shapeFull", RADIUS_FULL);
        TOKENS = Collections.unmodifiableMap(tokens);
    }

    private ShapeTokens() {
    }

    /** The frozen QML contract: token name -&gt; radius in px, in ascending order. */
    public static Map<String, Integer> tokens() {
        return TOKENS;
    }

    /** Radius in px for a contract token name, e.g. {@code "shapeM"}. */
    public static int radius(String tokenName) {
        Integer value = TOKENS.get(tokenName);
        if (value == null) {
            throw new IllegalArgumentException("Unknown shape token: " + tokenName);
        }
        return value;
    }

    /** The complete MD3E shape scale (contract subset + the M3E-only intermediate sizes). */
    public static Map<String, Integer> md3eScale() {
        Map<String, Integer> scale = new LinkedHashMap<>();
        scale.put("extraSmall", RADIUS_EXTRA_SMALL);
        scale.put("small", RADIUS_SMALL);
        scale.put("medium", RADIUS_MEDIUM);
        scale.put("large", RADIUS_LARGE);
        scale.put("largeIncreased", RADIUS_LARGE_INCREASED);
        scale.put("extraLarge", RADIUS_EXTRA_LARGE);
        scale.put("extraLargeIncreased", RADIUS_EXTRA_LARGE_INCREASED);
        scale.put("extraExtraLarge", RADIUS_EXTRA_EXTRA_LARGE);
        return Collections.unmodifiableMap(scale);
    }

    /** True when a radius is the pill/circle sentinel rather than a real corner radius. */
    public static boolean isFull(double radius) {
        return radius >= RADIUS_FULL;
    }

    /**
     * Resolves a token radius against a real widget size: sentinel values become half the shorter
     * side, real radii pass through unchanged. Lets a component write
     * {@code radius: Theme.shapeFull} on any size and still get a perfect pill.
     */
    public static double resolve(double tokenRadius, double width, double height) {
        double halfShorterSide = Math.min(Math.abs(width), Math.abs(height)) / 2.0;
        return isFull(tokenRadius) ? halfShorterSide : Math.min(tokenRadius, halfShorterSide);
    }
}
