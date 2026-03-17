package indexer.core;

import indexer.core.api.Tokenizer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenizerImpl implements Tokenizer {
    @Override
    public Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text.split("[\\p{Punct}\\s]+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());
    }
}
