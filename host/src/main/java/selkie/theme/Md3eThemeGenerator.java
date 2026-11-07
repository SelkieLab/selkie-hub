package selkie.theme;

import java.util.Objects;

/**
 * Generates the 48 MD3E color roles from a single seed ARGB.
 *
 * <p>Pipeline: seed -> HCT -> six tonal palettes (primary, secondary, tertiary, neutral,
 * neutralVariant, error) -> per-role tone lookup. Only hue and chroma come from the seed;
 * the light/dark tone of every role is fixed by the MD3E spec
 * ({@code references/design-tokens.md} section 1.1), so no role carries a literal color.
 *
 * <p>Dark mode is not an inversion: each role names two tones (e.g. primary 40/80,
 * onPrimary 100/20) and dark mode switches to the second column. That is why
 * {@code onPrimary} is near-white in light mode and near-black in dark mode while
 * {@code primary} moves from mid-dark to mid-light, and why the {@code *Fixed} roles
 * keep the same tone in both modes.
 */
public final class Md3eThemeGenerator {

    /** Exactly 48 MD3E color roles. */
    public static final int ROLE_COUNT = ColorScheme.ROLE_COUNT;

    /** A sensible default seed (M3 baseline purple) for apps without a chosen brand color. */
    public static final int DEFAULT_SEED = 0xFF6750A4;

    /** Chroma floors/caps: M3 derives palettes from seed chroma, clamped so tones stay in gamut. */
    private static final double PRIMARY_CHROMA_FLOOR = 48.0;
    private static final double PRIMARY_CHROMA_CAP = 96.0;
    private static final double SECONDARY_CHROMA_FLOOR = 16.0;
    private static final double SECONDARY_CHROMA_CAP = 32.0;
    private static final double TERTIARY_CHROMA_FLOOR = 24.0;
    private static final double TERTIARY_CHROMA_CAP = 48.0;
    private static final double NEUTRAL_CHROMA = 6.0;
    private static final double NEUTRAL_VARIANT_CHROMA = 8.0;
    private static final double ERROR_HUE = 25.0;
    private static final double ERROR_CHROMA = 84.0;
    private static final double TERTIARY_HUE_SHIFT = 60.0;

    private Md3eThemeGenerator() {
    }

    /** The six tonal palettes a scheme is sampled from. */
    public record Palettes(
            Hct.TonalPalette primary,
            Hct.TonalPalette secondary,
            Hct.TonalPalette tertiary,
            Hct.TonalPalette neutral,
            Hct.TonalPalette neutralVariant,
            Hct.TonalPalette error) {

        public Palettes {
            Objects.requireNonNull(primary, "primary");
            Objects.requireNonNull(secondary, "secondary");
            Objects.requireNonNull(tertiary, "tertiary");
            Objects.requireNonNull(neutral, "neutral");
            Objects.requireNonNull(neutralVariant, "neutralVariant");
            Objects.requireNonNull(error, "error");
        }
    }

    /**
     * Derives the six tonal palettes from a seed color.
     * Hue comes from the seed's CAM16 hue; chroma is scaled per palette with the
     * standard M3 ratios (primary 1x, secondary ~1/3, tertiary ~1/2, rotated +60 degrees).
     */
    public static Palettes palettes(int seedArgb) {
        Hct seed = Hct.fromInt(seedArgb);
        double hue = seed.hue();
        double chroma = seed.chroma();
        return new Palettes(
                Hct.TonalPalette.fromHueAndChroma(hue, clamp(chroma, PRIMARY_CHROMA_FLOOR, PRIMARY_CHROMA_CAP)),
                Hct.TonalPalette.fromHueAndChroma(hue, clamp(chroma / 3.0, SECONDARY_CHROMA_FLOOR, SECONDARY_CHROMA_CAP)),
                Hct.TonalPalette.fromHueAndChroma(hue + TERTIARY_HUE_SHIFT,
                        clamp(chroma / 2.0, TERTIARY_CHROMA_FLOOR, TERTIARY_CHROMA_CAP)),
                Hct.TonalPalette.fromHueAndChroma(hue, NEUTRAL_CHROMA),
                Hct.TonalPalette.fromHueAndChroma(hue, NEUTRAL_VARIANT_CHROMA),
                Hct.TonalPalette.fromHueAndChroma(ERROR_HUE, ERROR_CHROMA));
    }

