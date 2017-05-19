package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class PhraseTree {

    private final SortedPhraseMaker sortedPhraseMaker;
    private final OrderPreservedPhraseMaker orderPreservedPhraseMaker;
    private final ImmutableSet<String> wordFilter;
    private final ImmutableSet<String> staticPhrases;
    private final Function<List<String>, List<String>> stemmingFunction;

    protected PhraseTree() {
        this(
            ImmutableSet.of(),
            ImmutableSet.of(),
            PhraseTree::getLowerCasedWords);
    }

    protected PhraseTree(
        final ImmutableSet<String> wordFilter,
        final ImmutableSet<String> staticPhrases
    ) {
        this(wordFilter, staticPhrases, PhraseTree::getLowerCasedWords);
    }

    protected PhraseTree(
        final ImmutableSet<String> wordFilter,
        final ImmutableSet<String> staticPhrases,
        final Function<List<String>, List<String>> stemmingFunction
    ) {

        this.wordFilter = checkNotNull(wordFilter, "wordFilter");
        this.staticPhrases = checkNotNull(staticPhrases, "staticPhrases");
        this.sortedPhraseMaker = new SortedPhraseMaker(wordFilter, staticPhrases);
        this.orderPreservedPhraseMaker = new OrderPreservedPhraseMaker(wordFilter, staticPhrases);
        this.stemmingFunction = checkNotNull(stemmingFunction, "stemmingFunction");

    }

    public static List<String> getLowerCasedWords(List<String> words) {
        return words
            .stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }

    abstract Map<String, PhraseTreeNode> getNodes();

    abstract Map<UUID, PhraseTreeNode> getNodesById();

    abstract Map<PhraseTreePath, PhraseTreePath> getKnownPaths();


    public PhraseTreePath get(final String rawText) {
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

        final PhraseTreePath phraseTreePath = PhraseTreePath.of(path);

        return getKnownPaths().get(phraseTreePath);
    }

    public PhraseTreePath computeIfAbsent(final String rawText) {
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

        final PhraseTreePath phraseTreePath = PhraseTreePath.of(path);

        return getKnownPaths().computeIfAbsent(phraseTreePath, key -> key);

    }

    public String getSynonym(final String rawPhrase) {
        checkNotNull(rawPhrase, "rawPhrase");

        final PhraseTreePath path = get(rawPhrase);

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

    public List<String> getOrderPreservedParts(final PhraseTreePath phraseTreePath) {
        checkNotNull(phraseTreePath, "phraseTreePath");

        return phraseTreePath
            .getOrderedPath()
            .stream()
            .filter(id -> getNodesById().containsKey(id))
            .map(id -> getNodesById().get(id).getUnstemmedKey())
            .collect(Collectors.toList());
    }

    public String getPhraseText(final PhraseTreePath phraseTreePath) {
        checkNotNull(phraseTreePath, "phraseTreePath");

        StringBuilder builder = new StringBuilder();

        for (UUID uuid : phraseTreePath
            .getOrderedPath()) {
            final PhraseTreeNode node = getNodesById().get(uuid);

            checkNotNull(node, "Unknown node id in path!");

            builder = builder.append(node.getUnstemmedKey());
        }

        return builder.toString();
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
}
