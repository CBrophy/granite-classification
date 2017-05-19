package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.UUID;

public class ImmutablePhraseTree extends PhraseTree {

    private final ImmutableMap<PhraseTreePath, PhraseTreePath> knownPaths;
    private final ImmutableMap<String, PhraseTreeNode> nodes;
    private final ImmutableMap<UUID, PhraseTreeNode> nodesById;
    private final ImmutableMultimap<PhraseTreePath, PhraseTreePath> alternativePaths;


    public ImmutablePhraseTree(PhraseTree phraseTree) {
        super(
            checkNotNull(phraseTree, "phraseTree").getWordFilter(),
            phraseTree.getStaticPhrases(),
            phraseTree.getStemmingFunction(),
            phraseTree.getPhraseSplittingFunction(),
            phraseTree.getPhraseJoiningFunction()
        );
        this.knownPaths = ImmutableMap.copyOf(phraseTree.getKnownPaths());
        this.nodes = ImmutableMap.copyOf(phraseTree.getNodes());
        this.nodesById = ImmutableMap.copyOf(phraseTree.getNodesById());
        this.alternativePaths = ImmutableMultimap.copyOf(phraseTree.getAlternativePaths());
    }

    @Override
    @Deprecated
    public PhraseTreePath computeIfAbsent(String rawText) {
        throw new UnsupportedOperationException();
    }

    @Override
    Multimap<PhraseTreePath, PhraseTreePath> getAlternativePaths() {
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
    Map<PhraseTreePath, PhraseTreePath> getKnownPaths() {
        return knownPaths;
    }
}
