package selkie.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for the MD3E theme generator, plus a plain {@code main()} self-check
 * so the same assertions can be run with only a JDK (no Gradle, no JUnit on the classpath).
 *
 * <p>The expected tones below are transcribed from the MD3E spec
 * ({@code references/design-tokens.md} section 1.1), deliberately <em>not</em> imported from
 * the generator, so the test verifies the implementation against the spec rather than
 * against itself.
 */
public class ThemeTest {

    /** Seeds spanning saturated blue, purple, green, orange and a near-gray. */
    private static final int[] SEEDS = {
        0xFF4285F4, // Google blue
        0xFF6750A4, // M3 baseline purple
        0xFF4CAF50, // green
        0xFFFF6D00, // vivid orange
        0xFF808080  // gray (chroma floor must still be applied)
    };

    /** Light/dark tone per role, straight from the MD3E token table. */
    private static final Map<String, int[]> SPEC_TONES = new LinkedHashMap<>();

    static {
        // primary
        put("primary", 40, 80);
        put("onPrimary", 100, 20);
        put("primaryContainer", 90, 30);
        put("onPrimaryContainer", 10, 90);
        put("inversePrimary", 80, 40);
        put("primaryFixed", 90, 90);
        put("primaryFixedDim", 80, 80);
        put("onPrimaryFixed", 10, 10);
        put("onPrimaryFixedVariant", 30, 30);
        // secondary
        put("secondary", 40, 80);
        put("onSecondary", 100, 20);
        put("secondaryContainer", 90, 30);
        put("onSecondaryContainer", 10, 90);
        put("secondaryFixed", 90, 90);
        put("secondaryFixedDim", 80, 80);
        put("onSecondaryFixed", 10, 10);
        put("onSecondaryFixedVariant", 30, 30);
        // tertiary
        put("tertiary", 40, 80);
        put("onTertiary", 100, 20);
        put("tertiaryContainer", 90, 30);
        put("onTertiaryContainer", 10, 90);
        put("tertiaryFixed", 90, 90);
        put("tertiaryFixedDim", 80, 80);
        put("onTertiaryFixed", 10, 10);
        put("onTertiaryFixedVariant", 30, 30);
        // error
        put("error", 40, 80);
        put("onError", 100, 20);
        put("errorContainer", 90, 30);
        put("onErrorContainer", 10, 90);
        // surface / background
        put("background", 99, 10);
        put("onBackground", 10, 90);
        put("surface", 99, 10);
        put("onSurface", 10, 90);
        put("surfaceVariant", 90, 30);
        put("onSurfaceVariant", 30, 80);
        put("surfaceTint", 40, 80);
        put("inverseSurface", 20, 90);
        put("inverseOnSurface", 95, 20);
        put("outline", 50, 60);
        put("outlineVariant", 80, 30);
        put("scrim", 0, 0);
        // surface containers
        put("surfaceDim", 87, 6);
        put("surfaceBright", 98, 24);
        put("surfaceContainerLowest", 100, 4);
        put("surfaceContainerLow", 96, 10);
        put("surfaceContainer", 94, 12);
        put("surfaceContainerHigh", 92, 17);
        put("surfaceContainerHighest", 90, 22);
    }

    private static void put(String role, int light, int dark) {
        SPEC_TONES.put(role, new int[] {light, dark});
    }

    /** Tolerance in tone units: 8-bit quantization plus gamut clipping. */
    private static final double TONE_TOLERANCE = 0.75;

    // ==================================================================
    // JUnit entry points (JUnit 5 treats any thrown Throwable as failure)
    // ==================================================================

    @Test
    public void roleCountIs48() {
        assertEquals(48, ColorScheme.ROLE_COUNT, "MD3E has exactly 48 color roles");
        assertEquals(48, ColorScheme.roleNames().size(), "role name list");
        assertEquals(48, Md3eThemeGenerator.generate(SEEDS[0], false).roles().size(), "light roles");
        assertEquals(48, Md3eThemeGenerator.generate(SEEDS[0], true).roles().size(), "dark roles");
        verifyRoleCount();
    }

    @Test
    public void toneMappingMatchesMd3eSpec() {
        verifyToneMapping();
    }

    @Test
    public void contrastRatiosMeetMd3eExpectations() {
        verifyContrast();
    }

    @Test
    public void fixedRolesAreIdenticalInLightAndDark() {
        verifyFixedRoles();
    }

    @Test
    public void hctMathIsFaithful() {
        verifyHct();
    }

    @Test
    public void palettesDeriveFromSeed() {
        verifyPalettes();
    }

    @Test
    public void matchesPublishedM3BaselinePalette() {
        verifyPublishedBaseline();
    }

    @Test
    public void shapeTokensMatchContract() {
        verifyShapeTokens();
    }

    @Test
    public void spacingTokensMatchContract() {
        verifySpacingTokens();
    }

    @Test
    public void typographyTokensMatchContract() {
        verifyTypographyTokens();
    }

    @Test
    public void themeQmlDelegatesEveryToken() {
        verifyThemeQmlContract();
    }

    @Test
    public void themeBridgeExposesEveryToken() {
        verifyThemeBridgeContract();
    }

    @Test
    public void motionTokensMatchMd3eMotionScheme() {
        verifyMotionTokens();
    }

    @Test
    public void effectsTokensNeverOvershoot() {
        verifyEffectsNeverOvershoot();
    }

    // ==================================================================
    // JDK-only verification bodies (also used by main())
    // ==================================================================

