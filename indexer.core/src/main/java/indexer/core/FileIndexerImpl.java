package indexer.core;

import indexer.core.api.FileIndexer;
import indexer.core.api.Tokenizer;
import indexer.core.watcher.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class FileIndexerImpl implements FileIndexer {

    private final InvertedIndex index;
    private final DirectoryWatcher watcher;

    public FileIndexerImpl(Tokenizer tokenizer) throws IOException {
        this.index = new InvertedIndex();
        this.watcher = new DirectoryWatcher(this.index, tokenizer);
    }

    @Override
    public void watchDirectory(Path directory) throws IOException {
        watcher.watchDirectory(directory);
    }

    @Override
    public Set<Path> search(String word) {
        return index.search(word);
    }

    @Override
    public void close() throws Exception {
        watcher.close();
    }
}