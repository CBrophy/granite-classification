package org.granite.classification.model;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class AssociationStatistics<V> {

    private V value;
    private double probability;
    private double likelihood;
    private double frequency;
    private double associationFrequency;
    private Map<V, Double> associatedValueProbabilities = new HashMap<>();

    public AssociationStatistics(final V value) {
        this.value = value;
    }

    public AssociationStatistics(final AssociationStatistics<V> associationStatistics) {
        checkNotNull(associationStatistics, "associationStatistics");
        this.value = associationStatistics.value;
        this.probability = associationStatistics.probability;
        this.likelihood = associationStatistics.likelihood;
        this.frequency = associationStatistics.frequency;
        this.associatedValueProbabilities = associationStatistics.associatedValueProbabilities;
        this.associationFrequency = associationStatistics.associationFrequency;
    }

    public AssociationStatistics(
        final V value,
        final double probability,
        final double frequency,
        final double associationFrequency
    ) {
        this.value = checkNotNull(value, "value");
        this.probability = probability;
        this.frequency = frequency;
        this.associationFrequency = associationFrequency;
    }

    public double getLikelihood() {
        return likelihood;
    }

    public AssociationStatistics<V> withLikelihood(double likelihood) {
        this.likelihood = likelihood;
        return this;
    }

    public V getValue() {
        return value;
    }

    public double getProbability() {
        return probability;
    }

    public AssociationStatistics<V> withProbability(final double probability) {
        this.probability = probability;
        return this;
    }

    public double getFrequency() {
        return frequency;
    }

    public AssociationStatistics<V> withFrequency(final double frequency) {
        this.frequency = frequency;
        return this;
    }

    public double getAssociationFrequency() {
        return associationFrequency;
    }

    public AssociationStatistics<V> withAssociationFrequency(final double associationFrequency) {
        this.associationFrequency = associationFrequency;
        return this;
    }

    public Map<V, Double> getAssociatedValueProbabilities() {
        return associatedValueProbabilities;
    }

}