    static void verifyRoleCount() {
        List<String> names = ColorScheme.roleNames();
        check(names.size() == 48, "expected 48 role names, got " + names.size());
        check(new java.util.HashSet<>(names).size() == 48, "role names must be unique");
        check(ColorScheme.roleNames().equals(ColorScheme.roleNames()), "role names are stable");

        ColorScheme scheme = Md3eThemeGenerator.generate(SEEDS[0], false);
        Map<String, Integer> roles = scheme.roles();
        check(roles.size() == 48, "scheme exposes 48 roles, got " + roles.size());
        for (String name : names) {
            check(roles.containsKey(name), "missing role: " + name);
            check((roles.get(name) >>> 24) == 0xFF, "role must be opaque: " + name);
        }
        for (String specName : SPEC_TONES.keySet()) {
            check(names.contains(specName), "spec role not exposed: " + specName);
        }
    }

    static void verifyToneMapping() {
        double maxDeviation = 0.0;
        String worst = "";
        for (int seed : SEEDS) {
            for (boolean dark : new boolean[] {false, true}) {
                ColorScheme scheme = Md3eThemeGenerator.generate(seed, dark);
                for (Map.Entry<String, int[]> entry : SPEC_TONES.entrySet()) {
                    String role = entry.getKey();
                    int expected = dark ? entry.getValue()[1] : entry.getValue()[0];
                    double actual = Hct.fromInt(scheme.role(role)).tone();
                    double deviation = Math.abs(actual - expected);
                    if (deviation > maxDeviation) {
                        maxDeviation = deviation;
                        worst = String.format("%s seed=%08X dark=%s expected=%d actual=%.3f",
                                role, seed, dark, expected, actual);
                    }
                    check(deviation <= TONE_TOLERANCE,
                            "tone out of spec for " + role + " seed=" + ColorScheme.hex(seed)
                                    + " dark=" + dark + ": expected tone " + expected
                                    + " but measured " + String.format("%.3f", actual));
                }
            }
        }
        System.out.printf("  tone mapping: max deviation %.3f tone units (%s)%n", maxDeviation, worst);
    }

    /** Foreground/background role pairs and their MD3 accessibility floor. */
    private static final String[][] CONTRAST_PAIRS = {
        {"onPrimary", "primary", "4.5"},
        {"onSecondary", "secondary", "4.5"},
        {"onTertiary", "tertiary", "4.5"},
        {"onError", "error", "4.5"},
        {"onPrimaryContainer", "primaryContainer", "4.5"},
        {"onSecondaryContainer", "secondaryContainer", "4.5"},
        {"onTertiaryContainer", "tertiaryContainer", "4.5"},
        {"onErrorContainer", "errorContainer", "4.5"},
        {"onSurface", "surface", "7.0"},
        {"onBackground", "background", "7.0"},
        {"onSurfaceVariant", "surfaceVariant", "4.5"},
        {"inverseOnSurface", "inverseSurface", "4.5"},
        {"onPrimaryFixed", "primaryFixed", "4.5"},
        {"onSecondaryFixed", "secondaryFixed", "4.5"},
        {"onTertiaryFixed", "tertiaryFixed", "4.5"},
        {"onPrimaryFixedVariant", "primaryFixedDim", "4.5"},
        {"surfaceTint", "surface", "1.4"},
        {"outline", "surface", "3.0"},
        {"outlineVariant", "surface", "1.4"}
    };

    static void verifyContrast() {
        System.out.println("  measured WCAG contrast ratios (target >= 4.5 for on* text roles):");
        for (int seed : SEEDS) {
            for (boolean dark : new boolean[] {false, true}) {
                ColorScheme scheme = Md3eThemeGenerator.generate(seed, dark);
                StringBuilder line = new StringBuilder();
                line.append(String.format("    seed %s %s:", ColorScheme.hex(seed), dark ? "dark " : "light"));
                for (String[] pair : CONTRAST_PAIRS) {
                    String fg = pair[0];
                    String bg = pair[1];
                    double floor = Double.parseDouble(pair[2]);
                    double ratio = ColorScheme.contrastRatio(scheme.role(fg), scheme.role(bg));
                    line.append(String.format(" %s/%s=%.2f", fg, bg, ratio));
                    check(ratio >= floor,
                            "contrast too low for " + fg + "/" + bg + " seed=" + ColorScheme.hex(seed)
                                    + " dark=" + dark + ": " + String.format("%.3f", ratio)
                                    + " < " + floor);
                }
                System.out.println(line);
            }
        }
    }

    static void verifyFixedRoles() {
        String[] fixedRoles = {
            "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
            "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
            "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant"
        };
        for (int seed : SEEDS) {
            ColorScheme light = Md3eThemeGenerator.generate(seed, false);
            ColorScheme dark = Md3eThemeGenerator.generate(seed, true);
            for (String role : fixedRoles) {
                checkEquals(light.role(role), dark.role(role),
                        "fixed role must not change with theme: " + role);
            }
            // Non-fixed roles must actually change: dark mode is a different tonal mapping,
            // not an inversion and definitely not a copy.
            checkNotEquals(light.primary(), dark.primary(), "primary must differ light vs dark");
            checkNotEquals(light.surface(), dark.surface(), "surface must differ light vs dark");
            check(Hct.fromInt(dark.surface()).tone() < 15.0, "dark surface must be dark");
            check(Hct.fromInt(light.surface()).tone() > 90.0, "light surface must be light");
        }
    }

