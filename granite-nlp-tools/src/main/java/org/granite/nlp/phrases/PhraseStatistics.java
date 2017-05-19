package org.granite.nlp.phrases;

import java.util.HashMap;
import java.util.TreeSet;

public class PhraseStatistics {

    private final String phraseText;
    private final PhraseTreePath phraseTreePath;
    private final double lexicalProbability;
    private final double lexicalLikelihood;
    private final HashMap<PhraseTreePath, Double> associatedPhraseProbability = new HashMap<>();
    private final HashMap<PhraseTreePath, Double> associatedPhrasePosteriorProbility = new HashMap<>();
    private final HashMap<String, Double> subclassPhraseProbability = new HashMap<>();
    private final TreeSet<String> superclassPhrases = new TreeSet<>();

    public PhraseStatistics(String phraseText,
        PhraseTreePath phraseTreePath,
        double lexicalProbability,
        double lexicalLikelihood) {
        this.phraseText = phraseText;
        this.phraseTreePath = phraseTreePath;
        this.lexicalProbability = lexicalProbability;
        this.lexicalLikelihood = lexicalLikelihood;
    }

    public String getPhraseText() {
        return phraseText;
    }

    public PhraseTreePath getPhraseTreePath() {
        return phraseTreePath;
    }

    public double getLexicalLikelihood() {
        return lexicalLikelihood;
    }

    public double getLexicalProbability() {
        return lexicalProbability;
    }

    public HashMap<PhraseTreePath, Double> getAssociatedPhraseProbability() {
        return associatedPhraseProbability;
    }

    public HashMap<PhraseTreePath, Double> getAssociatedPhrasePosteriorProbility() {
        return associatedPhrasePosteriorProbility;
    }

    public HashMap<String, Double> getSubclassPhraseProbability() {
        return subclassPhraseProbability;
    }

    public TreeSet<String> getSuperclassPhrases() {
        return superclassPhrases;
    }
}
