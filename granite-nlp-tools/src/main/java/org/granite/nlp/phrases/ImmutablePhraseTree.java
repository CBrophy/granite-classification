package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;

public class ImmutablePhraseTree extends PhraseTree {

    private final ImmutableMap<PhraseTreePath, PhraseTreePath> knownPaths;
    private final ImmutableMap<String, PhraseTreeNode> nodes;
    private final ImmutableMap<UUID, PhraseTreeNode> nodesById;


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
    }

    @Override
    @Deprecated
    public PhraseTreePath computeIfAbsent(String rawText) {
        throw new UnsupportedOperationException();
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
