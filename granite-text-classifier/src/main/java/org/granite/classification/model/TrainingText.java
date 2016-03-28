package org.granite.classification.model;

import java.util.TreeSet;

public class TrainingText implements Comparable<TrainingText> {
    private int id;
    private TreeSet<String> classifications = new TreeSet<>();
    private TreeSet<String> wordBag = new TreeSet<>();
    private String text;

    TrainingText(){}

    public TrainingText(final int id, final TreeSet<String> classifications, final String text) {
        this.id = id;
        this.classifications = classifications;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public TreeSet<String> getClassifications() {
        return classifications;
    }

    public TreeSet<String> getWordBag() {
        return wordBag;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(TrainingText trainingText) {
        return Integer.compare(id, trainingText.getId());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TrainingText
                && getId() == ((TrainingText) obj).getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }
}
