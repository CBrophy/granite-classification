package org.granite.classification.model;

import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class Classification implements Comparable<Classification> {
    private String label;
    private TreeMap<String, Integer> wordFrequencyMap = new TreeMap<>();
    private TreeSet<String> falseSignalWords = new TreeSet<>();
    private int trainingTextCount = 0;

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

    public int getTrainingTextCount() {
        return trainingTextCount;
    }

    public void setTrainingTextCount(int trainingTextCount) {
        this.trainingTextCount = trainingTextCount;
    }

    public void addAll(final Iterable<String> words) {
        checkNotNull(words, "words");

        words
                .forEach(word -> {
                    wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);
                });
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
