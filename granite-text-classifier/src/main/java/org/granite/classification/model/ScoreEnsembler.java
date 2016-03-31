package org.granite.classification.model;

import com.google.common.collect.ImmutableSet;

public abstract class ScoreEnsembler<T extends StringScore> {

    public abstract ImmutableSet<T> ensemble(final Iterable<T> classificationScores);
}
