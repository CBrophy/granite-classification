package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderPreservedPhraseMaker implements PhraseMaker {

    private final Set<String> wordFilter;
    private final ImmutableMap<String, String> staticPhrases;


    public OrderPreservedPhraseMaker(final Set<String> wordFilter,
        final Set<String> staticPhrases) {
        this.wordFilter = checkNotNull(wordFilter, "wordFilter");

        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        checkNotNull(staticPhrases, "staticPhrases")
            .forEach(phrase -> builder.put(phrase.toLowerCase(), phrase));

        this.staticPhrases = builder.build();
    }

    @Override
    public ImmutableList<String> rawTextToPhrase(String rawText) {
        checkNotNull(rawText, "rawText");

        final String trimmed = rawText.trim();

        if (trimmed.isEmpty()) {
            return ImmutableList.of();
        }

        final String staticPhrase = staticPhrases.get(trimmed.toLowerCase());

        if (staticPhrase != null) {
            return ImmutableList.of(staticPhrase);
        }

        final List<String> phraseParts = PhraseTools.splitText(trimmed, wordFilter);

        final HashSet<String> unique = new HashSet<>();

        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        phraseParts
            .stream()
            .filter(word -> !unique.contains(word.toLowerCase()))
            .forEach(word -> {
                builder.add(word);
                unique.add(word);
            });

        return builder.build();

    }

    @Override
    public String rawTextToCorrectedPhrase(String rawText) {
        StringBuilder builder = new StringBuilder();

        for (String word : rawTextToPhrase(rawText)) {
            builder = builder.append(word);
        }

        return builder.toString();
    }
}