    static void verifyHct() {
        // CIE L* of the primaries: pure, independently computable reference values.
        checkApprox(53.24, Hct.fromInt(0xFFFF0000).tone(), 0.3, "tone of pure red (CIE L*)");
        checkApprox(87.74, Hct.fromInt(0xFF00FF00).tone(), 0.3, "tone of pure green (CIE L*)");
        checkApprox(32.30, Hct.fromInt(0xFF0000FF).tone(), 0.3, "tone of pure blue (CIE L*)");
        checkApprox(100.0, Hct.fromInt(0xFFFFFFFF).tone(), 0.01, "tone of white");
        checkApprox(0.0, Hct.fromInt(0xFF000000).tone(), 0.01, "tone of black");

        // CAM16 hue/chroma sanity: these are not HSL or HSV values.
        Hct red = Hct.fromInt(0xFFFF0000);
        check(red.hue() > 20.0 && red.hue() < 35.0, "CAM16 hue of red, got " + red.hue());
        check(red.chroma() > 100.0, "CAM16 chroma of red must be very high, got " + red.chroma());
        // HSL/HSV would call pure red hue 0 and mid gray lightness 50; HCT does neither.
        check(red.hue() > 15.0, "pure red has CAM16 hue ~27, not the HSL/HSV hue 0");
        Hct tint = Hct.fromInt(0xFFFF8080); // same HSL hue as pure red, lighter tint
        check(Math.abs(tint.hue() - red.hue()) < 10.0,
                "tint of red keeps a nearby CAM16 hue, got " + tint.hue());
        check(tint.tone() > red.tone(), "tint of red must have a higher L* tone");
        check(tint.chroma() < red.chroma(), "tint of red must have lower chroma");

        // Gray has no chroma, and L* of mid gray is ~53.6 (not 50 like HSL lightness).
        Hct gray = Hct.fromInt(0xFF808080);
        check(gray.chroma() < 2.0, "gray must have ~0 chroma, got " + gray.chroma());
        checkApprox(53.59, gray.tone(), 0.2, "tone of mid gray is CIE L*, not HSL L");

        // Round trip: argb -> HCT -> argb must be lossless within one 8-bit step.
        int[] colors = {
            0xFF4285F4, 0xFF6750A4, 0xFF4CAF50, 0xFFFF6D00, 0xFF123456, 0xFF7F7F7F,
            0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF010203, 0xFFFEFDFC, 0xFF00FFFF
        };
        for (int argb : colors) {
            Hct hct = Hct.fromInt(argb);
            int rebuilt = Hct.argbFrom(hct.hue(), hct.chroma(), hct.tone());
            for (int shift = 0; shift <= 16; shift += 8) {
                int expected = (argb >> shift) & 0xFF;
                int actual = (rebuilt >> shift) & 0xFF;
                check(Math.abs(expected - actual) <= 2,
                        "HCT round trip drifted for " + ColorScheme.hex(argb) + " at shift " + shift
                                + ": expected " + expected + " got " + actual);
            }
        }

        // Tone is preserved by the inverse even where chroma has to be given up.
        for (int tone = 0; tone <= 100; tone += 5) {
            for (double chroma : new double[] {0.0, 12.0, 48.0, 120.0}) {
                int argb = Hct.argbFrom(266.0, chroma, tone);
                double measured = Hct.fromInt(argb).tone();
                check(Math.abs(measured - tone) <= TONE_TOLERANCE,
                        "tonal palette must preserve tone " + tone + " at chroma " + chroma
                                + ", measured " + measured);
            }
        }
    }

    static void verifyPalettes() {
        for (int seed : SEEDS) {
            Hct seedHct = Hct.fromInt(seed);
            Md3eThemeGenerator.Palettes p = Md3eThemeGenerator.palettes(seed);

            check(Math.abs(p.primary().hue() - seedHct.hue()) < 0.001,
                    "primary keeps the seed hue");
            check(p.primary().chroma() >= 48.0,
                    "primary chroma floor 48, got " + p.primary().chroma());
            check(p.primary().chroma() <= 96.0,
                    "primary chroma cap 96, got " + p.primary().chroma());
            check(Math.abs(p.tertiary().hue() - (seedHct.hue() + 60.0) % 360.0) < 0.001,
                    "tertiary is the seed hue rotated by 60 degrees");
            check(p.secondary().chroma() < p.primary().chroma(),
                    "secondary is more desaturated than primary");
            check(p.neutral().chroma() < p.secondary().chroma(),
                    "neutral is the most desaturated palette");
            check(p.neutralVariant().chroma() >= p.neutral().chroma(),
                    "neutralVariant carries slightly more chroma than neutral");
            check(p.error().chroma() > 80.0, "error palette is the standard M3 red");

            // A gray seed still yields a chromatic primary (M3 chroma floor), so a neutral
            // brand color does not collapse the whole theme to grayscale.
            if ((seed & 0xFFFFFF) == 0x808080) {
                check(p.primary().chroma() == 48.0, "gray seed must be lifted to chroma 48");
                ColorScheme scheme = Md3eThemeGenerator.generate(seed, false);
                double chroma = Hct.fromInt(scheme.primary()).chroma();
                check(chroma > 20.0, "gray seed primary must still be chromatic, got " + chroma);
            }
        }
    }

    /**
     * The published Material 3 baseline light scheme for seed {@code #6750A4}, used as an
     * independent oracle for the whole pipeline (seed -> HCT -> palettes -> roles).
     * A correct HCT implementation reproduces these within a couple of 8-bit steps.
     */
    private static final Map<String, String> M3_BASELINE_LIGHT = new LinkedHashMap<>();

