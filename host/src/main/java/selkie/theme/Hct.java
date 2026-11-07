package selkie.theme;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HCT color space: <b>H</b>ue and <b>C</b>hroma from CAM16, <b>T</b>one from CIE L*.
 *
 * <p>This is a pure-JDK implementation of the algorithm used by Google's
 * {@code material-color-utilities} (Apache-2.0): a color is converted from sRGB to
 * XYZ (D65), run through the CAM16 color appearance model to obtain hue and chroma,
 * and the tone is the CIE L* of its luminance Y. Tone is deliberately <em>not</em>
 * perceptual lightness from CAM16 (J) nor HSL/HSV value/saturation: L* is the
 * quantity Material tonal palettes are defined in, and it is what makes
 * "tone 40 always contrasts the same way with tone 100" true regardless of hue.
 *
 * <p>Forward direction: {@link #fromInt(int)}. Inverse direction: {@link #argbFrom(double, double, double)}.
 * The inverse solves for the CAM16 lightness J that produces the requested tone, then
 * reduces chroma (hue and tone preserved) when the request falls outside the sRGB
 * gamut, which is how tonal palettes avoid per-role hex constants.
 */
public record Hct(double hue, double chroma, double tone) {

    /** Standard sRGB viewing conditions (the material-color-utilities default). */
    static final ViewingConditions VIEWING_CONDITIONS = ViewingConditions.makeDefaultSrgb();

    /**
     * Gamut tolerance in gamma-encoded sRGB channel units. Kept near-zero on purpose: a loose
     * tolerance lets a channel overshoot 1.0 by a few 8-bit steps and get clamped, which at
     * pale tones (tone 95+) invents a visible tint in the wrong hue direction.
     */
    private static final double GAMUT_TOLERANCE = 1.0e-7;
    private static final int J_ITERATIONS = 24;
    private static final int CHROMA_ITERATIONS = 14;

    public Hct {
        hue = sanitizeDegrees(hue);
        chroma = Math.max(0.0, chroma);
        tone = clamp(0.0, 100.0, tone);
    }

    /** Forward conversion: sRGB ARGB to HCT. */
    public static Hct fromInt(int argb) {
        double[] xyz = xyzFromArgb(argb);
        double[] hcj = Cam16.forward(xyz[0], xyz[1], xyz[2]);
        return new Hct(hcj[0], hcj[1], lstarFromY(xyz[1]));
    }

    /** Inverse conversion: this HCT back to opaque ARGB. */
    public int toInt() {
        return argbFrom(hue, chroma, tone);
    }

    /**
     * Inverse conversion, gamut mapped. The returned color has (very nearly) the requested
     * tone and hue; chroma is reduced only as far as the sRGB gamut requires.
     */
    public static int argbFrom(double hue, double chroma, double tone) {
        // Tone 0 / 100 have exactly one in-gamut answer; return it without gamut iteration.
        if (tone <= 0.0) {
            return 0xFF000000;
        }
        if (tone >= 100.0) {
            return 0xFFFFFFFF;
        }
        double yTarget = yFromLstar(tone);
        double requested = Math.max(0.0, chroma);
        double[] xyz = xyzAt(hue, requested, yTarget);
        if (!inGamut(xyz)) {
            double lo = 0.0;
            double hi = requested;
            for (int i = 0; i < CHROMA_ITERATIONS; i++) {
                double mid = (lo + hi) * 0.5;
                if (inGamut(xyzAt(hue, mid, yTarget))) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            xyz = xyzAt(hue, lo, yTarget);
        }
        return encode(xyz);
    }

    /** XYZ (Y on a 0..100 scale) for a CAM16 color whose luminance matches the target. */
    private static double[] xyzAt(double hue, double chroma, double yTarget) {
        double safeHue = sanitizeDegrees(hue);
        double safeChroma = Math.max(0.0, chroma);
        if (yTarget <= 0.0) {
            return Cam16.inverse(safeHue, safeChroma, 0.0);
        }
        double hi = 100.0;
        double[] hiXyz = Cam16.inverse(safeHue, safeChroma, hi);
        if (hiXyz[1] <= yTarget) {
            return hiXyz;
        }
        double lo = 0.0;
        for (int i = 0; i < J_ITERATIONS; i++) {
            double mid = (lo + hi) * 0.5;
            double[] midXyz = Cam16.inverse(safeHue, safeChroma, mid);
            if (midXyz[1] < yTarget) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return Cam16.inverse(safeHue, safeChroma, (lo + hi) * 0.5);
    }

    private static boolean inGamut(double[] xyz) {
        double[] rgb = gammaRgbFromXyz(xyz[0], xyz[1], xyz[2]);
        for (double channel : rgb) {
            if (channel < -GAMUT_TOLERANCE || channel > 1.0 + GAMUT_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static int encode(double[] xyz) {
        double[] rgb = gammaRgbFromXyz(xyz[0], xyz[1], xyz[2]);
        int r = encodeChannel(rgb[0]);
        int g = encodeChannel(rgb[1]);
        int b = encodeChannel(rgb[2]);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int encodeChannel(double gammaChannel) {
        double clamped = clamp(0.0, 1.0, gammaChannel);
        return (int) Math.round(clamped * 255.0);
    }

    // ------------------------------------------------------------------
    // sRGB <-> XYZ (D65)
    // ------------------------------------------------------------------

    /** sRGB ARGB to XYZ, Y on the usual 0..100 scale. */
    static double[] xyzFromArgb(int argb) {
        double r = linearizedChannel((argb >> 16) & 0xFF);
        double g = linearizedChannel((argb >> 8) & 0xFF);
        double b = linearizedChannel(argb & 0xFF);
        double x = 0.41233895 * r + 0.35762064 * g + 0.18051042 * b;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = 0.01932141 * r + 0.11916382 * g + 0.95034478 * b;
        return new double[] {x, y, z};
    }

    /** XYZ to gamma-encoded sRGB in 0..1 per channel. */
    static double[] gammaRgbFromXyz(double x, double y, double z) {
        double r = 3.2413774792388685 * x - 1.5376652402851851 * y - 0.49885366846268053 * z;
        double g = -0.9691452513005321 * x + 1.8758853451067872 * y + 0.04156585616912061 * z;
        double b = 0.05562093689691305 * x - 0.20395524564742123 * y + 1.0571799111220335 * z;
        return new double[] {gammaFromLinear(r), gammaFromLinear(g), gammaFromLinear(b)};
    }

    /** sRGB channel (0..255) to linear luminance contribution (0..100). */
    static double linearizedChannel(int channel) {
        double normalized = channel / 255.0;
        if (normalized <= 0.040449936) {
            return normalized / 12.92 * 100.0;
        }
        return Math.pow((normalized + 0.055) / 1.055, 2.4) * 100.0;
    }

    /** Linear luminance (0..100) to gamma-encoded sRGB channel (0..1). */
    static double gammaFromLinear(double linear) {
        double normalized = linear / 100.0;
        if (normalized <= 0.0031308) {
            return 12.92 * normalized;
        }
        return 1.055 * Math.pow(normalized, 1.0 / 2.4) - 0.055;
    }

    // ------------------------------------------------------------------
    // CIE L* (tone)
    // ------------------------------------------------------------------

    /** CIE L* for a luminance Y on a 0..100 scale. Tone in HCT/ATSC. */
    public static double lstarFromY(double y) {
        double normalized = y / 100.0;
        double e = 216.0 / 24389.0;
        double kappa = 24389.0 / 27.0;
        if (normalized <= e) {
            return normalized * kappa;
        }
        return Math.cbrt(normalized) * 116.0 - 16.0;
    }

    /** Luminance Y (0..100) for a CIE L* tone. */
    public static double yFromLstar(double lstar) {
        double e = 216.0 / 24389.0;
        double kappa = 24389.0 / 27.0;
        double ft = (lstar + 16.0) / 116.0;
        double cube = ft * ft * ft;
        if (cube > e) {
            return cube * 100.0;
        }
        return (116.0 * ft - 16.0) / kappa * 100.0;
    }

    // ------------------------------------------------------------------
    // small numeric helpers
    // ------------------------------------------------------------------

    static double clamp(double min, double max, double value) {
        return value < min ? min : (value > max ? max : value);
    }

    static double sanitizeDegrees(double degrees) {
        double m = degrees % 360.0;
        if (m < 0.0) {
            m += 360.0;
        }
        return m >= 360.0 ? 0.0 : m;
    }

    static double lerp(double start, double stop, double amount) {
        return (1.0 - amount) * start + amount * stop;
    }

    // ------------------------------------------------------------------
    // CAM16 viewing conditions
    // ------------------------------------------------------------------

    /** CAM16 viewing conditions; {@link #makeDefaultSrgb()} reproduces the sRGB display defaults. */
    static final class ViewingConditions {
        final double n;
        final double aw;
        final double nbb;
        final double ncb;
        final double c;
        final double nc;
        final double z;
        final double fl;
        final double[] rgbD;

        private ViewingConditions(double n, double aw, double nbb, double ncb, double c, double nc,
                double z, double fl, double[] rgbD) {
            this.n = n;
            this.aw = aw;
            this.nbb = nbb;
            this.ncb = ncb;
            this.c = c;
            this.nc = nc;
            this.z = z;
            this.fl = fl;
            this.rgbD = rgbD;
        }

        static ViewingConditions makeDefaultSrgb() {
            // D65 white point, average surround, background L* 50, standard sRGB display.
            double[] whitePoint = {95.047, 100.0, 108.883};
            double adaptingLuminance = 200.0 / Math.PI * yFromLstar(50.0) / 100.0;
            double surround = 2.0;
            boolean discountingIlluminant = false;

            double rW = whitePoint[0];
            double gW = whitePoint[1];
            double bW = whitePoint[2];

            double f = 0.8 + surround / 10.0;
            double c = f >= 0.9
                    ? lerp(0.59, 0.69, (f - 0.9) * 10.0)
                    : lerp(0.525, 0.59, (f - 0.8) * 10.0);
            double d = discountingIlluminant
                    ? 1.0
                    : f * (1.0 - (1.0 / 3.6) * Math.exp((-adaptingLuminance - 42.0) / 92.0));
            d = clamp(0.0, 1.0, d);

            double nc = f;
            // Degree of adaptation per cone channel: D_i = D * (Y_w / R_w) + 1 - D, where R_w is the
            // white point's *cone response* (CAM16 RGB), not its XYZ. Using the cone response keeps
            // the achromatic axis exactly neutral after adaptation.
            double[] camWhite = Cam16.toCam16Rgb(rW, gW, bW);
            double[] rgbD = {
                d * (100.0 / camWhite[0]) + 1.0 - d,
                d * (100.0 / camWhite[1]) + 1.0 - d,
                d * (100.0 / camWhite[2]) + 1.0 - d
            };

            double k = 1.0 / (5.0 * adaptingLuminance + 1.0);
            double k4 = k * k * k * k;
            double k4F = 1.0 - k4;
            double fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * Math.cbrt(5.0 * adaptingLuminance);

            double n = yFromLstar(50.0) / whitePoint[1];
            double z = 1.48 + Math.sqrt(n);
            double nbb = 0.725 / Math.pow(n, 0.2);
            double ncb = nbb;

            double[] rgbAFactors = new double[] {
                Math.pow(fl * rgbD[0] * camWhite[0] / 100.0, 0.42),
                Math.pow(fl * rgbD[1] * camWhite[1] / 100.0, 0.42),
                Math.pow(fl * rgbD[2] * camWhite[2] / 100.0, 0.42)
            };
            double[] rgbA = new double[3];
            for (int i = 0; i < 3; i++) {
                rgbA[i] = 400.0 * rgbAFactors[i] / (rgbAFactors[i] + 27.13);
            }
            double aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb;

            return new ViewingConditions(n, aw, nbb, ncb, c, nc, z, fl, rgbD);
        }
    }

    // ------------------------------------------------------------------
    // CAM16 appearance model
    // ------------------------------------------------------------------

    /** CAM16 forward and inverse mappings, in sRGB viewing conditions. */
    static final class Cam16 {

        private Cam16() {
        }

        /** CAT16 XYZ to "cone-like" RGB. */
        static double[] toCam16Rgb(double x, double y, double z) {
            return new double[] {
                0.401288 * x + 0.650173 * y - 0.051461 * z,
                -0.250268 * x + 1.204414 * y + 0.045854 * z,
                -0.002079 * x + 0.048952 * y + 0.953127 * z
            };
        }

        /** Inverse of {@link #toCam16Rgb(double, double, double)}. */
        static double[] fromCam16Rgb(double r, double g, double b) {
            return new double[] {
                1.86206786 * r - 1.01125463 * g + 0.14918677 * b,
                0.38752654 * r + 0.62144744 * g - 0.00897398 * b,
                -0.01584150 * r - 0.03412294 * g + 1.0499644 * b
            };
        }

        /**
         * XYZ to CAM16 correlates.
         * @return {hue in degrees, chroma C, lightness J}
         */
        static double[] forward(double x, double y, double z) {
            ViewingConditions vc = VIEWING_CONDITIONS;
            double[] rgb = toCam16Rgb(x, y, z);

            double[] adapted = new double[3];
            double[] compressed = new double[3];
            for (int i = 0; i < 3; i++) {
                adapted[i] = vc.rgbD[i] * rgb[i];
                double f = Math.pow(vc.fl * Math.abs(adapted[i]) / 100.0, 0.42);
                compressed[i] = Math.signum(adapted[i]) * 400.0 * f / (f + 27.13);
            }

            double rA = compressed[0];
            double gA = compressed[1];
            double bA = compressed[2];

            double a = (11.0 * rA - 12.0 * gA + bA) / 11.0;
            double b = (rA + gA - 2.0 * bA) / 9.0;
            double u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0;
            double p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0;

            double atanDegrees = Math.toDegrees(Math.atan2(b, a));
            double hue = atanDegrees < 0 ? atanDegrees + 360.0 : (atanDegrees >= 360.0 ? atanDegrees - 360.0 : atanDegrees);

            double ac = p2 * vc.nbb;
            double j = 100.0 * Math.pow(ac / vc.aw, vc.c * vc.z);

            double huePrime = hue < 20.14 ? hue + 360.0 : hue;
            double eHue = 0.25 * (Math.cos(Math.toRadians(huePrime) + 2.0) + 3.8);
            double p1 = 50000.0 / 13.0 * eHue * vc.nc * vc.ncb;
            double t = p1 * Math.hypot(a, b) / (u + 0.305);
            double alpha = Math.pow(1.64 - Math.pow(0.29, vc.n), 0.73) * Math.pow(t, 0.9);
            double chroma = alpha * Math.sqrt(j / 100.0);

            return new double[] {hue, chroma, j};
        }

        /**
         * Inverse mapping: (hue, chroma, lightness J) to XYZ.
         * Chroma that cannot be represented is not clipped here - callers gamut-map.
         */
        static double[] inverse(double hue, double chroma, double j) {
            ViewingConditions vc = VIEWING_CONDITIONS;
            double safeJ = clamp(0.0, 100.0, j);
            double safeChroma = Math.max(0.0, chroma);

            double hRad = Math.toRadians(hue);
            double sinH = Math.sin(hRad);
            double cosH = Math.cos(hRad);

            // Chroma C = t^0.9 * sqrt(J/100) * (1.64 - 0.29^n)^0.73, so
            // t = (C / (sqrt(J/100) * (1.64 - 0.29^n)^0.73))^(1/0.9).
            // Note: C here is CAM16 *chroma*, not colorfulness M = C * F_L^0.25.
            double alpha = safeChroma == 0.0 ? 0.0 : safeChroma / Math.sqrt(safeJ / 100.0);
            double t = Math.pow(alpha / Math.pow(1.64 - Math.pow(0.29, vc.n), 0.73), 1.0 / 0.9);
            double eHue = 0.25 * (Math.cos(hRad + 2.0) + 3.8);
            double ac = vc.aw * Math.pow(safeJ / 100.0, 1.0 / vc.c / vc.z);
            double p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb;
            double p2 = ac / vc.nbb;

            double gamma = 23.0 * (p2 + 0.305) * t
                    / (23.0 * p1 + 11.0 * t * cosH + 108.0 * t * sinH);
            double a = gamma * cosH;
            double b = gamma * sinH;

            double rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0;
            double gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0;
            double bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0;

            double rC = inverseCompress(rA) / vc.rgbD[0];
            double gC = inverseCompress(gA) / vc.rgbD[1];
            double bC = inverseCompress(bA) / vc.rgbD[2];

            return fromCam16Rgb(rC, gC, bC);
        }

        private static double inverseCompress(double compressed) {
            double abs = Math.min(399.999999, Math.abs(compressed));
            double base = 27.13 * abs / (400.0 - abs);
            return Math.signum(compressed) * (100.0 / VIEWING_CONDITIONS.fl) * Math.pow(base, 1.0 / 0.42);
        }
    }

    // ------------------------------------------------------------------
    // Tonal palettes
    // ------------------------------------------------------------------

    /**
     * A Material tonal palette: a fixed CAM16 hue and chroma, sampled at CIE L* tones.
     * Every MD3E color role is a tone from one of the six palettes derived from the seed,
     * which is why no role needs a literal color: only (hue, chroma, tone) triples.
     */
    public static final class TonalPalette {
        private final double hue;
        private final double chroma;
        private final Map<Integer, Integer> cache = new ConcurrentHashMap<>();

        private TonalPalette(double hue, double chroma) {
            this.hue = sanitizeDegrees(hue);
            this.chroma = Math.max(0.0, chroma);
        }

        public static TonalPalette fromHueAndChroma(double hue, double chroma) {
            return new TonalPalette(hue, chroma);
        }

        public static TonalPalette fromInt(int argb) {
            Hct hct = Hct.fromInt(argb);
            return new TonalPalette(hct.hue(), hct.chroma());
        }

        public double hue() {
            return hue;
        }

        public double chroma() {
            return chroma;
        }

        /** ARGB at the requested tone (0..100), gamut mapped with hue and tone preserved. */
        public int tone(int requestedTone) {
            int tone = (int) Math.round(clamp(0.0, 100.0, requestedTone));
            return cache.computeIfAbsent(tone, key -> Hct.argbFrom(hue, chroma, key));
        }

        /** The HCT actually produced at a tone (after gamut mapping and 8-bit quantization). */
        public Hct hctAt(int requestedTone) {
            return Hct.fromInt(tone(requestedTone));
        }

        @Override
        public String toString() {
            return "TonalPalette[hue=" + String.format("%.2f", hue)
                    + ", chroma=" + String.format("%.2f", chroma) + "]";
        }
    }
}
