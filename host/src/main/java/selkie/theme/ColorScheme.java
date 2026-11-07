package selkie.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 48 Material Design 3 Expressive color roles, as opaque ARGB integers.
 *
 * <p>The component order matches the MD3E token groups and the tone tables in
 * {@code references/design-tokens.md} (androidx.compose.material3 {@code ColorScheme}):
 * primary (9), secondary (8), tertiary (8), error (4), surface/base (12),
 * surface containers (7) = 48 roles.
 *
 * <p>Every value is produced from a seed-derived tonal palette; there are no literal
 * per-role colors anywhere in this project. {@code *Fixed} roles are intentionally
 * identical in light and dark schemes.
 */
public record ColorScheme(
        // primary (9)
        int primary,
        int onPrimary,
        int primaryContainer,
        int onPrimaryContainer,
        int inversePrimary,
        int primaryFixed,
        int primaryFixedDim,
        int onPrimaryFixed,
        int onPrimaryFixedVariant,
        // secondary (8)
        int secondary,
        int onSecondary,
        int secondaryContainer,
        int onSecondaryContainer,
        int secondaryFixed,
        int secondaryFixedDim,
        int onSecondaryFixed,
        int onSecondaryFixedVariant,
        // tertiary (8)
        int tertiary,
        int onTertiary,
        int tertiaryContainer,
        int onTertiaryContainer,
        int tertiaryFixed,
        int tertiaryFixedDim,
        int onTertiaryFixed,
        int onTertiaryFixedVariant,
        // error (4)
        int error,
        int onError,
        int errorContainer,
        int onErrorContainer,
        // surface / background (12)
        int background,
        int onBackground,
        int surface,
        int onSurface,
        int surfaceVariant,
        int onSurfaceVariant,
        int surfaceTint,
        int inverseSurface,
        int inverseOnSurface,
        int outline,
        int outlineVariant,
        int scrim,
        // surface containers (7)
        int surfaceDim,
        int surfaceBright,
        int surfaceContainerLowest,
        int surfaceContainerLow,
        int surfaceContainer,
        int surfaceContainerHigh,
        int surfaceContainerHighest) {

    /** Number of MD3E color roles. */
    public static final int ROLE_COUNT = 48;

    private static final List<String> ROLE_NAMES = List.of(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "inversePrimary", "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
            "surfaceTint", "inverseSurface", "inverseOnSurface", "outline", "outlineVariant", "scrim",
            "surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow",
            "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest");

    /** All 48 role names, in token-group order. */
    public static List<String> roleNames() {
        return ROLE_NAMES;
    }

    /** The scheme as an ordered name -&gt; ARGB map (48 entries). */
    public Map<String, Integer> roles() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("primary", primary);
        map.put("onPrimary", onPrimary);
        map.put("primaryContainer", primaryContainer);
        map.put("onPrimaryContainer", onPrimaryContainer);
        map.put("inversePrimary", inversePrimary);
        map.put("primaryFixed", primaryFixed);
        map.put("primaryFixedDim", primaryFixedDim);
        map.put("onPrimaryFixed", onPrimaryFixed);
        map.put("onPrimaryFixedVariant", onPrimaryFixedVariant);
        map.put("secondary", secondary);
        map.put("onSecondary", onSecondary);
        map.put("secondaryContainer", secondaryContainer);
        map.put("onSecondaryContainer", onSecondaryContainer);
        map.put("secondaryFixed", secondaryFixed);
        map.put("secondaryFixedDim", secondaryFixedDim);
        map.put("onSecondaryFixed", onSecondaryFixed);
        map.put("onSecondaryFixedVariant", onSecondaryFixedVariant);
        map.put("tertiary", tertiary);
        map.put("onTertiary", onTertiary);
        map.put("tertiaryContainer", tertiaryContainer);
        map.put("onTertiaryContainer", onTertiaryContainer);
        map.put("tertiaryFixed", tertiaryFixed);
        map.put("tertiaryFixedDim", tertiaryFixedDim);
        map.put("onTertiaryFixed", onTertiaryFixed);
        map.put("onTertiaryFixedVariant", onTertiaryFixedVariant);
        map.put("error", error);
        map.put("onError", onError);
        map.put("errorContainer", errorContainer);
        map.put("onErrorContainer", onErrorContainer);
        map.put("background", background);
        map.put("onBackground", onBackground);
        map.put("surface", surface);
        map.put("onSurface", onSurface);
        map.put("surfaceVariant", surfaceVariant);
        map.put("onSurfaceVariant", onSurfaceVariant);
        map.put("surfaceTint", surfaceTint);
        map.put("inverseSurface", inverseSurface);
        map.put("inverseOnSurface", inverseOnSurface);
        map.put("outline", outline);
        map.put("outlineVariant", outlineVariant);
        map.put("scrim", scrim);
        map.put("surfaceDim", surfaceDim);
        map.put("surfaceBright", surfaceBright);
        map.put("surfaceContainerLowest", surfaceContainerLowest);
        map.put("surfaceContainerLow", surfaceContainerLow);
        map.put("surfaceContainer", surfaceContainer);
        map.put("surfaceContainerHigh", surfaceContainerHigh);
        map.put("surfaceContainerHighest", surfaceContainerHighest);
        return Collections.unmodifiableMap(map);
    }

    /** ARGB for a role name (matches {@link #roleNames()}). */
    public int role(String name) {
        Integer value = roles().get(name);
        if (value == null) {
            throw new IllegalArgumentException("Unknown MD3E role: " + name);
        }
        return value;
    }

    // ------------------------------------------------------------------
    // WCAG 2.1 contrast (used by tests and by QML-side accessibility checks)
    // ------------------------------------------------------------------

    /** WCAG 2.1 relative luminance of an opaque ARGB color, in 0..1. */
    public static double relativeLuminance(int argb) {
        double r = wcagChannel((argb >> 16) & 0xFF);
        double g = wcagChannel((argb >> 8) & 0xFF);
        double b = wcagChannel(argb & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double wcagChannel(int channel) {
        double s = channel / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** WCAG 2.1 contrast ratio between two opaque colors, in 1..21. */
    public static double contrastRatio(int foreground, int background) {
        double a = relativeLuminance(foreground);
        double b = relativeLuminance(background);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /** {@code #RRGGBB} for logging and QML string export. */
    public static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    /** Human readable dump: role name, hex, and the HCT tone it resolved to. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : roles().entrySet()) {
            Hct hct = Hct.fromInt(entry.getValue());
            sb.append(String.format("%-24s %s  tone %6.2f  chroma %6.2f",
                    entry.getKey(), hex(entry.getValue()), hct.tone(), hct.chroma()));
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
