package org.granite.classification.model;

import com.google.common.collect.ImmutableSet;

public abstract class ScoreFilter<T extends StringScore> {

    public abstract ImmutableSet<T> filter(final Iterable<T> classificationScores);
}
