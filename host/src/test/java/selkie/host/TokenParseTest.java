package selkie.host;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * data/tokens.json parsing, the "missing fixture" degradation, the breakdown invariant and
 * the shipped sample itself.
 */
class TokenParseTest {

    @TempDir
    Path dataRoot;

    @Test
    void readsEveryBudgetFieldAsDouble() throws IOException {
        Fixtures.writeTokens(dataRoot, 118_400, 200_000, 24_600, 19_000, 41_200, 33_600);

        TokenBridge bridge = new TokenBridge(dataRoot, message -> { });

        assertEquals(118_400.0, bridge.used.peek().doubleValue());
        assertEquals(200_000.0, bridge.total.peek().doubleValue());
        assertEquals(24_600.0, bridge.persona.peek().doubleValue());
        assertEquals(19_000.0, bridge.recall.peek().doubleValue());
        assertEquals(41_200.0, bridge.tools.peek().doubleValue());
        assertEquals(33_600.0, bridge.history.peek().doubleValue());
        assertEquals(TokenBridge.SOURCE, bridge.source.peek());
        assertEquals(1L, bridge.revision.peek().longValue());
    }

    @Test
    void breakdownAddsUpToUsedInTheShippedSample() {
        Path data = Fixtures.projectData();
        assumeTrue(Files.isRegularFile(data.resolve(Fixtures.TOKENS)),
                "no data/tokens.json next to the build; skipping the sample check");

        List<String> log = new ArrayList<>();
        TokenBridge bridge = new TokenBridge(data, log::add);

        double parts = bridge.persona.peek() + bridge.recall.peek()
                + bridge.tools.peek() + bridge.history.peek();
        assertEquals(bridge.used.peek().doubleValue(), parts, 0.001,
                "persona + recall + tools + history must equal used");
        assertTrue(bridge.total.peek() > bridge.used.peek(), "total must exceed used");
        assertEquals(TokenBridge.SOURCE, bridge.source.peek());
        assertTrue(log.stream().noneMatch(line -> line.contains("cannot read")),
                () -> "shipped tokens.json could not be read: " + log);
        assertTrue(log.stream().noneMatch(line -> line.contains("inconsistent")),
                () -> "shipped tokens.json is inconsistent: " + log);
    }

    @Test
    void missingFilePublishesZeroesAndSaysSo() {
        List<String> log = new ArrayList<>();

        TokenBridge bridge = new TokenBridge(dataRoot, log::add);

        assertEquals(0.0, bridge.used.peek().doubleValue());
        assertEquals(0.0, bridge.total.peek().doubleValue());
        assertEquals(TokenBridge.UNAVAILABLE, bridge.source.peek());
        assertEquals(0L, bridge.revision.peek().longValue(), "a failed read is not a revision");
        assertEquals(1, log.size(), () -> "expected exactly one diagnostic line, got " + log);
        assertTrue(log.get(0).contains("tokens"), log.get(0));
    }

    @Test
    void malformedFileKeepsZeroesInsteadOfThrowing() throws IOException {
        Files.writeString(dataRoot.resolve(Fixtures.TOKENS), "not json at all",
                StandardCharsets.UTF_8);

        TokenBridge bridge = new TokenBridge(dataRoot, message -> { });

        assertEquals(TokenBridge.UNAVAILABLE, bridge.source.peek());
        assertEquals(0.0, bridge.used.peek().doubleValue());
    }

    @Test
    void inconsistentBreakdownIsReportedButStillPublished() throws IOException {
        List<String> log = new ArrayList<>();
        // used deliberately wrong: the parts add up to 60 000, not 70 000.
        Fixtures.writeTokens(dataRoot, 70_000, 200_000, 20_000, 15_000, 15_000, 10_000);

        TokenBridge bridge = new TokenBridge(dataRoot, log::add);

        assertEquals(70_000.0, bridge.used.peek().doubleValue());
        assertEquals(20_000.0, bridge.persona.peek().doubleValue());
        assertTrue(log.stream().anyMatch(line -> line.contains("inconsistent")),
                () -> "expected an inconsistency warning, got " + log);
    }

    @Test
    void missingFieldsReadAsZeroRatherThanFailing() throws IOException {
        Files.writeString(dataRoot.resolve(Fixtures.TOKENS),
                "{\"used\": 10, \"total\": 20}", StandardCharsets.UTF_8);

        TokenBridge bridge = new TokenBridge(dataRoot, message -> { });

        assertEquals(10.0, bridge.used.peek().doubleValue());
        assertEquals(20.0, bridge.total.peek().doubleValue());
        assertEquals(0.0, bridge.persona.peek().doubleValue());
        assertEquals(0.0, bridge.history.peek().doubleValue());
        assertEquals(TokenBridge.SOURCE, bridge.source.peek());
    }
}
