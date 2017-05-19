package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhraseTools {
  private static Splitter TEXT_SPLITTER = Splitter
      .on(CharMatcher.whitespace())
      .trimResults()
      .omitEmptyStrings();

  public static List<String> splitText(String trimmed, Set<String> wordFilter) {
    checkNotNull(trimmed, "trimmed");
    checkNotNull(wordFilter, "wordFilter");

    return TEXT_SPLITTER
        .splitToList(trimmed)
        .stream()
        .filter(word -> !wordFilter.contains(word))
        .collect(Collectors.toList());
  }
}