    /**
     * Full 48-role MD3E scheme for a seed color.
     *
     * @param seedArgb any opaque ARGB seed color (the alpha channel is ignored)
     * @param dark     false for the light scheme, true for the dark scheme
     */
    public static ColorScheme generate(int seedArgb, boolean dark) {
        Palettes p = palettes(seedArgb);
        Hct.TonalPalette primary = p.primary();
        Hct.TonalPalette secondary = p.secondary();
        Hct.TonalPalette tertiary = p.tertiary();
        Hct.TonalPalette neutral = p.neutral();
        Hct.TonalPalette variant = p.neutralVariant();
        Hct.TonalPalette error = p.error();

        return new ColorScheme(
                // primary (9)                          light / dark tones
                tone(primary, 40, 80, dark),           // primary
                tone(primary, 100, 20, dark),          // onPrimary
                tone(primary, 90, 30, dark),           // primaryContainer
                tone(primary, 10, 90, dark),           // onPrimaryContainer
                tone(primary, 80, 40, dark),           // inversePrimary
                tone(primary, 90, 90, dark),           // primaryFixed
                tone(primary, 80, 80, dark),           // primaryFixedDim
                tone(primary, 10, 10, dark),           // onPrimaryFixed
                tone(primary, 30, 30, dark),           // onPrimaryFixedVariant
                // secondary (8)
                tone(secondary, 40, 80, dark),         // secondary
                tone(secondary, 100, 20, dark),        // onSecondary
                tone(secondary, 90, 30, dark),         // secondaryContainer
                tone(secondary, 10, 90, dark),         // onSecondaryContainer
                tone(secondary, 90, 90, dark),         // secondaryFixed
                tone(secondary, 80, 80, dark),         // secondaryFixedDim
                tone(secondary, 10, 10, dark),         // onSecondaryFixed
                tone(secondary, 30, 30, dark),         // onSecondaryFixedVariant
                // tertiary (8)
                tone(tertiary, 40, 80, dark),          // tertiary
                tone(tertiary, 100, 20, dark),         // onTertiary
                tone(tertiary, 90, 30, dark),          // tertiaryContainer
                tone(tertiary, 10, 90, dark),          // onTertiaryContainer
                tone(tertiary, 90, 90, dark),          // tertiaryFixed
                tone(tertiary, 80, 80, dark),          // tertiaryFixedDim
                tone(tertiary, 10, 10, dark),          // onTertiaryFixed
                tone(tertiary, 30, 30, dark),          // onTertiaryFixedVariant
                // error (4)
                tone(error, 40, 80, dark),             // error
                tone(error, 100, 20, dark),            // onError
                tone(error, 90, 30, dark),             // errorContainer
                tone(error, 10, 90, dark),             // onErrorContainer
                // surface / background (12)
                tone(neutral, 99, 10, dark),           // background
                tone(neutral, 10, 90, dark),           // onBackground
                tone(neutral, 99, 10, dark),           // surface
                tone(neutral, 10, 90, dark),           // onSurface
                tone(variant, 90, 30, dark),           // surfaceVariant
                tone(variant, 30, 80, dark),           // onSurfaceVariant
                tone(primary, 40, 80, dark),           // surfaceTint (== primary)
                tone(neutral, 20, 90, dark),           // inverseSurface
                tone(neutral, 95, 20, dark),           // inverseOnSurface
                tone(variant, 50, 60, dark),           // outline
                tone(variant, 80, 30, dark),           // outlineVariant
                tone(neutral, 0, 0, dark),             // scrim (pure black in both modes)
                // surface containers (7)
                tone(neutral, 87, 6, dark),            // surfaceDim
                tone(neutral, 98, 24, dark),           // surfaceBright
                tone(neutral, 100, 4, dark),           // surfaceContainerLowest
                tone(neutral, 96, 10, dark),           // surfaceContainerLow
                tone(neutral, 94, 12, dark),           // surfaceContainer
                tone(neutral, 92, 17, dark),           // surfaceContainerHigh
                tone(neutral, 90, 22, dark));          // surfaceContainerHighest
    }

    /** Light scheme for a seed. */
    public static ColorScheme light(int seedArgb) {
        return generate(seedArgb, false);
    }

    /** Dark scheme for a seed. */
    public static ColorScheme dark(int seedArgb) {
        return generate(seedArgb, true);
    }

    private static int tone(Hct.TonalPalette palette, int lightTone, int darkTone, boolean dark) {
        return palette.tone(dark ? darkTone : lightTone);
    }

    private static double clamp(double value, double floor, double cap) {
        return Math.min(cap, Math.max(floor, value));
    }
}
