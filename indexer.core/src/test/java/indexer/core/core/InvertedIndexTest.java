package indexer.core.core;

import indexer.core.InvertedIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvertedIndexTest {

    private InvertedIndex index;

    @BeforeEach
    void setUp() {
        index = new InvertedIndex();
    }

    @Test
    void shouldAddWordsAndFindThem() {
        Path file1 = Path.of("file1.txt");
        index.updateFile(file1, Set.of("apple", "banana"));

        Set<Path> results = index.search("apple");

        assertEquals(1, results.size());
        assertTrue(results.contains(file1));
    }

    @Test
    void shouldHandleMultipleFiles() {
        Path file1 = Path.of("file1.txt");
        Path file2 = Path.of("file2.txt");

        index.updateFile(file1, Set.of("java", "spring"));
        index.updateFile(file2, Set.of("java", "kotlin"));

        Set<Path> javaResults = index.search("java");
        assertEquals(2, javaResults.size());
        assertTrue(javaResults.containsAll(Set.of(file1, file2)));

        Set<Path> springResults = index.search("spring");
        assertEquals(1, springResults.size());
        assertTrue(springResults.contains(file1));
    }

    @Test
    void shouldRemoveWordsWhenFileUpdated() {
        Path file = Path.of("doc.txt");

        index.updateFile(file, Set.of("hello", "world"));
        assertTrue(index.search("hello").contains(file));

        index.updateFile(file, Set.of("world", "java"));

        assertFalse(index.search("hello").contains(file));
        assertTrue(index.search("java").contains(file));
    }

    @Test
    void shouldRemoveFileEntirely() {
        Path file = Path.of("delete_me.txt");
        index.updateFile(file, Set.of("secret"));

        index.removeFile(file);

        assertTrue(index.search("secret").isEmpty());
    }
}