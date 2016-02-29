package org.granite.classification.model;

import java.util.TreeSet;

public class TrainingText {
    private int id;
    private TreeSet<String> classifications = new TreeSet<String>();
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

    public String getText() {
        return text;
    }
}
