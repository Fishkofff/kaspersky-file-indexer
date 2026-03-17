package indexer.core.watcher;

import indexer.core.api.Tokenizer;
import indexer.core.InvertedIndex;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher implements AutoCloseable {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final InvertedIndex index;
    private final Tokenizer tokenizer;
    private final ExecutorService executor;
    private volatile boolean running;

    public DirectoryWatcher(InvertedIndex index, Tokenizer tokenizer) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new ConcurrentHashMap<>();
        this.index = index;
        this.tokenizer = tokenizer;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.running = true;
    }

    public void watchDirectory(Path startDir) throws IOException {
        registerAll(startDir);

        Thread watchThread = new Thread(this::processEvents, "Watcher-Thread");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                processFileAsync(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void processEvents() {
        while (running) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        try {
                            registerAll(child);
                        } catch (IOException e) {
                            //игнор папок, к которым у нас нет прав доступа
                        }
                    } else {
                        processFileAsync(child);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        processFileAsync(child);
                    }
                } else if (kind == ENTRY_DELETE) {
                    index.removeFile(child);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void processFileAsync(Path file) {
        executor.submit(() -> {
            try {
                String content = Files.readString(file);
                Set<String> words = tokenizer.tokenize(content);
                index.updateFile(file, words);
            } catch (IOException e) {
                index.removeFile(file);
            }
        });
    }

    @Override
    public void close() throws Exception {
        running = false;
        executor.shutdownNow();
        watcher.close();
    }
}