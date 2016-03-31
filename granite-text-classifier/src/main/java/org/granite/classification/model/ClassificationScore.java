package org.granite.classification.model;

import java.util.TreeSet;

public class ClassificationScore extends StringScore {

    private TreeSet<StringScore> contributors = new TreeSet<>();

    public ClassificationScore(final String key) {
        super(key);
    }

    public TreeSet<StringScore> getContributors() {
        return contributors;
    }
}
