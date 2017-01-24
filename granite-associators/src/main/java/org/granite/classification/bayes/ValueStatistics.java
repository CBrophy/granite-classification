package org.granite.classification.bayes;

import com.google.common.math.DoubleMath;

import org.granite.math.ProbabilityTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ValueStatistics<V> {
    private V value;
    private double probability;
    private double likelihood;
    private double frequency;
    private double associationFrequency;
    private Map<V, Double> associatedValueProbabilities = new HashMap<>();
    private Map<V, Double> associatedValuePosteriorProbabilities = new HashMap<>();

    public ValueStatistics(
            final V value,
            final double probability,
            final double frequency
    ) {
        this.value = checkNotNull(value, "value");
        this.probability = probability;
        this.frequency = frequency;
    }

    public double getLikelihood() {
        return likelihood;
    }

    void setLikelihood(double likelihood) {
        this.likelihood = likelihood;
    }

    public V getValue() {
        return value;
    }

    public double getProbability() {
        return probability;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getAssociationFrequency() {
        return associationFrequency;
    }

    void setAssociationFrequency(double associationFrequency) {
        this.associationFrequency = associationFrequency;
    }

    public Map<V, Double> getAssociatedValueProbabilities() {
        return associatedValueProbabilities;
    }

    public Map<V, Double> getAssociatedValuePosteriorProbabilities() {
        return associatedValuePosteriorProbabilities;
    }

    public double findAssociationLikelihood(final V key) {
        checkNotNull(key, "key");

        final double associatedProbability = associatedValueProbabilities.getOrDefault(
                key,
                1.0 / (associationFrequency + 1.0)
        );

        if (DoubleMath.fuzzyEquals(associatedProbability, 0.0, 0.00001)) {
            return 0.0; //No likelihood
        }

        if (associatedValueProbabilities.size() == 0) return associatedProbability;

        final List<Double> otherAssociationProbabilities = associatedValueProbabilities
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(key))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        if(otherAssociationProbabilities.size() == 0) return associatedProbability;

        double denominator = ProbabilityTools
                .independentUnion(otherAssociationProbabilities);

        return denominator > 0.0 ? associatedProbability / denominator : associatedProbability;
    }
}
