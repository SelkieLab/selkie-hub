package selkie.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MD3E spacing tokens in pixels: the 4dp layout grid steps used by Material components.
 *
 * <p>Provenance (be precise - MD3E does <b>not</b> publish a spacing token catalog the way it
 * publishes color/shape/typography tokens; spacing in Material is a <em>layout grid</em>, not a
 * token set):
 * <ul>
 *   <li>Material layout is built on a 4dp baseline grid with 8dp increments
 *       (m3.material.io, "Understanding layout / Applying layout"), which is why every value here
 *       is a multiple of 4.</li>
 *   <li>16dp is the standard component content padding: official Compose samples in
 *       {@code references/compose-api-full.md} use {@code contentPadding = PaddingValues(16.dp)}
 *       and {@code Modifier.padding(16.dp)} for dialogs/pickers; 24dp is the large container /
 *       FAB padding, 8dp the tight gap used inside chips and between list-item icon and text.</li>
 * </ul>
 *
 * <p>The names are fixed by the Stage-2 shared contract ({@code spaceXs}..{@code spaceXxl}) and
 * must not be renamed.
 */
public final class SpacingTokens {

    /** The 4dp baseline grid step every spacing token is a multiple of. */
    public static final int GRID = 4;

    /** 4dp - icon/label micro gaps, dense chip internals. */
    public static final int XS = 4;
    /** 8dp - tight gap between related elements, list item icon to text. */
    public static final int S = 8;
    /** 12dp - compact padding inside small containers. */
    public static final int M = 12;
    /** 16dp - the standard component content padding. */
    public static final int L = 16;
    /** 24dp - padding of large containers, FAB and dialog insets. */
    public static final int XL = 24;
    /** 32dp - screen-level section gaps and page margins. */
    public static final int XXL = 32;

    /** Number of spacing tokens in the frozen QML contract. */
    public static final int CONTRACT_COUNT = 6;

    private static final Map<String, Integer> TOKENS;

    static {
        Map<String, Integer> tokens = new LinkedHashMap<>();
        tokens.put("spaceXs", XS);
        tokens.put("spaceS", S);
        tokens.put("spaceM", M);
        tokens.put("spaceL", L);
        tokens.put("spaceXl", XL);
        tokens.put("spaceXxl", XXL);
        TOKENS = Collections.unmodifiableMap(tokens);
    }

    private SpacingTokens() {
    }

    /** The frozen QML contract: token name -&gt; px, in ascending order. */
    public static Map<String, Integer> tokens() {
        return TOKENS;
    }

    /** Spacing in px for a contract token name, e.g. {@code "spaceM"}. */
    public static int px(String tokenName) {
        Integer value = TOKENS.get(tokenName);
        if (value == null) {
            throw new IllegalArgumentException("Unknown spacing token: " + tokenName);
        }
        return value;
    }
}
