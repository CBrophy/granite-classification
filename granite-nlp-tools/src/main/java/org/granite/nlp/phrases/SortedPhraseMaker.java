package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SortedPhraseMaker implements PhraseMaker {

  private final Set<String> wordFilter;
  private final ImmutableMap<String, String> staticPhrases;
  private final Function<String, List<String>> phraseSplittingFunction;

  public SortedPhraseMaker(
      final Set<String> wordFilter,
      final Set<String> staticPhrases,
      final Function<String, List<String>> phraseSplittingFunction) {

    this.wordFilter = checkNotNull(wordFilter, "wordFilter");
    this.phraseSplittingFunction = checkNotNull(phraseSplittingFunction, "phraseSplittingFunction");

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

    final List<String> phraseParts = phraseSplittingFunction
        .apply(trimmed)
        .stream()
        .filter(word -> !wordFilter.contains(word))
        .collect(Collectors.toList());

    final HashSet<String> unique = new HashSet<>();

    final ImmutableList.Builder<String> builder = ImmutableList.builder();

    phraseParts
        .stream()
        .sorted()
        .filter(word -> !unique.contains(word.toLowerCase()))
        .forEach(word -> {
          builder.add(word);
          unique.add(word.toLowerCase());
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
