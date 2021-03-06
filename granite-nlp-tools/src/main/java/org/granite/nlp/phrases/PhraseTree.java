package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class PhraseTree implements Serializable {

  public static Splitter DEFAULT_SPLITTER = Splitter
      .on(CharMatcher.whitespace())
      .trimResults()
      .omitEmptyStrings();
  public static Joiner DEFAULT_JOINER = Joiner
      .on(' ')
      .skipNulls();

  private final SortedPhraseMaker sortedPhraseMaker;
  private final OrderPreservedPhraseMaker orderPreservedPhraseMaker;
  private final ImmutableSet<String> wordFilter;
  private final ImmutableSet<String> staticPhrases;
  private final Function<List<String>, List<String>> stemmingFunction;
  private final Function<List<String>, String> phraseJoiningFunction;
  private final Function<String, List<String>> phraseSplittingFunction;

  protected PhraseTree() {
    this(
        ImmutableSet.of(),
        ImmutableSet.of(),
        PhraseTree::getLowerCasedWords,
        phrase -> DEFAULT_SPLITTER.splitToList(phrase),
        words -> DEFAULT_JOINER.join(words)
    );
  }

  protected PhraseTree(
      final ImmutableSet<String> wordFilter,
      final ImmutableSet<String> staticPhrases,
      final Function<String, List<String>> phraseSplittingFunction,
      final Function<List<String>, String> phraseJoiningFunction
  ) {
    this(
        wordFilter,
        staticPhrases,
        PhraseTree::getLowerCasedWords,
        phraseSplittingFunction,
        phraseJoiningFunction);
  }

  protected PhraseTree(
      final ImmutableSet<String> wordFilter,
      final ImmutableSet<String> staticPhrases,
      final Function<List<String>, List<String>> stemmingFunction,
      final Function<String, List<String>> phraseSplittingFunction,
      final Function<List<String>, String> phraseJoiningFunction
  ) {

    this.wordFilter = checkNotNull(wordFilter, "wordFilter");
    this.staticPhrases = checkNotNull(staticPhrases, "staticPhrases");
    this.sortedPhraseMaker = new SortedPhraseMaker(
        wordFilter,
        staticPhrases,
        phraseSplittingFunction);
    this.orderPreservedPhraseMaker = new OrderPreservedPhraseMaker(
        wordFilter,
        staticPhrases,
        phraseSplittingFunction);
    this.stemmingFunction = checkNotNull(stemmingFunction, "stemmingFunction");
    this.phraseJoiningFunction = checkNotNull(phraseJoiningFunction, "phraseJoiningFunction");
    this.phraseSplittingFunction = checkNotNull(phraseSplittingFunction, "phraseSplittingFunction");

  }

  public static List<String> getLowerCasedWords(List<String> words) {
    return words
        .stream()
        .map(String::toLowerCase)
        .collect(Collectors.toList());
  }

  public Function<List<String>, String> getPhraseJoiningFunction() {
    return phraseJoiningFunction;
  }

  public Function<String, List<String>> getPhraseSplittingFunction() {
    return phraseSplittingFunction;
  }

  abstract Multimap<Phrase, Phrase> getAlternativePaths();

  abstract Map<String, PhraseTreeNode> getNodes();

  abstract Map<UUID, PhraseTreeNode> getNodesById();

  abstract Map<Phrase, Phrase> getKnownPaths();

  public Phrase get(final String rawText) {
    checkNotNull(rawText, "rawText");

    final String trimmed = rawText.trim();

    if (trimmed.isEmpty()) {
      return null;
    }

    final ImmutableList<String> sortedPhrase = sortedPhraseMaker.rawTextToPhrase(
        trimmed);

    if (sortedPhrase.isEmpty()) {
      // text contained only stop words, etc
      return null;
    }

    final List<String> stems = stemmingFunction.apply(sortedPhrase);

    checkState(stems.size() == sortedPhrase.size(),
        "Stemming function should return a stem list of the same size as the input");

    PhraseTreeNode result = null;

    final HashMap<String, UUID> nodeIds = new HashMap<>();

    for (int index = 0; index < sortedPhrase.size(); index++) {

      final String stemmedKey = stems.get(index);
      final String unstemmedKey = sortedPhrase.get(index);

      final PhraseTreeNode parent = result;

      result = getNodes().get(stemmedKey);

      // Phrase part doesn't exist, so no known path exists
      if (result == null) {
        return null;
      }

      nodeIds.put(unstemmedKey, result.getNodeId());

      if (parent != null) {
        result.getParentNodes().put(parent.getKey(), parent);
        parent.getChildNodes().put(result.getKey(), result);
      }

    }

    final List<UUID> path = orderPreservedPhraseMaker
        .rawTextToPhrase(trimmed)
        .stream()
        .map(nodeIds::get)
        .collect(Collectors.toList());

    checkState(path.size() == sortedPhrase.size(),
        "Path did not generate the correct number of nodes for: %s",
        rawText);

    final Phrase phrase = IdentityPhrase.of(path);

    return getKnownPaths().get(phrase);
  }

  public Phrase computeIfAbsent(final String rawText) {
    checkNotNull(rawText, "rawText");

    final String trimmed = rawText.trim();

    if (trimmed.isEmpty()) {
      return null;
    }

    final ImmutableList<String> sortedPhrase = sortedPhraseMaker.rawTextToPhrase(
        trimmed);

    if (sortedPhrase.isEmpty()) {
      // text contained only stop words, etc
      return null;
    }

    final List<String> stems = stemmingFunction.apply(sortedPhrase);

    checkState(stems.size() == sortedPhrase.size(),
        "Stemming function should return a stem list of the same size as the input");

    PhraseTreeNode result = null;

    final HashMap<String, UUID> nodeIds = new HashMap<>();

    for (int index = 0; index < sortedPhrase.size(); index++) {

      final String stemmedKey = stems.get(index);
      final String unstemmedKey = sortedPhrase.get(index);

      final PhraseTreeNode parent = result;

      result = getNodes().computeIfAbsent(stemmedKey,
          key -> new PhraseTreeNode(key, unstemmedKey));

      getNodesById().put(result.getNodeId(), result);

      nodeIds.put(unstemmedKey, result.getNodeId());

      if (parent != null) {
        result.getParentNodes().put(parent.getKey(), parent);
        parent.getChildNodes().put(result.getKey(), result);
      }

    }

    final List<UUID> path = orderPreservedPhraseMaker
        .rawTextToPhrase(trimmed)
        .stream()
        .map(nodeIds::get)
        .collect(Collectors.toList());

    checkState(path.size() == sortedPhrase.size(),
        "Path did not generate the correct number of nodes for: %s",
        rawText);

    final Phrase phrase = IdentityPhrase.of(path);

    final Phrase knownPath = getKnownPaths().computeIfAbsent(phrase, key -> key);

    if (knownPath.divergesFrom(phrase)) {
      getAlternativePaths().put(knownPath, phrase);
    }

    return knownPath;
  }

  public String getSynonym(final String rawPhrase) {
    checkNotNull(rawPhrase, "rawPhrase");

    final Phrase path = get(rawPhrase);

    if (path == null) {
      return rawPhrase;
    }

    return getPhraseText(path);
  }


  public List<String> getOrderPreservedParts(final String phrase) {
    checkNotNull(phrase, "phrase");

    final String trimmed = phrase.trim();

    if (trimmed.isEmpty()) {
      return ImmutableList.of();
    }

    return orderPreservedPhraseMaker
        .rawTextToPhrase(trimmed);
  }

  public List<String> getOrderPreservedParts(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    return phrase
        .getOrderedPath()
        .stream()
        .filter(id -> getNodesById().containsKey(id))
        .map(id -> getNodesById().get(id).getUnstemmedKey())
        .collect(Collectors.toList());
  }

  public String getPhraseText(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    final List<String> words = new ArrayList<>();

    for (UUID uuid : phrase
        .getOrderedPath()) {
      final PhraseTreeNode node = getNodesById().get(uuid);

      checkNotNull(node, "Unknown node id in path!");

      words.add(node.getUnstemmedKey());
    }

    return phraseJoiningFunction.apply(words);
  }

  public String getIdentityPhraseText(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    final List<String> words = new ArrayList<>();

    for (UUID uuid : phrase
        .getIdentitySet()) {
      final PhraseTreeNode node = getNodesById().get(uuid);

      checkNotNull(node, "Unknown node id in path!");

      words.add(node.getUnstemmedKey());
    }

    return phraseJoiningFunction.apply(words);
  }

  protected SortedPhraseMaker getSortedPhraseMaker() {
    return sortedPhraseMaker;
  }

  protected OrderPreservedPhraseMaker getOrderPreservedPhraseMaker() {
    return orderPreservedPhraseMaker;
  }

  protected Function<List<String>, List<String>> getStemmingFunction() {
    return stemmingFunction;
  }

  protected ImmutableSet<String> getWordFilter() {
    return wordFilter;
  }

  protected ImmutableSet<String> getStaticPhrases() {
    return staticPhrases;
  }

  public Map<Phrase, List<Phrase>> generateOrderedComponentMap(
      final int maxComponentLength) {

    final HashMap<Phrase, List<Phrase>> result = new HashMap<>();

    for (Phrase phrase : getKnownPaths()
        .keySet()) {

      final List<Phrase> componentPaths = OrderedPhrase
          .of(phrase)
          .componentize(maxComponentLength);

      componentPaths
          .forEach(path ->
              result
                  .computeIfAbsent(path, key -> new ArrayList<>())
                  .add(phrase)
          );
    }

    return result;
  }

  public Map<Phrase, List<Phrase>> generateIdentityComponentMap(
      final int maxComponentLength) {

    final HashMap<Phrase, List<Phrase>> result = new HashMap<>();

    for (Phrase phrase : getKnownPaths()
        .keySet()) {

      final List<Phrase> componentPaths = phrase
          .componentize(maxComponentLength);

      componentPaths
          .forEach(path ->
              result
                  .computeIfAbsent(path, key -> new ArrayList<>())
                  .add(phrase)
          );
    }

    return result;
  }
}
