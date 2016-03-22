package org.granite.classification.model;

import com.google.common.collect.Sets;

import org.granite.classification.utils.TextUtils;
import org.granite.math.MathTools;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class Classification implements Comparable<Classification> {
    private String label;
    private TreeMap<String, Integer> wordFrequencyMap = new TreeMap<>();
    private TreeMap<String, Double> wordProbabilityMap = new TreeMap<>();
    private TreeSet<String> falseSignalWords = new TreeSet<>();
    private HashSet<Classification> complimentClassifications = new HashSet<>();
    private int classificationFrequency = 0;
    private double likelihood = 0.0;

    Classification() {
    }

    public Classification(final String label) {
        this.label = checkNotNull(label, "label");
    }

    public String getLabel() {
        return label;
    }

    public TreeMap<String, Integer> getWordFrequencyMap() {
        return wordFrequencyMap;
    }

    public TreeSet<String> getFalseSignalWords() {
        return falseSignalWords;
    }

    public int getClassificationFrequency() {
        return classificationFrequency;
    }

    public void setClassificationFrequency(int classificationFrequency) {
        this.classificationFrequency = classificationFrequency;
    }

    public double getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(double likelihood) {
        this.likelihood = likelihood;
    }

    public TreeMap<String, Double> getWordProbabilityMap() {
        return wordProbabilityMap;
    }

    public HashSet<Classification> getComplimentClassifications() {
        return complimentClassifications;
    }

    public void addAll(final Iterable<String> words) {
        checkNotNull(words, "words");

        TextUtils.updateFrequencyMap(words, wordFrequencyMap);
    }

    public double score(final Set<String> documentWords) {
        checkNotNull(documentWords, "documentWords");

        double score = 0.0;

        for (String documentWord : documentWords) {

            final Double probability = getWordProbabilityMap().get(documentWord);

            if (probability != null) {
                score += probability;
            }
        }

        return MathTools.round((score / (double) getWordProbabilityMap().size()) * getLikelihood(), 3);
    }

    @Override
    public int compareTo(Classification classification) {
        return getLabel().compareTo(classification.getLabel());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Classification && ((Classification) obj).getLabel().equalsIgnoreCase(getLabel());
    }
}
