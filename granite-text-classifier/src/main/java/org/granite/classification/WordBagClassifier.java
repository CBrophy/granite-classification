package org.granite.classification;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.model.TrainingSet;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WordBagClassifier {

    private final TrainingSet trainingSet;

    protected WordBagClassifier(final TrainingSet trainingSet) {
        this.trainingSet = checkNotNull(trainingSet);
    }

    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    public abstract ImmutableMap<String, ImmutableMap<String, Double>> classify(final Set<String> wordBag);


}
