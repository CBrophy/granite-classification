package org.granite.nlp.phrases;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class HashPhraseTree extends PhraseTree {

  private final HashMap<Phrase, Phrase> knownPaths = new HashMap<>();
  private final HashMap<String, PhraseTreeNode> nodes = new HashMap<>();
  private final HashMap<UUID, PhraseTreeNode> nodesById = new HashMap<>();
  private final HashMultimap<Phrase, Phrase> alternativePaths = HashMultimap
      .create();

  public HashPhraseTree(
      final ImmutableSet<String> wordFilter,
      final ImmutableSet<String> staticPhrases,
      final Function<List<String>, List<String>> stemmingFunction) {
    super(
        wordFilter,
        staticPhrases,
        stemmingFunction,
        phrase -> DEFAULT_SPLITTER.splitToList(phrase),
        words -> DEFAULT_JOINER.join(words)
    );
  }

  public HashPhraseTree(
      final ImmutableSet<String> wordFilter,
      final ImmutableSet<String> staticPhrases,
      final Function<List<String>, List<String>> stemmingFunction,
      final Function<String, List<String>> phraseSplittingFunction,
      final Function<List<String>, String> phraseJoiningFunction) {
    super(
        wordFilter,
        staticPhrases,
        stemmingFunction,
        phraseSplittingFunction,
        phraseJoiningFunction
    );
  }

  @Override
  Multimap<Phrase, Phrase> getAlternativePaths() {
    return alternativePaths;
  }

  @Override
  Map<String, PhraseTreeNode> getNodes() {
    return nodes;
  }

  @Override
  Map<UUID, PhraseTreeNode> getNodesById() {
    return nodesById;
  }

  @Override
  Map<Phrase, Phrase> getKnownPaths() {
    return knownPaths;
  }


}