    static {
        M3_BASELINE_LIGHT.put("primary", "6750A4");
        M3_BASELINE_LIGHT.put("onPrimary", "FFFFFF");
        M3_BASELINE_LIGHT.put("primaryContainer", "EADDFF");
        M3_BASELINE_LIGHT.put("onPrimaryContainer", "21005D");
        M3_BASELINE_LIGHT.put("inversePrimary", "D0BCFF");
        M3_BASELINE_LIGHT.put("secondary", "625B71");
        M3_BASELINE_LIGHT.put("onSecondary", "FFFFFF");
        M3_BASELINE_LIGHT.put("secondaryContainer", "E8DEF8");
        M3_BASELINE_LIGHT.put("onSecondaryContainer", "1D192B");
        M3_BASELINE_LIGHT.put("tertiary", "7D5260");
        M3_BASELINE_LIGHT.put("onTertiary", "FFFFFF");
        M3_BASELINE_LIGHT.put("tertiaryContainer", "FFD8E4");
        M3_BASELINE_LIGHT.put("onTertiaryContainer", "31111D");
        M3_BASELINE_LIGHT.put("error", "BA1A1A");
        M3_BASELINE_LIGHT.put("onError", "FFFFFF");
        M3_BASELINE_LIGHT.put("errorContainer", "FFDAD6");
        M3_BASELINE_LIGHT.put("onErrorContainer", "410002");
        M3_BASELINE_LIGHT.put("surface", "FFFBFF");
        M3_BASELINE_LIGHT.put("onSurface", "1D1B20");
        M3_BASELINE_LIGHT.put("surfaceVariant", "E7E0EC");
        M3_BASELINE_LIGHT.put("onSurfaceVariant", "49454F");
        M3_BASELINE_LIGHT.put("surfaceTint", "6750A4");
        M3_BASELINE_LIGHT.put("outline", "79747E");
        M3_BASELINE_LIGHT.put("outlineVariant", "CAC4D0");
        M3_BASELINE_LIGHT.put("inverseSurface", "322F35");
        M3_BASELINE_LIGHT.put("inverseOnSurface", "F5EFF7");
    }

    /** Max per-channel deviation allowed against the published baseline (8-bit steps). */
    private static final int BASELINE_TOLERANCE = 6;

    static void verifyPublishedBaseline() {
        ColorScheme light = Md3eThemeGenerator.generate(0xFF6750A4, false);
        int worst = 0;
        String worstRole = "";
        for (Map.Entry<String, String> entry : M3_BASELINE_LIGHT.entrySet()) {
            int actual = light.role(entry.getKey());
            int expected = Integer.parseInt(entry.getValue(), 16) | 0xFF000000;
            int deviation = Math.max(
                    Math.abs(((actual >> 16) & 0xFF) - ((expected >> 16) & 0xFF)),
                    Math.max(Math.abs(((actual >> 8) & 0xFF) - ((expected >> 8) & 0xFF)),
                            Math.abs((actual & 0xFF) - (expected & 0xFF))));
            if (deviation > worst) {
                worst = deviation;
                worstRole = entry.getKey();
            }
            check(deviation <= BASELINE_TOLERANCE,
                    "role " + entry.getKey() + " differs from the published M3 baseline: ours "
                            + ColorScheme.hex(actual) + " vs published #" + entry.getValue()
                            + " (" + deviation + " steps)");
        }
        // Dark baseline spot checks (same seed).
        ColorScheme dark = Md3eThemeGenerator.generate(0xFF6750A4, true);
        checkChannelEquals(dark.primary(), "D0BCFF", "dark primary");
        checkChannelEquals(dark.onPrimary(), "381E72", "dark onPrimary");
        System.out.println("  baseline: reproduced published M3 light scheme for #6750A4, worst role "
                + worstRole + " at " + worst + " of 255 steps");
    }

