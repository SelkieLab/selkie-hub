package selkie.theme;

import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.binding.Property;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The QML-facing side of the MD3E theme: one public {@link Property} per color role,
 * so every QML binding that reads a role re-runs when the scheme changes.
 *
 * <h2>Why two fields per role</h2>
 * qml4j resolves QML {@code color:} values as strings ({@code Renderer.parseColor(String)});
 * binding a {@code Property<Integer>} ARGB straight to {@code color:} throws
 * {@code ClassCastException: java.lang.Long cannot be cast to java.lang.String}
 * (measured, see the stage-1b probe). So each role exists twice:
 * <ul>
 *   <li>{@code <role>} - the authoritative ARGB integer ({@code MaterialToken} surface,
 *       used by the host, tests and any Java consumer);</li>
 *   <li>{@code <role>Hex} - the same value as {@code "#RRGGBB"}, which is what
 *       {@code qml/Theme.qml} binds to. Both are written together by {@link #apply(ColorScheme)}.</li>
 * </ul>
 *
 * <p>This object is registered as the per-window context property {@code theme}
 * ({@code view.context("theme", bridge)}) <b>before</b> {@code view.load(...)}. QML may
 * call {@link #toggleDark()}, {@link #setDark(boolean)} and {@link #setSeed(int)};
 * the role fields themselves are host-owned.
 *
 * <p>Threading: all QML-visible mutation happens on the render thread (the host calls
 * the methods from its frame loop / key handler), per qml4j's single-thread rule.
 */
public final class ThemeBridge extends QObject {

    /** M3 baseline purple - the same seed the published-baseline test uses. */
    public static final int DEFAULT_SEED = 0xFF6750A4;

    // ------------------------------------------------------------------
    // 48 MD3E color roles (ARGB integers + QML-ready hex strings)
    // ------------------------------------------------------------------

    public final Property<Integer> primary = new Property<>(0);
    public final Property<String> primaryHex = new Property<>("#000000");
    public final Property<Integer> onPrimary = new Property<>(0);
    public final Property<String> onPrimaryHex = new Property<>("#000000");
    public final Property<Integer> primaryContainer = new Property<>(0);
    public final Property<String> primaryContainerHex = new Property<>("#000000");
    public final Property<Integer> onPrimaryContainer = new Property<>(0);
    public final Property<String> onPrimaryContainerHex = new Property<>("#000000");
    public final Property<Integer> inversePrimary = new Property<>(0);
    public final Property<String> inversePrimaryHex = new Property<>("#000000");
    public final Property<Integer> primaryFixed = new Property<>(0);
    public final Property<String> primaryFixedHex = new Property<>("#000000");
    public final Property<Integer> primaryFixedDim = new Property<>(0);
    public final Property<String> primaryFixedDimHex = new Property<>("#000000");
    public final Property<Integer> onPrimaryFixed = new Property<>(0);
    public final Property<String> onPrimaryFixedHex = new Property<>("#000000");
    public final Property<Integer> onPrimaryFixedVariant = new Property<>(0);
    public final Property<String> onPrimaryFixedVariantHex = new Property<>("#000000");
    public final Property<Integer> secondary = new Property<>(0);
    public final Property<String> secondaryHex = new Property<>("#000000");
    public final Property<Integer> onSecondary = new Property<>(0);
    public final Property<String> onSecondaryHex = new Property<>("#000000");
    public final Property<Integer> secondaryContainer = new Property<>(0);
    public final Property<String> secondaryContainerHex = new Property<>("#000000");
    public final Property<Integer> onSecondaryContainer = new Property<>(0);
    public final Property<String> onSecondaryContainerHex = new Property<>("#000000");
    public final Property<Integer> secondaryFixed = new Property<>(0);
    public final Property<String> secondaryFixedHex = new Property<>("#000000");
    public final Property<Integer> secondaryFixedDim = new Property<>(0);
    public final Property<String> secondaryFixedDimHex = new Property<>("#000000");
    public final Property<Integer> onSecondaryFixed = new Property<>(0);
    public final Property<String> onSecondaryFixedHex = new Property<>("#000000");
    public final Property<Integer> onSecondaryFixedVariant = new Property<>(0);
    public final Property<String> onSecondaryFixedVariantHex = new Property<>("#000000");
    public final Property<Integer> tertiary = new Property<>(0);
    public final Property<String> tertiaryHex = new Property<>("#000000");
    public final Property<Integer> onTertiary = new Property<>(0);
    public final Property<String> onTertiaryHex = new Property<>("#000000");
    public final Property<Integer> tertiaryContainer = new Property<>(0);
    public final Property<String> tertiaryContainerHex = new Property<>("#000000");
    public final Property<Integer> onTertiaryContainer = new Property<>(0);
    public final Property<String> onTertiaryContainerHex = new Property<>("#000000");
    public final Property<Integer> tertiaryFixed = new Property<>(0);
    public final Property<String> tertiaryFixedHex = new Property<>("#000000");
    public final Property<Integer> tertiaryFixedDim = new Property<>(0);
    public final Property<String> tertiaryFixedDimHex = new Property<>("#000000");
    public final Property<Integer> onTertiaryFixed = new Property<>(0);
    public final Property<String> onTertiaryFixedHex = new Property<>("#000000");
    public final Property<Integer> onTertiaryFixedVariant = new Property<>(0);
    public final Property<String> onTertiaryFixedVariantHex = new Property<>("#000000");
    public final Property<Integer> error = new Property<>(0);
    public final Property<String> errorHex = new Property<>("#000000");
    public final Property<Integer> onError = new Property<>(0);
    public final Property<String> onErrorHex = new Property<>("#000000");
    public final Property<Integer> errorContainer = new Property<>(0);
    public final Property<String> errorContainerHex = new Property<>("#000000");
    public final Property<Integer> onErrorContainer = new Property<>(0);
    public final Property<String> onErrorContainerHex = new Property<>("#000000");
    public final Property<Integer> background = new Property<>(0);
    public final Property<String> backgroundHex = new Property<>("#000000");
    public final Property<Integer> onBackground = new Property<>(0);
    public final Property<String> onBackgroundHex = new Property<>("#000000");
    public final Property<Integer> surface = new Property<>(0);
    public final Property<String> surfaceHex = new Property<>("#000000");
    public final Property<Integer> onSurface = new Property<>(0);
    public final Property<String> onSurfaceHex = new Property<>("#000000");
    public final Property<Integer> surfaceVariant = new Property<>(0);
    public final Property<String> surfaceVariantHex = new Property<>("#000000");
    public final Property<Integer> onSurfaceVariant = new Property<>(0);
    public final Property<String> onSurfaceVariantHex = new Property<>("#000000");
    public final Property<Integer> surfaceTint = new Property<>(0);
    public final Property<String> surfaceTintHex = new Property<>("#000000");
    public final Property<Integer> inverseSurface = new Property<>(0);
    public final Property<String> inverseSurfaceHex = new Property<>("#000000");
    public final Property<Integer> inverseOnSurface = new Property<>(0);
    public final Property<String> inverseOnSurfaceHex = new Property<>("#000000");
    public final Property<Integer> outline = new Property<>(0);
    public final Property<String> outlineHex = new Property<>("#000000");
    public final Property<Integer> outlineVariant = new Property<>(0);
    public final Property<String> outlineVariantHex = new Property<>("#000000");
    public final Property<Integer> scrim = new Property<>(0);
    public final Property<String> scrimHex = new Property<>("#000000");
    public final Property<Integer> surfaceDim = new Property<>(0);
    public final Property<String> surfaceDimHex = new Property<>("#000000");
    public final Property<Integer> surfaceBright = new Property<>(0);
    public final Property<String> surfaceBrightHex = new Property<>("#000000");
    public final Property<Integer> surfaceContainerLowest = new Property<>(0);
    public final Property<String> surfaceContainerLowestHex = new Property<>("#000000");
    public final Property<Integer> surfaceContainerLow = new Property<>(0);
    public final Property<String> surfaceContainerLowHex = new Property<>("#000000");
    public final Property<Integer> surfaceContainer = new Property<>(0);
    public final Property<String> surfaceContainerHex = new Property<>("#000000");
    public final Property<Integer> surfaceContainerHigh = new Property<>(0);
    public final Property<String> surfaceContainerHighHex = new Property<>("#000000");
    public final Property<Integer> surfaceContainerHighest = new Property<>(0);
    public final Property<String> surfaceContainerHighestHex = new Property<>("#000000");

    // ------------------------------------------------------------------
    // Scheme state
    // ------------------------------------------------------------------

    /** True when the current scheme is the dark one. Reactive, so QML can rebind. */
    public final Property<Boolean> dark = new Property<>(Boolean.FALSE);
    /** The seed color as an ARGB integer. */
    public final Property<Integer> seed = new Property<>(DEFAULT_SEED);
    /** The seed color as {@code #RRGGBB} (what QML displays). */
    public final Property<String> seedHex = new Property<>(ColorScheme.hex(DEFAULT_SEED));
    /** Number of roles in the current scheme (48). */
    public final Property<Integer> roleCount = new Property<>(ColorScheme.ROLE_COUNT);

    /** The six MD3E spring tokens, read from QML as {@code Theme.spring.*}. */
    public final SpringBridge spring = new SpringBridge();

    // ------------------------------------------------------------------
    // MD3E shape tokens (Stage 2) - corner radii in px
    // ------------------------------------------------------------------
    // Frozen QML names: shapeNone..shapeFull. Values come from ShapeTokens; a component
    // writes radius: Theme.shapeM and never a literal. Property<Double> (not a constant)
    // so a future UI-scale setting can rescale every radius in one place.

    public final Property<Double> shapeNone = num(ShapeTokens.RADIUS_NONE);
    public final Property<Double> shapeXs = num(ShapeTokens.RADIUS_EXTRA_SMALL);
    public final Property<Double> shapeS = num(ShapeTokens.RADIUS_SMALL);
    public final Property<Double> shapeM = num(ShapeTokens.RADIUS_MEDIUM);
    public final Property<Double> shapeL = num(ShapeTokens.RADIUS_LARGE);
    public final Property<Double> shapeXl = num(ShapeTokens.RADIUS_EXTRA_LARGE);
    /** Pill/circle sentinel; components use ShapeTokens.resolve semantics (half the shorter side). */
    public final Property<Double> shapeFull = num(ShapeTokens.RADIUS_FULL);

    // ------------------------------------------------------------------
    // MD3E spacing tokens (Stage 2) - px, 4dp grid
    // ------------------------------------------------------------------

    public final Property<Double> spaceXs = num(SpacingTokens.XS);
    public final Property<Double> spaceS = num(SpacingTokens.S);
    public final Property<Double> spaceM = num(SpacingTokens.M);
    public final Property<Double> spaceL = num(SpacingTokens.L);
    public final Property<Double> spaceXl = num(SpacingTokens.XL);
    public final Property<Double> spaceXxl = num(SpacingTokens.XXL);

    // ------------------------------------------------------------------
    // MD3E typography tokens (Stage 2) - font sizes in px (1sp == 1px)
    // ------------------------------------------------------------------

    public final Property<Double> typeDisplayLarge = num(TypographyTokens.DISPLAY_LARGE.sizePx());
    public final Property<Double> typeDisplayMedium = num(TypographyTokens.DISPLAY_MEDIUM.sizePx());
    public final Property<Double> typeDisplaySmall = num(TypographyTokens.DISPLAY_SMALL.sizePx());
    public final Property<Double> typeHeadlineLarge = num(TypographyTokens.HEADLINE_LARGE.sizePx());
    public final Property<Double> typeHeadlineMedium = num(TypographyTokens.HEADLINE_MEDIUM.sizePx());
    public final Property<Double> typeHeadlineSmall = num(TypographyTokens.HEADLINE_SMALL.sizePx());
    public final Property<Double> typeTitleLarge = num(TypographyTokens.TITLE_LARGE.sizePx());
    public final Property<Double> typeTitleMedium = num(TypographyTokens.TITLE_MEDIUM.sizePx());
    public final Property<Double> typeTitleSmall = num(TypographyTokens.TITLE_SMALL.sizePx());
    public final Property<Double> typeBodyLarge = num(TypographyTokens.BODY_LARGE.sizePx());
    public final Property<Double> typeBodyMedium = num(TypographyTokens.BODY_MEDIUM.sizePx());
    public final Property<Double> typeBodySmall = num(TypographyTokens.BODY_SMALL.sizePx());
    public final Property<Double> typeLabelLarge = num(TypographyTokens.LABEL_LARGE.sizePx());
    public final Property<Double> typeLabelMedium = num(TypographyTokens.LABEL_MEDIUM.sizePx());
    public final Property<Double> typeLabelSmall = num(TypographyTokens.LABEL_SMALL.sizePx());

    private final Map<String, Property<Integer>> argbByName = new LinkedHashMap<>();
    private final Map<String, Property<String>> hexByName = new LinkedHashMap<>();

    /** Light scheme for the M3 baseline seed. */
    public ThemeBridge() {
        this(DEFAULT_SEED, false);
    }

    public ThemeBridge(int seedArgb, boolean darkMode) {
        register("primary", primary, primaryHex);
        register("onPrimary", onPrimary, onPrimaryHex);
        register("primaryContainer", primaryContainer, primaryContainerHex);
        register("onPrimaryContainer", onPrimaryContainer, onPrimaryContainerHex);
        register("inversePrimary", inversePrimary, inversePrimaryHex);
        register("primaryFixed", primaryFixed, primaryFixedHex);
        register("primaryFixedDim", primaryFixedDim, primaryFixedDimHex);
        register("onPrimaryFixed", onPrimaryFixed, onPrimaryFixedHex);
        register("onPrimaryFixedVariant", onPrimaryFixedVariant, onPrimaryFixedVariantHex);
        register("secondary", secondary, secondaryHex);
        register("onSecondary", onSecondary, onSecondaryHex);
        register("secondaryContainer", secondaryContainer, secondaryContainerHex);
        register("onSecondaryContainer", onSecondaryContainer, onSecondaryContainerHex);
        register("secondaryFixed", secondaryFixed, secondaryFixedHex);
        register("secondaryFixedDim", secondaryFixedDim, secondaryFixedDimHex);
        register("onSecondaryFixed", onSecondaryFixed, onSecondaryFixedHex);
        register("onSecondaryFixedVariant", onSecondaryFixedVariant, onSecondaryFixedVariantHex);
        register("tertiary", tertiary, tertiaryHex);
        register("onTertiary", onTertiary, onTertiaryHex);
        register("tertiaryContainer", tertiaryContainer, tertiaryContainerHex);
        register("onTertiaryContainer", onTertiaryContainer, onTertiaryContainerHex);
        register("tertiaryFixed", tertiaryFixed, tertiaryFixedHex);
        register("tertiaryFixedDim", tertiaryFixedDim, tertiaryFixedDimHex);
        register("onTertiaryFixed", onTertiaryFixed, onTertiaryFixedHex);
        register("onTertiaryFixedVariant", onTertiaryFixedVariant, onTertiaryFixedVariantHex);
        register("error", error, errorHex);
        register("onError", onError, onErrorHex);
        register("errorContainer", errorContainer, errorContainerHex);
        register("onErrorContainer", onErrorContainer, onErrorContainerHex);
        register("background", background, backgroundHex);
        register("onBackground", onBackground, onBackgroundHex);
        register("surface", surface, surfaceHex);
        register("onSurface", onSurface, onSurfaceHex);
        register("surfaceVariant", surfaceVariant, surfaceVariantHex);
        register("onSurfaceVariant", onSurfaceVariant, onSurfaceVariantHex);
        register("surfaceTint", surfaceTint, surfaceTintHex);
        register("inverseSurface", inverseSurface, inverseSurfaceHex);
        register("inverseOnSurface", inverseOnSurface, inverseOnSurfaceHex);
        register("outline", outline, outlineHex);
        register("outlineVariant", outlineVariant, outlineVariantHex);
        register("scrim", scrim, scrimHex);
        register("surfaceDim", surfaceDim, surfaceDimHex);
        register("surfaceBright", surfaceBright, surfaceBrightHex);
        register("surfaceContainerLowest", surfaceContainerLowest, surfaceContainerLowestHex);
        register("surfaceContainerLow", surfaceContainerLow, surfaceContainerLowHex);
        register("surfaceContainer", surfaceContainer, surfaceContainerHex);
        register("surfaceContainerHigh", surfaceContainerHigh, surfaceContainerHighHex);
        register("surfaceContainerHighest", surfaceContainerHighest, surfaceContainerHighestHex);

        this.seed.set(seedArgb | 0xFF000000);
        this.seedHex.set(ColorScheme.hex(seedArgb));
        this.dark.set(darkMode);
        regenerate();
    }

    // ------------------------------------------------------------------
    // Host-side mutation: regenerate every role from the seed
    // ------------------------------------------------------------------

    /** Switch scheme. No-op when the mode is unchanged (avoids needless QML invalidation). */
    public void setDark(boolean darkMode) {
        if (Boolean.valueOf(darkMode).equals(dark.peek())) return;
        dark.set(darkMode);
        regenerate();
    }

    /** Flip light &#8596; dark. This is what the demo's toggle button calls. */
    public void toggleDark() {
        setDark(!Boolean.TRUE.equals(dark.peek()));
    }

    /** Re-seed the whole scheme (a new HCT primary) and re-derive every role. */
    public void setSeed(int seedArgb) {
        int opaque = seedArgb | 0xFF000000;
        seed.set(opaque);
        seedHex.set(ColorScheme.hex(opaque));
        regenerate();
    }

    /** Push a freshly generated scheme into every role property. */
    private void regenerate() {
        apply(Md3eThemeGenerator.generate(seed.peek(), Boolean.TRUE.equals(dark.peek())));
    }

    private void apply(ColorScheme scheme) {
        for (Map.Entry<String, Integer> entry : scheme.roles().entrySet()) {
            Property<Integer> argb = argbByName.get(entry.getKey());
            Property<String> hex = hexByName.get(entry.getKey());
            if (argb == null || hex == null) {
                throw new IllegalStateException("Unbound MD3E role: " + entry.getKey());
            }
            argb.set(entry.getValue());
            hex.set(ColorScheme.hex(entry.getValue()));
        }
    }

    // ------------------------------------------------------------------
    // Read helpers (plain methods, callable from QML by exact arity)
    // ------------------------------------------------------------------

    /** ARGB of one role by name, e.g. {@code theme.roleArgb("primary")}. */
    public int roleArgb(String name) {
        return requireArgb(name).get();
    }

    /** {@code #RRGGBB} of one role by name. */
    public String roleHex(String name) {
        return requireHex(name).get();
    }

    private Property<Integer> requireArgb(String name) {
        Property<Integer> property = argbByName.get(name);
        if (property == null) throw new IllegalArgumentException("Unknown MD3E role: " + name);
        return property;
    }

    private Property<String> requireHex(String name) {
        Property<String> property = hexByName.get(name);
        if (property == null) throw new IllegalArgumentException("Unknown MD3E role: " + name);
        return property;
    }

    /** Numeric token property. Kept as Property (not a plain field) so a UI scale can retune it. */
    private static Property<Double> num(double value) {
        return new Property<>(value);
    }

    private void register(String name, Property<Integer> argb, Property<String> hex) {
        argbByName.put(name, argb);
        hexByName.put(name, hex);
    }
}
