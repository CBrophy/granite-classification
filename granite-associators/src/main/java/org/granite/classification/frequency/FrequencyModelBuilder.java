package org.granite.classification.frequency;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.granite.classification.model.AssociationStatistics;
import org.granite.classification.model.TrainingSet;
import org.granite.math.ProbabilityTools;

public class FrequencyModelBuilder {

    public static <K extends Comparable<K>, V> FrequencyModel<V> build(
        final TrainingSet<K, V> trainingSet
    ) {
        checkNotNull(trainingSet, "trainingSet");

        checkArgument(trainingSet.getTotalValueFrequency() >= 1.0, "Training set has no values");

        final HashMap<V, AssociationStatistics<V>> result = new HashMap<>();

        // Calculate overall probability of each value and its associations
        for (Map.Entry<V, Double> frequencyEntry : trainingSet.getValueFrequency().entrySet()) {
            final AssociationStatistics<V> associationStatistics = new AssociationStatistics<V>(
                frequencyEntry.getKey()
            );

            result.put(
                frequencyEntry.getKey(),
                associationStatistics
            );

            associationStatistics
                .withFrequency(frequencyEntry.getValue())
                .withProbability(frequencyEntry.getValue() / trainingSet.getTotalValueFrequency());

            final ImmutableMap<V, Double> associationMap = trainingSet.getValueToValueFrequency().get(
                frequencyEntry.getKey());

            if (associationMap == null || associationMap.isEmpty()) {
                continue;
            }

            double totalAssociations = 0.0;

            for (Double associationFrequency : associationMap.values()) {
                totalAssociations += associationFrequency;
            }

            associationStatistics
                .withAssociationFrequency(totalAssociations);

            // Calculate P(associatedValue : value)
            for (Map.Entry<V, Double> associationEntry : associationMap.entrySet()) {
                associationStatistics.getAssociatedValueProbabilities()
                    .put(
                        associationEntry.getKey(),
                        associationEntry.getValue() / totalAssociations);
            }
        }

        calculateLikelihoods(result);

        return new FrequencyModel<>(
            result,
            trainingSet.getTotalValueFrequency()
        );
    }

    private static <V> void calculateLikelihoods(
        final HashMap<V, AssociationStatistics<V>> associationStatistics) {
        for (Map.Entry<V, AssociationStatistics<V>> statisticsEntry : associationStatistics
            .entrySet()) {

            final List<Double> allOtherProbabilities = associationStatistics
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(statisticsEntry.getKey()))
                .map(entry -> entry.getValue().getProbability())
                .collect(Collectors.toList());

            statisticsEntry
                .getValue()
                .withLikelihood(
                    statisticsEntry.getValue().getProbability() /
                        ProbabilityTools.independentUnion(allOtherProbabilities)
                );
        }

    }

}
