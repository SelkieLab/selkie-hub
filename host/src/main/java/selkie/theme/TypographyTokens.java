package selkie.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MD3E typography tokens, in pixels, with the full per-style metrics.
 *
 * <p>Provenance: {@code references/design-tokens.md} section 2 "Typography Scale" - the 15 base
 * M3 styles ("30 typography slots on the 1.5.0 line = 15 base styles + 15 *Emphasized variants").
 * Size / line height / weight / letter spacing below are copied from that table (the 15
 * *Emphasized variants do not exist on stable Compose 1.4.0, so they are not exposed).
 *
 * <p>Units: the design tokens are sp (scaled pixels). qml4j renders with no density concept, so
 * 1sp == 1px at the design density and these values are used directly as {@code font.pixelSize}.
 * A future UI-scale setting multiplies them; that is why {@code ThemeBridge} exposes them as
 * {@code Property<Double>} rather than constants.
 */
public final class TypographyTokens {

    /** One MD3E type style. Sizes are px (1sp == 1px, see class javadoc). */
    public record TypeStyle(String tokenName, String styleName, double sizePx, double lineHeightPx,
            int weight, double letterSpacingPx) {

        public TypeStyle {
            if (!(sizePx > 0)) {
                throw new IllegalArgumentException("sizePx must be > 0: " + tokenName);
            }
            if (!(lineHeightPx >= sizePx)) {
                throw new IllegalArgumentException("lineHeightPx must be >= sizePx: " + tokenName);
            }
            if (weight < 100 || weight > 900) {
                throw new IllegalArgumentException("weight out of range: " + tokenName);
            }
        }
    }

    // --- display ---
    /** 57/64, regular - hero text. */
    public static final TypeStyle DISPLAY_LARGE =
            new TypeStyle("typeDisplayLarge", "displayLarge", 57, 64, 400, -0.25);
    /** 45/52, regular. */
    public static final TypeStyle DISPLAY_MEDIUM =
            new TypeStyle("typeDisplayMedium", "displayMedium", 45, 52, 400, 0);
    /** 36/44, regular. */
    public static final TypeStyle DISPLAY_SMALL =
            new TypeStyle("typeDisplaySmall", "displaySmall", 36, 44, 400, 0);

    // --- headline ---
    /** 32/40, regular. */
    public static final TypeStyle HEADLINE_LARGE =
            new TypeStyle("typeHeadlineLarge", "headlineLarge", 32, 40, 400, 0);
    /** 28/36, regular. */
    public static final TypeStyle HEADLINE_MEDIUM =
            new TypeStyle("typeHeadlineMedium", "headlineMedium", 28, 36, 400, 0);
    /** 24/32, regular. */
    public static final TypeStyle HEADLINE_SMALL =
            new TypeStyle("typeHeadlineSmall", "headlineSmall", 24, 32, 400, 0);

    // --- title ---
    /** 22/28, medium. */
    public static final TypeStyle TITLE_LARGE =
            new TypeStyle("typeTitleLarge", "titleLarge", 22, 28, 500, 0);
    /** 16/24, medium. */
    public static final TypeStyle TITLE_MEDIUM =
            new TypeStyle("typeTitleMedium", "titleMedium", 16, 24, 500, 0.15);
    /** 14/20, medium. */
    public static final TypeStyle TITLE_SMALL =
            new TypeStyle("typeTitleSmall", "titleSmall", 14, 20, 500, 0.1);

    // --- body ---
    /** 16/24, regular. */
    public static final TypeStyle BODY_LARGE =
            new TypeStyle("typeBodyLarge", "bodyLarge", 16, 24, 400, 0.5);
    /** 14/20, regular - default body text. */
    public static final TypeStyle BODY_MEDIUM =
            new TypeStyle("typeBodyMedium", "bodyMedium", 14, 20, 400, 0.25);
    /** 12/16, regular. */
    public static final TypeStyle BODY_SMALL =
            new TypeStyle("typeBodySmall", "bodySmall", 12, 16, 400, 0.4);

    // --- label ---
    /** 14/20, medium - button text. */
    public static final TypeStyle LABEL_LARGE =
            new TypeStyle("typeLabelLarge", "labelLarge", 14, 20, 500, 0.1);
    /** 12/16, medium. */
    public static final TypeStyle LABEL_MEDIUM =
            new TypeStyle("typeLabelMedium", "labelMedium", 12, 16, 500, 0.5);
    /** 11/16, medium - captions. */
    public static final TypeStyle LABEL_SMALL =
            new TypeStyle("typeLabelSmall", "labelSmall", 11, 16, 500, 0.5);

    /** Number of typography tokens in the frozen QML contract. */
    public static final int CONTRACT_COUNT = 15;

    private static final Map<String, TypeStyle> STYLES;

    static {
        Map<String, TypeStyle> styles = new LinkedHashMap<>();
        for (TypeStyle style : new TypeStyle[] {
            DISPLAY_LARGE, DISPLAY_MEDIUM, DISPLAY_SMALL,
            HEADLINE_LARGE, HEADLINE_MEDIUM, HEADLINE_SMALL,
            TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
            BODY_LARGE, BODY_MEDIUM, BODY_SMALL,
            LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL
        }) {
            styles.put(style.tokenName(), style);
        }
        STYLES = Collections.unmodifiableMap(styles);
    }

    private TypographyTokens() {
    }

    /** The frozen QML contract: token name -&gt; style, in MD3E order (largest to smallest). */
    public static Map<String, TypeStyle> styles() {
        return STYLES;
    }

    /** Style for a contract token name, e.g. {@code "typeBodyMedium"}. */
    public static TypeStyle style(String tokenName) {
        TypeStyle style = STYLES.get(tokenName);
        if (style == null) {
            throw new IllegalArgumentException("Unknown typography token: " + tokenName);
        }
        return style;
    }

    /** Font size in px for a contract token name. */
    public static double sizePx(String tokenName) {
        return style(tokenName).sizePx();
    }
}