    private static void checkChannelEquals(int actual, String hex, String label) {
        int expected = Integer.parseInt(hex, 16) | 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = (actual >> shift) & 0xFF;
            int b = (expected >> shift) & 0xFF;
            check(Math.abs(a - b) <= BASELINE_TOLERANCE,
                    label + " differs from published #" + hex + ": ours " + ColorScheme.hex(actual)
                            + " channel delta " + Math.abs(a - b));
        }
    }

    // ==================================================================
    // Stage 2: shape / spacing / typography token contracts
    // ==================================================================

    /** Lead-fixed shape contract: token -> corner radius px. */
    private static final Map<String, Integer> CONTRACT_SHAPE = new LinkedHashMap<>();
    /** Lead-fixed spacing contract: token -> px (4dp grid). */
    private static final Map<String, Integer> CONTRACT_SPACE = new LinkedHashMap<>();
    /** Lead-fixed typography contract: token -> font size px. */
    private static final Map<String, Integer> CONTRACT_TYPE = new LinkedHashMap<>();

    static {
        CONTRACT_SHAPE.put("shapeNone", 0);
        CONTRACT_SHAPE.put("shapeXs", 4);
        CONTRACT_SHAPE.put("shapeS", 8);
        CONTRACT_SHAPE.put("shapeM", 12);
        CONTRACT_SHAPE.put("shapeL", 16);
        CONTRACT_SHAPE.put("shapeXl", 28);
        CONTRACT_SHAPE.put("shapeFull", 999);

        CONTRACT_SPACE.put("spaceXs", 4);
        CONTRACT_SPACE.put("spaceS", 8);
        CONTRACT_SPACE.put("spaceM", 12);
        CONTRACT_SPACE.put("spaceL", 16);
        CONTRACT_SPACE.put("spaceXl", 24);
        CONTRACT_SPACE.put("spaceXxl", 32);

        CONTRACT_TYPE.put("typeDisplayLarge", 57);
        CONTRACT_TYPE.put("typeDisplayMedium", 45);
        CONTRACT_TYPE.put("typeDisplaySmall", 36);
        CONTRACT_TYPE.put("typeHeadlineLarge", 32);
        CONTRACT_TYPE.put("typeHeadlineMedium", 28);
        CONTRACT_TYPE.put("typeHeadlineSmall", 24);
        CONTRACT_TYPE.put("typeTitleLarge", 22);
        CONTRACT_TYPE.put("typeTitleMedium", 16);
        CONTRACT_TYPE.put("typeTitleSmall", 14);
        CONTRACT_TYPE.put("typeBodyLarge", 16);
        CONTRACT_TYPE.put("typeBodyMedium", 14);
        CONTRACT_TYPE.put("typeBodySmall", 12);
        CONTRACT_TYPE.put("typeLabelLarge", 14);
        CONTRACT_TYPE.put("typeLabelMedium", 12);
        CONTRACT_TYPE.put("typeLabelSmall", 11);
    }

    static void verifyShapeTokens() {
        checkMapEquals(CONTRACT_SHAPE, ShapeTokens.tokens(), "shape contract");
        checkEquals(7, ShapeTokens.CONTRACT_COUNT, "shape token count");

        // The MD3E scale itself (design-tokens.md section 3), including the M3E-only sizes.
        Map<String, Integer> scale = ShapeTokens.md3eScale();
        checkEquals(8, scale.size(), "MD3E shape scale has 8 sizes");
        checkEquals(4, scale.get("extraSmall").intValue(), "extraSmall");
        checkEquals(8, scale.get("small").intValue(), "small");
        checkEquals(12, scale.get("medium").intValue(), "medium");
        checkEquals(16, scale.get("large").intValue(), "large");
        checkEquals(20, scale.get("largeIncreased").intValue(), "largeIncreased");
        checkEquals(28, scale.get("extraLarge").intValue(), "extraLarge");
        checkEquals(32, scale.get("extraLargeIncreased").intValue(), "extraLargeIncreased");
        checkEquals(48, scale.get("extraExtraLarge").intValue(), "extraExtraLarge");

        // Real radii ascend and sit on the 4px grid; shapeFull is a sentinel, not a radius.
        int previous = -1;
        for (Map.Entry<String, Integer> entry : CONTRACT_SHAPE.entrySet()) {
            if (entry.getKey().equals("shapeFull")) {
                continue;
            }
            int radius = entry.getValue();
            check(radius > previous, "shape tokens must ascend: " + entry.getKey());
            check(radius % 4 == 0, "shape radius on the 4px grid: " + entry.getKey());
            previous = radius;
        }

        // shapeFull must be large enough that no widget in this app gets clipped corners.
        check(ShapeTokens.RADIUS_FULL >= 400, "shapeFull must exceed twice any realistic widget side");
        check(ShapeTokens.isFull(ShapeTokens.RADIUS_FULL), "shapeFull must read as the pill sentinel");
        check(!ShapeTokens.isFull(ShapeTokens.RADIUS_EXTRA_LARGE), "shapeXl is a real radius");

        // resolve(): sentinel -> half the shorter side, real radius -> itself.
        checkApprox(20.0, ShapeTokens.resolve(ShapeTokens.RADIUS_FULL, 200, 40), 1e-9,
                "shapeFull on a 40px tall widget becomes a pill");
        checkApprox(12.0, ShapeTokens.resolve(ShapeTokens.RADIUS_MEDIUM, 200, 40), 1e-9,
                "real radii pass through when they fit");
        checkApprox(6.0, ShapeTokens.resolve(ShapeTokens.RADIUS_MEDIUM, 200, 12), 1e-9,
                "real radii clamp to half the shorter side");
        checkApprox(999.0, ShapeTokens.radius("shapeFull"), 1e-9, "radius(name) lookup");
        try {
            ShapeTokens.radius("shapeHuge");
            throw new AssertionError("unknown shape token must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    static void verifySpacingTokens() {
        checkMapEquals(CONTRACT_SPACE, SpacingTokens.tokens(), "spacing contract");
        checkEquals(6, SpacingTokens.CONTRACT_COUNT, "spacing token count");
        checkEquals(16, SpacingTokens.px("spaceL"), "16dp is the standard content padding");
        checkEquals(4, SpacingTokens.GRID, "4dp baseline grid");

        int previous = 0;
        for (Map.Entry<String, Integer> entry : CONTRACT_SPACE.entrySet()) {
            int px = entry.getValue();
            check(px > previous, "spacing tokens must strictly ascend: " + entry.getKey());
            check(px % SpacingTokens.GRID == 0, "spacing on the 4dp grid: " + entry.getKey());
            previous = px;
        }
        try {
            SpacingTokens.px("spaceHuge");
            throw new AssertionError("unknown spacing token must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    static void verifyTypographyTokens() {
        Map<String, TypographyTokens.TypeStyle> styles = TypographyTokens.styles();
        checkEquals(15, styles.size(), "MD3E has 15 base type styles");
        checkEquals(15, TypographyTokens.CONTRACT_COUNT, "typography token count");
        check(new ArrayList<>(CONTRACT_TYPE.keySet()).equals(new ArrayList<>(styles.keySet())),
                "typography contract names/order: expected " + CONTRACT_TYPE.keySet()
                        + " but got " + styles.keySet());

        for (Map.Entry<String, Integer> entry : CONTRACT_TYPE.entrySet()) {
            TypographyTokens.TypeStyle style = styles.get(entry.getKey());
            check(style != null, "missing type style " + entry.getKey());
            checkApprox(entry.getValue(), style.sizePx(), 1e-9, "font size of " + entry.getKey());
            check(style.lineHeightPx() >= style.sizePx(),
                    "line height must be >= font size: " + entry.getKey());
            check(style.weight() == 400 || style.weight() == 500,
                    "MD3E base styles use regular(400) or medium(500): " + entry.getKey());
        }

        // Sizes decrease inside each family, and families descend display > headline > title > body.
        String[][] families = {
            {"typeDisplayLarge", "typeDisplayMedium", "typeDisplaySmall"},
            {"typeHeadlineLarge", "typeHeadlineMedium", "typeHeadlineSmall"},
            {"typeTitleLarge", "typeTitleMedium", "typeTitleSmall"},
            {"typeBodyLarge", "typeBodyMedium", "typeBodySmall"},
            {"typeLabelLarge", "typeLabelMedium", "typeLabelSmall"}
        };
        for (String[] family : families) {
            double previous = Double.MAX_VALUE;
            for (String name : family) {
                double size = TypographyTokens.sizePx(name);
                check(size < previous, "type sizes must descend within a family: " + name);
                previous = size;
            }
        }
        check(TypographyTokens.sizePx("typeDisplayLarge") > TypographyTokens.sizePx("typeHeadlineLarge"),
                "display outranks headline");
        check(TypographyTokens.sizePx("typeHeadlineSmall") > TypographyTokens.sizePx("typeTitleLarge"),
                "headline outranks title");

        // Weights from the MD3E table: titles/labels are medium, display/headline/body regular.
        check(TypographyTokens.TITLE_LARGE.weight() == 500, "titleLarge is medium");
        check(TypographyTokens.LABEL_LARGE.weight() == 500, "labelLarge is medium (button text)");
        check(TypographyTokens.BODY_MEDIUM.weight() == 400, "bodyMedium is regular");
        checkApprox(0.5, TypographyTokens.BODY_LARGE.letterSpacingPx(), 1e-9, "bodyLarge tracking");
        checkApprox(-0.25, TypographyTokens.DISPLAY_LARGE.letterSpacingPx(), 1e-9, "displayLarge tracking");
        checkApprox(64.0, TypographyTokens.DISPLAY_LARGE.lineHeightPx(), 1e-9, "displayLarge line height");

        try {
            TypographyTokens.sizePx("typeHuge");
            throw new AssertionError("unknown typography token must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Guard the QML half of the contract: every frozen name is delegated in qml/Theme.qml, the
     * 48 role names and six spring tokens are still there (main.qml depends on them), and the
     * file contains no literal colors.
     */
    static void verifyThemeQmlContract() {
        java.nio.file.Path path = locateThemeQml();
        if (path == null) {
            System.out.println("  (skipped Theme.qml contract: qml/Theme.qml not found from "
                    + System.getProperty("user.dir") + ")");
            return;
        }
        String qml;
        try {
            qml = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError("cannot read " + path + ": " + e);
        }
        check(qml.contains("pragma Singleton"), "Theme.qml must stay a pragma Singleton");
        // Comments may mention "#RRGGBB" as documentation; real code must never carry a literal.
        String code = qml.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .collect(java.util.stream.Collectors.joining("\n"));
        check(!code.contains("\"#"), "Theme.qml must delegate colors, never hardcode a hex literal");

        for (String role : ColorScheme.roleNames()) {
            contains(qml, "property color " + role + ": theme." + role + "Hex", "color delegation " + role);
            contains(qml, "property string " + role + "Hex: theme." + role + "Hex",
                    "hex delegation " + role);
        }
        for (String state : new String[] {
            "property bool dark: theme.dark",
            "property string seedHex: theme.seedHex",
            "property int roleCount: theme.roleCount"
        }) {
            contains(qml, state, "scheme state: " + state);
        }
        for (String name : CONTRACT_SHAPE.keySet()) {
            contains(qml, "property real " + name + ": theme." + name, "shape token " + name);
        }
        for (String name : CONTRACT_SPACE.keySet()) {
            contains(qml, "property real " + name + ": theme." + name, "spacing token " + name);
        }
        for (String name : CONTRACT_TYPE.keySet()) {
            contains(qml, "property real " + name + ": theme." + name, "type token " + name);
        }
        for (String spring : new String[] {
            "fastSpatial", "defaultSpatial", "slowSpatial", "fastEffects", "defaultEffects", "slowEffects"
        }) {
            contains(qml, "property string " + spring + ": theme.spring." + spring, "spring token " + spring);
        }
        System.out.println("  Theme.qml: 48 roleHex + 6 springs + 7 shape + 6 space + 15 type delegations present");
    }

    /**
     * Guard the Java half of the contract: every frozen name is a public numeric property on
     * ThemeBridge with the contract value. Uses reflection only, so this test file does not need
     * qml4j on the compile classpath; it skips itself when qml4j is absent.
     */
    static void verifyThemeBridgeContract() {
        Object bridge;
        try {
            Class<?> type = Class.forName("selkie.theme.ThemeBridge");
            bridge = type.getDeclaredConstructor().newInstance();
        } catch (Throwable unavailable) {
            System.out.println("  (skipped ThemeBridge contract: qml4j runtime not on classpath - "
                    + unavailable.getClass().getSimpleName() + ")");
            return;
        }
        verifyBridgeFields(bridge, CONTRACT_SHAPE);
        verifyBridgeFields(bridge, CONTRACT_SPACE);
        verifyBridgeFields(bridge, CONTRACT_TYPE);
        System.out.println("  ThemeBridge: 7 shape + 6 space + 15 type properties match the contract");
    }

    private static void verifyBridgeFields(Object bridge, Map<String, Integer> expected) {
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            java.lang.reflect.Field field;
            try {
                field = bridge.getClass().getField(entry.getKey());
            } catch (NoSuchFieldException missing) {
                throw new AssertionError("ThemeBridge is missing public field " + entry.getKey());
            }
            Object property;
            try {
                property = field.get(bridge);
            } catch (IllegalAccessException e) {
                throw new AssertionError("ThemeBridge." + entry.getKey() + " not accessible: " + e);
            }
            check(property != null, "null property " + entry.getKey());
            double actual = ((Number) readPropertyValue(property)).doubleValue();
            checkApprox(entry.getValue(), actual, 1e-9,
                    "ThemeBridge." + entry.getKey() + " must be " + entry.getValue());
        }
    }

    private static Object readPropertyValue(Object property) {
        for (String method : new String[] {"peek", "get"}) {
            try {
                return property.getClass().getMethod(method).invoke(property);
            } catch (NoSuchMethodException ignored) {
                // try the next accessor
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("cannot read property value: " + e);
            }
        }
        throw new AssertionError("property object has neither peek() nor get()");
    }

    private static java.nio.file.Path locateThemeQml() {
        java.nio.file.Path direct = java.nio.file.Path.of("qml", "Theme.qml").toAbsolutePath();
        if (java.nio.file.Files.isRegularFile(direct)) {
            return direct;
        }
        java.nio.file.Path fromRoot = java.nio.file.Path.of("..", "qml", "Theme.qml").toAbsolutePath();
        return java.nio.file.Files.isRegularFile(fromRoot) ? fromRoot : null;
    }

    private static void contains(String haystack, String needle, String label) {
        check(haystack.contains(needle), "Theme.qml is missing " + label + " (expected: " + needle + ")");
    }

    static void verifyMotionTokens() {
        Map<String, MotionTokens.SpringSpec> tokens = MotionTokens.tokens();
        checkEquals(6, tokens.size(), "MD3E motion scheme has exactly six springs");
        for (String name : new String[] {"fastEffectsSpec", "defaultEffectsSpec", "slowEffectsSpec",
                "fastSpatialSpec", "defaultSpatialSpec", "slowSpatialSpec"}) {
            check(tokens.containsKey(name), "missing motion token: " + name);
        }

        // Values from the MD3E spec: spring(dampingRatio, stiffness), mass = 1.
        assertSpring(tokens.get("fastEffectsSpec"), 1.0, 3800.0, "fastEffectsSpec");
        assertSpring(tokens.get("defaultEffectsSpec"), 1.0, 1600.0, "defaultEffectsSpec");
        assertSpring(tokens.get("slowEffectsSpec"), 1.0, 800.0, "slowEffectsSpec");
        assertSpring(tokens.get("fastSpatialSpec"), 0.6, 1400.0, "fastSpatialSpec");
        assertSpring(tokens.get("defaultSpatialSpec"), 0.6, 700.0, "defaultSpatialSpec");
        assertSpring(tokens.get("slowSpatialSpec"), 0.6, 300.0, "slowSpatialSpec");

        // Speed tiers: fast > default > slow within each family.
        check(tokens.get("fastSpatialSpec").stiffness() > tokens.get("defaultSpatialSpec").stiffness(),
                "spatial fast must be stiffer than default");
        check(tokens.get("defaultSpatialSpec").stiffness() > tokens.get("slowSpatialSpec").stiffness(),
                "spatial default must be stiffer than slow");
        check(tokens.get("fastEffectsSpec").stiffness() > tokens.get("defaultEffectsSpec").stiffness(),
                "effects fast must be stiffer than default");
        check(tokens.get("defaultEffectsSpec").stiffness() > tokens.get("slowEffectsSpec").stiffness(),
                "effects default must be stiffer than slow");

        // Effects are critically damped; spatial specs bounce.
        check(!tokens.get("fastEffectsSpec").overshoots(), "effects must not overshoot");
        check(tokens.get("fastSpatialSpec").overshoots(), "spatial specs are meant to overshoot");

        // Raw damping coefficient derivation: c = 2 * zeta * sqrt(k * m).
        MotionTokens.SpringSpec spec = tokens.get("fastSpatialSpec");
        checkApprox(2.0 * 0.6 * Math.sqrt(1400.0), spec.damping(), 1e-9, "derived damping coefficient");
        checkApprox(1.0, spec.mass(), 1e-12, "MD3E springs have mass 1");

        // Settle times grow with the slow tier and stay in the MD3E ballpark.
        double fastSettle = tokens.get("fastEffectsSpec").settleDurationMs();
        double slowSettle = tokens.get("slowEffectsSpec").settleDurationMs();
        check(fastSettle < slowSettle, "fast effects must settle before slow effects");
        check(fastSettle > 0.0 && fastSettle < 1000.0, "fast effects settle time sensible: " + fastSettle);
        check(slowSettle < 3000.0, "slow effects settle time sensible: " + slowSettle);
        System.out.printf("  motion: fastEffects settle %.0f ms, defaultEffects %.0f ms, slowEffects %.0f ms%n",
                fastSettle, tokens.get("defaultEffectsSpec").settleDurationMs(), slowSettle);
        System.out.printf("  motion: fastSpatial %.0f ms, defaultSpatial %.0f ms, slowSpatial %.0f ms%n",
                tokens.get("fastSpatialSpec").settleDurationMs(),
                tokens.get("defaultSpatialSpec").settleDurationMs(),
                tokens.get("slowSpatialSpec").settleDurationMs());

        // Standard preset: same speeds, no overshoot anywhere.
        for (Map.Entry<String, MotionTokens.SpringSpec> entry : MotionTokens.standard().entrySet()) {
            check(!entry.getValue().overshoots(),
                    "standard motion scheme must never overshoot: " + entry.getKey());
        }
        check(MotionTokens.expressive().get("slowSpatialSpec").overshoots(),
                "expressive scheme bounces on slow spatial");
        check(MotionTokens.effectsSpecsNeverOvershoot(), "effects specs must never overshoot");
        check(MotionTokens.DampingRatio.NO_BOUNCY == 1.0, "NoBouncy");
        check(MotionTokens.DampingRatio.LOW_BOUNCY == 0.75, "LowBouncy");
        check(MotionTokens.DampingRatio.MEDIUM_BOUNCY == 0.5, "MediumBouncy");
        check(MotionTokens.DampingRatio.HIGH_BOUNCY == 0.2, "HighBouncy");
    }

    static void verifyEffectsNeverOvershoot() {
        for (Map.Entry<String, MotionTokens.SpringSpec> entry : MotionTokens.tokens().entrySet()) {
            String name = entry.getKey();
            MotionTokens.SpringSpec spec = entry.getValue();
            if (MotionTokens.isEffectsToken(name)) {
                check(spec.dampingRatio() >= 1.0, "effects token must be critically damped: " + name);
                double max = 0.0;
                for (int ms = 1; ms <= 3000; ms++) {
                    max = Math.max(max, spec.valueAt(ms / 1000.0));
                }
                check(max <= 1.0 + 1e-9,
                        "effects token overshoots its target: " + name + " reached " + max);
            } else {
                double max = 0.0;
                for (int ms = 1; ms <= 3000; ms++) {
                    max = Math.max(max, spec.valueAt(ms / 1000.0));
                }
                check(max > 1.0, "spatial token should overshoot: " + name);
            }
        }
    }

    private static void assertSpring(MotionTokens.SpringSpec spec, double dampingRatio, double stiffness,
            String name) {
        check(spec != null, "missing " + name);
        checkApprox(dampingRatio, spec.dampingRatio(), 1e-12, name + " dampingRatio");
        checkApprox(stiffness, spec.stiffness(), 1e-12, name + " stiffness");
        checkApprox(1.0, spec.mass(), 1e-12, name + " mass");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void checkNotEquals(int unexpected, int actual, String message) {
        if (unexpected == actual) {
            throw new AssertionError(message + ": both were " + actual);
        }
    }

    private static void checkMapEquals(Map<String, Integer> expected, Map<String, Integer> actual,
            String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }

    private static void checkApprox(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(message + ": expected " + expected + " +/- " + delta
                    + " but was " + actual);
        }
    }

    // ==================================================================
    // JDK-only self-check: java selkie.theme.ThemeTest
    // ==================================================================

    private interface Check {
        void run();
    }

    private static int run(String label, Check check) {
        try {
            check.run();
            System.out.println("[PASS] " + label);
            return 0;
        } catch (Throwable t) {
            System.out.println("[FAIL] " + label + " -> " + t.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        System.out.println("Selkie Hub - MD3E theme self-check");
        System.out.println("java.version = " + System.getProperty("java.version")
                + ", java.vendor = " + System.getProperty("java.vendor"));
        System.out.println();

        List<String> labels = new ArrayList<>();
        List<Check> checks = new ArrayList<>();
        labels.add("48 role inventory");
        checks.add(ThemeTest::verifyRoleCount);
        labels.add("tone mapping matches MD3E spec");
        checks.add(ThemeTest::verifyToneMapping);
        labels.add("WCAG contrast");
        checks.add(ThemeTest::verifyContrast);
        labels.add("fixed roles identical in light/dark");
        checks.add(ThemeTest::verifyFixedRoles);
        labels.add("HCT math (CAM16 + L* tone)");
        checks.add(ThemeTest::verifyHct);
        labels.add("palettes derived from seed");
        checks.add(ThemeTest::verifyPalettes);
        labels.add("published M3 baseline palette");
        checks.add(ThemeTest::verifyPublishedBaseline);
        labels.add("shape tokens match contract");
        checks.add(ThemeTest::verifyShapeTokens);
        labels.add("spacing tokens match contract");
        checks.add(ThemeTest::verifySpacingTokens);
        labels.add("typography tokens match contract");
        checks.add(ThemeTest::verifyTypographyTokens);
        labels.add("Theme.qml delegates every token");
        checks.add(ThemeTest::verifyThemeQmlContract);
        labels.add("motion tokens (six springs)");
        checks.add(ThemeTest::verifyMotionTokens);
        checks.add(ThemeTest::verifyEffectsNeverOvershoot);
        labels.add("effects tokens never overshoot");

        int failures = 0;
        for (int i = 0; i < checks.size(); i++) {
            failures += run(labels.get(i), checks.get(i));
        }

        System.out.println();
        printPalette(SEEDS[0], false);
        printPalette(SEEDS[0], true);
        System.out.println();
        System.out.println(failures == 0
                ? "ALL CHECKS PASSED (" + checks.size() + " checks, seeds=" + SEEDS.length + " x light+dark)"
                : failures + " CHECK(S) FAILED");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void printPalette(int seed, boolean dark) {
        ColorScheme scheme = Md3eThemeGenerator.generate(seed, dark);
        System.out.println("=== palette: seed " + ColorScheme.hex(seed)
                + ", " + (dark ? "dark" : "light") + " (" + scheme.roles().size() + " roles) ===");
        for (Map.Entry<String, Integer> entry : scheme.roles().entrySet()) {
            Hct hct = Hct.fromInt(entry.getValue());
            System.out.printf("%-26s %s  tone %6.2f  chroma %6.2f  hue %6.2f%n",
                    entry.getKey(), ColorScheme.hex(entry.getValue()), hct.tone(), hct.chroma(), hct.hue());
        }
        System.out.println();
    }
}
