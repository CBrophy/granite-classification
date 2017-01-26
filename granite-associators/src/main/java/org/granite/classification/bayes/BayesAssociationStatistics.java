package org.granite.classification.bayes;

import com.google.common.math.DoubleMath;

import org.granite.classification.model.AssociationStatistics;
import org.granite.math.ProbabilityTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class BayesAssociationStatistics<V> extends AssociationStatistics<V> {

    private Map<V, Double> associatedValuePosteriorProbabilities = new HashMap<>();

    public BayesAssociationStatistics(AssociationStatistics<V> associationStatistics) {
        super(associationStatistics);
    }

    public BayesAssociationStatistics(V value) {
        super(value);
    }

    public BayesAssociationStatistics(V value,
        double probability,
        double frequency,
        double associationFrequency) {
        super(value, probability, frequency, associationFrequency);
    }

    public Map<V, Double> getAssociatedValuePosteriorProbabilities() {
        return associatedValuePosteriorProbabilities;
    }

    public double findAssociationLikelihood(final V key) {
        checkNotNull(key, "key");

        final Double associatedProbability = getAssociatedValueProbabilities().get(
            key);

        if (associatedProbability == null) {
            return 0.0;
        }

        if (DoubleMath.fuzzyEquals(associatedProbability, 0.0, 0.00001)) {
            return 0.0; //No likelihood
        }

        if (getAssociatedValueProbabilities().size() == 0) {
            return associatedProbability;
        }

        final List<Double> otherAssociationProbabilities = getAssociatedValueProbabilities()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().equals(key))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

        if (otherAssociationProbabilities.size() == 0) {
            return associatedProbability;
        }

        double denominator = ProbabilityTools
            .independentUnion(otherAssociationProbabilities);

        return denominator > 0.0 ? associatedProbability / denominator : associatedProbability;
    }
}
