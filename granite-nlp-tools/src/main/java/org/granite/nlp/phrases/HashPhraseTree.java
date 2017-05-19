package org.granite.nlp.phrases;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class HashPhraseTree extends PhraseTree {

    private final HashMap<PhraseTreePath, PhraseTreePath> knownPaths = new HashMap<>();
    private final HashMap<String, PhraseTreeNode> nodes = new HashMap<>();
    private final HashMap<UUID, PhraseTreeNode> nodesById = new HashMap<>();

    public HashPhraseTree(ImmutableSet<String> wordFilter,
        ImmutableSet<String> staticPhrases,
        Function<List<String>, List<String>> stemmingFunction) {
        super(wordFilter, staticPhrases, stemmingFunction);
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
    Map<PhraseTreePath, PhraseTreePath> getKnownPaths() {
        return knownPaths;
    }
}
