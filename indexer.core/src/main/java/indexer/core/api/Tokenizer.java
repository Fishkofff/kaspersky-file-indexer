package indexer.core.api;

import java.util.Set;

public interface Tokenizer {
    Set<String> tokenize(String text);
}
