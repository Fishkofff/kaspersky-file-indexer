package indexer.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndex {

    private final ConcurrentHashMap<String, Set<Path>> index = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, Set<String>> fileWords = new ConcurrentHashMap<>();

    public void updateFile(Path file, Set<String> newWords) {
        fileWords.compute(file, (f, oldWords) -> {

            Set<String> previousWords;
            if (oldWords == null) {
                previousWords = Collections.emptySet();
            } else {
                previousWords = oldWords;
            }

            for (String word : previousWords) {
                if (!newWords.contains(word)) {
                    index.computeIfPresent(word, (w, paths) -> {
                        paths.remove(file);

                        if (paths.isEmpty()) {
                            return null;
                        }
                        return paths;
                    });
                }
            }

            for (String word : newWords) {
                if (!previousWords.contains(word)) {
                    index.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(file);
                }
            }

            if (newWords.isEmpty()) {
                return null;
            }
            return new HashSet<>(newWords);
        });
    }

    public void removeFile(Path file) {
        updateFile(file, Collections.emptySet());
    }

    public Set<Path> search(String word) {
        Set<Path> paths = index.get(word.toLowerCase());

        if (paths == null) {
            return Collections.emptySet();
        }
        return Set.copyOf(paths);
    }
}