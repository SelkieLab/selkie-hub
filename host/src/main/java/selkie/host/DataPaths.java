package selkie.host;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the host finds its runtime data ({@code data/plugins/**}, {@code data/tokens.json}).
 *
 * <p>Resolution order (first hit wins):
 * <ol>
 *   <li>{@code --data-root=<path>} on the command line;</li>
 *   <li>the {@code selkie.data} system property;</li>
 *   <li>the {@code SELKIE_DATA} environment variable;</li>
 *   <li>{@code ./data} (the normal case: {@code gradlew run} starts in the project root);</li>
 *   <li>the first {@code data} directory found walking up from the working directory, so a
 *       launcher started from {@code build/install/selkie-hub/bin} still sees the repo data.</li>
 * </ol>
 *
 * <p>A missing directory is not an error: the bridges publish an empty plugin list and a
 * zeroed token budget, and the window still opens.
 */
public final class DataPaths {

    /** Name of the data directory, relative to the project root. */
    public static final String DEFAULT_DIRECTORY = "data";

    private DataPaths() {
    }

    /** Resolves the data directory for this process (never null, may not exist). */
    public static Path resolve(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        String property = System.getProperty("selkie.data");
        if (property != null && !property.isBlank()) {
            return Path.of(property).toAbsolutePath().normalize();
        }
        String environment = System.getenv("SELKIE_DATA");
        if (environment != null && !environment.isBlank()) {
            return Path.of(environment).toAbsolutePath().normalize();
        }
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve(DEFAULT_DIRECTORY);
        if (Files.isDirectory(direct)) {
            return direct;
        }
        for (Path parent = workingDirectory.getParent(); parent != null; parent = parent.getParent()) {
            Path candidate = parent.resolve(DEFAULT_DIRECTORY);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return direct;
    }
}
