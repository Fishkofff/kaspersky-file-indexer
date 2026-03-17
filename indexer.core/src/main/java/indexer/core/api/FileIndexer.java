package indexer.core.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface FileIndexer extends AutoCloseable {
    void watchDirectory(Path directory) throws IOException;

    Set<Path> search(String word);
}
