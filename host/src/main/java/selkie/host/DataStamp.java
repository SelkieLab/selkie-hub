package selkie.host;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A cheap fingerprint of the files a bridge reads, so {@code tick()} does a handful of
 * {@code stat} calls per frame instead of re-parsing JSON.
 *
 * <p>Fields, all of which must match for the fingerprint to be "unchanged":
 * <ul>
 *   <li>{@code files} - how many manifests exist (catches a plugin directory added/removed);</li>
 *   <li>{@code totalBytes} - summed size (catches most in-place edits);</li>
 *   <li>{@code newest} - newest last-modified time in ms (catches a rewrite of identical size);</li>
 *   <li>{@code pathHash} - hash of the entry names (catches rename/add/remove at equal count).</li>
 * </ul>
 *
 * <p>Deliberately <b>not</b> a content hash: reading every manifest on every frame would cost
 * more than the reload it is meant to detect.
 */
record DataStamp(long files, long totalBytes, long newest, int pathHash) {

    /** Nothing there (or the directory could not be listed). */
    static final DataStamp MISSING = new DataStamp(0L, 0L, 0L, 0);

    /** Sentinel used as "never read yet", so the first {@code tick()} always reads. */
    static final DataStamp NEVER = new DataStamp(-1L, -1L, -1L, -1);

    /** Fingerprint of one regular file (tokens.json). */
    static DataStamp ofFile(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return MISSING;
            }
            return new DataStamp(1L, Files.size(file), Files.getLastModifiedTime(file).toMillis(), file.hashCode());
        } catch (IOException | RuntimeException error) {
            return MISSING;
        }
    }

    /** Fingerprint of every {@code <data>/plugins/<id>/manifest.json} (one directory level). */
    static DataStamp ofPluginTree(Path pluginsDirectory) {
        if (!Files.isDirectory(pluginsDirectory)) {
            return MISSING;
        }
        long files = 0L;
        long bytes = 0L;
        long newest = 0L;
        int pathHash = 1;
        try (DirectoryStream<Path> children = Files.newDirectoryStream(pluginsDirectory)) {
            for (Path child : children) {
                Path manifest = child.resolve(ManifestReader.FILE_NAME);
                if (!Files.isRegularFile(manifest)) {
                    continue;
                }
                files++;
                bytes += Files.size(manifest);
                newest = Math.max(newest, Files.getLastModifiedTime(manifest).toMillis());
                pathHash = 31 * pathHash + child.getFileName().toString().hashCode();
            }
        } catch (IOException | RuntimeException error) {
            return MISSING;
        }
        return new DataStamp(files, bytes, newest, pathHash);
    }
}
