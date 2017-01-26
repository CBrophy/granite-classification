package org.granite.classification.bayes;

import org.granite.classification.frequency.FrequencyModel;
import org.granite.classification.frequency.FrequencyModelBuilder;
import org.granite.classification.model.AssociationStatistics;
import org.granite.classification.model.TrainingSet;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class BayesModelBuilder {

    public static <K extends Comparable<K>, V> BayesModel<V> build(
        final TrainingSet<K, V> trainingSet
    ) {

        checkNotNull(trainingSet, "trainingSet");

        final FrequencyModel<V> frequencyModel = FrequencyModelBuilder
            .build(trainingSet);

        final HashMap<V, BayesAssociationStatistics<V>> result = new HashMap<>();

        for (AssociationStatistics<V> associationStatistics : frequencyModel
            .getAssociationStatisticsMap().values()) {

            final BayesAssociationStatistics<V> bayesAssociationStatistics = new BayesAssociationStatistics<>(
                associationStatistics);

            result.put(bayesAssociationStatistics.getValue(), bayesAssociationStatistics);
        }

        calculateAssociativePosteriors(result);

        return new BayesModel<>(result, frequencyModel.getTotalValueFrequency());
    }

    private static <V> void calculateAssociativePosteriors(
        final Map<V, BayesAssociationStatistics<V>> associationStatisticsMap) {

        // Calculate P(value : associatedValue)
        // P(V:A) = (P(A:V) * P(V)) / P(A)
        // P(V) = prior
        // P(A:V) = P(A:V) / P(A:!V1 U A:!V2 U A:!V3...)
        for (V value : associationStatisticsMap.keySet()) {

            final BayesAssociationStatistics<V> currentValueStatistics = associationStatisticsMap
                .get(value);

            checkNotNull(currentValueStatistics, "currentValueStatistics");

            for (Map.Entry<V, Double> associatedEntry : currentValueStatistics
                .getAssociatedValueProbabilities()
                .entrySet()) {

                final BayesAssociationStatistics<V> associatedValueStatistics = associationStatisticsMap
                    .get(
                        associatedEntry.getKey());

                checkNotNull(associatedValueStatistics, "associatedValueStatistics");

                final double associationLikelihood = currentValueStatistics
                    .findAssociationLikelihood(associatedEntry.getKey());

                final double posterior =
                    (currentValueStatistics.getProbability() * associationLikelihood) /
                        associatedValueStatistics.getProbability();

                currentValueStatistics.getAssociatedValuePosteriorProbabilities()
                    .put(associatedEntry.getKey(),
                        posterior);

            }

        }

    }

}
