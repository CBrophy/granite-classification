package org.granite.classification.model;

import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClassifierCheat {
    private String label;
    private TreeSet<String> keywords = new TreeSet<>();
    private TreeSet<String> falseSignalWords = new TreeSet<>();

    ClassifierCheat(){}

    public ClassifierCheat(final String label) {
        this.label = checkNotNull(label, "label");
    }

    public String getLabel() {
        return label;
    }

    public TreeSet<String> getKeywords() {
        return keywords;
    }

    public TreeSet<String> getFalseSignalWords() {
        return falseSignalWords;
    }
}
