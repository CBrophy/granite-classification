package org.granite.classification.model;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.granite.base.KeyValue;
import org.granite.math.PercentileTools;
import org.granite.math.StatsTools;

public abstract class AssociationModel<V> {

    private final double totalValueFrequency;

    public AssociationModel(double totalValueFrequency) {
        this.totalValueFrequency = totalValueFrequency;
    }

    public double getTotalValueFrequency() {
        return totalValueFrequency;
    }

    public abstract Map<V, Double> supportingProbabilities(final V value,
        final List<V> givenAssociations);

    public double meanProbability(
        final V value,
        final List<V> givenAssociations) {
        return ensembleProbability(value, givenAssociations, StatsTools::mean);
    }

    public Map<V, Double> meanProbability(
        final List<V> values,
        final List<V> givenAssociations) {
        return ensembleProbability(values, givenAssociations, StatsTools::mean);
    }

    public double medianProbability(
        final V value,
        final List<V> givenAssociations) {
        return ensembleProbability(
            value,
            givenAssociations,
            probabilities ->
                PercentileTools.median(
                    probabilities
                        .stream()
                        .sorted()
                        .collect(Collectors.toList())
                )

        );
    }

    public Map<V, Double> medianProbability(
        final List<V> value,
        final List<V> givenAssociations) {
        return ensembleProbability(
            value,
            givenAssociations,
            probabilities ->
                PercentileTools.median(
                    probabilities
                        .stream()
                        .sorted()
                        .collect(Collectors.toList())
                )

        );
    }

    public double ensembleProbability(
        final V value,
        final List<V> givenAssociations,
        final Function<Collection<Double>, Double> ensembleFunction) {
        checkNotNull(value, "value");
        checkNotNull(givenAssociations, "givenAssociations");
        checkNotNull(ensembleFunction, "ensembleFunction");

        final Map<V, Double> results = supportingProbabilities(value, givenAssociations);

        if (results == null || results.isEmpty()) {
            return 0.0;
        }

        return ensembleFunction.apply(results.values());
    }

    public Map<V, Double> ensembleProbability(
        final List<V> values,
        final List<V> givenAssociations,
        final Function<Collection<Double>, Double> ensembleFunction) {
        checkNotNull(values, "values");
        checkNotNull(givenAssociations, "givenAssociations");

        final HashMap<V, Double> result = new HashMap<>();

        for (V value : values) {
            result.put(value, ensembleProbability(value, givenAssociations, ensembleFunction));
        }

        return result;

    }

    public Map<V, Map<V, Double>> supportingProbabilities(final List<V> values,
        final List<V> givenAssociations) {
        checkNotNull(values, "values");
        checkNotNull(givenAssociations, "givenAssociations");

        final HashMap<V, Map<V, Double>> result = new HashMap<>();

        for (V value : values) {
            result.put(value, supportingProbabilities(value, givenAssociations));
        }

        return result;

    }

    public KeyValue<V, Double> mostProbable(
        final List<V> values,
        final List<V> givenAssociations,
        final Function<Collection<Double>, Double> ensembleFunction) {
        checkNotNull(values, "values");
        checkNotNull(givenAssociations, "givenAssociations");

        final HashMap<V, Double> result = new HashMap<>();

        V highestValue = null;
        double highestProbability = -1.0;

        for (V value : values) {
            double probability = ensembleProbability(value, givenAssociations, ensembleFunction);

            if (probability > highestProbability) {
                highestProbability = probability;
                highestValue = value;
            }
        }

        return new KeyValue<>(highestValue, highestProbability);

    }

}
