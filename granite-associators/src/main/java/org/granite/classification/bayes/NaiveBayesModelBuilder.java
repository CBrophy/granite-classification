package org.granite.classification.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

import org.granite.classification.model.AssociativeModel;
import org.granite.classification.model.TrainingRow;
import org.granite.math.ProbabilityTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class NaiveBayesModelBuilder<K extends Comparable<K>, V> {

    private final HashMap<V, Double> valueFrequency = new HashMap<>();
    private final HashMap<V, HashMap<V, Double>> valueToValueFrequency = new HashMap<>();
    private double totalValueFrequency = 0.0;
    private List<TrainingRow<K, V>> trainingRows;

    public NaiveBayesModelBuilder() {
        this(ImmutableList.of());
    }

    public NaiveBayesModelBuilder(final List<TrainingRow<K, V>> trainingRows) {
        this.trainingRows = checkNotNull(trainingRows, "trainingRows");
    }

    public NaiveBayesModelBuilder<K, V> withTrainingRows(final List<TrainingRow<K, V>> trainingRows) {

        this.trainingRows = checkNotNull(trainingRows, "trainingRows");

        return this;
    }

    public AssociativeModel<V> build() {

        validateTrainingRows();

        findValueFrequencies();

        return new NaiveBayesModel<>(
                calculateStatistics(),
                this.totalValueFrequency
        );
    }

    private void validateTrainingRows() {
        checkState(trainingRows.size() > 0, "No training rows to build from!");

        for (TrainingRow<K, V> trainingRow : trainingRows) {
            final ImmutableSet<V> rowValues = ImmutableSet.copyOf(trainingRow.getValues());

            checkState(rowValues.size() > 0, "Training row %s has no values", trainingRow.getId());

            checkState(rowValues.size() == trainingRow.getValues().size(),
                       "Training row %s contains duplicate values",
                       trainingRow.getId());
        }

    }

    private Map<V, ValueStatistics<V>> calculateStatistics() {

        if (valueToValueFrequency.isEmpty() || valueFrequency.isEmpty()) return ImmutableMap.of();

        this.totalValueFrequency = 0.0;

        for (Double valueFrequency : valueFrequency.values()) {
            totalValueFrequency += valueFrequency;
        }

        if (DoubleMath.fuzzyEquals(totalValueFrequency, 0.0, 0.0001)) return ImmutableMap.of();

        final HashMap<V, ValueStatistics<V>> result = new HashMap<>();

        // Calculate overall probability of each value
        for (Map.Entry<V, Double> frequencyEntry : valueFrequency.entrySet()) {

            final ValueStatistics<V> valueStatistics = new ValueStatistics<>(
                    frequencyEntry.getKey(),
                    frequencyEntry.getValue() / totalValueFrequency,
                    frequencyEntry.getValue()
            );

            result.put(
                    frequencyEntry.getKey(),
                    valueStatistics
            );

        }

        result
                .values()
                .forEach(this::calculateAssociativeProbabilities);

        this.calculateLikelihoods(result);

        this.calculateAssociativePosteriors(result);

        return result;
    }

    private void calculateLikelihoods(final Map<V, ValueStatistics<V>> trainingRowStatistics) {
        for (Map.Entry<V, ValueStatistics<V>> valueStatisticsEntry : trainingRowStatistics.entrySet()) {

            final List<Double> allOtherProbabilities = trainingRowStatistics
                    .entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().equals(valueStatisticsEntry.getKey()))
                    .map(entry -> entry.getValue().getProbability())
                    .collect(Collectors.toList());

            valueStatisticsEntry
                    .getValue()
                    .setLikelihood(
                            valueStatisticsEntry.getValue().getProbability() /
                                    ProbabilityTools.independentUnion(allOtherProbabilities)
                    );
        }

    }

    private void calculateAssociativeProbabilities(final ValueStatistics<V> valueStatistics) {
        checkNotNull(valueStatistics, "valueStatistics");

        final HashMap<V, Double> associationMap = valueToValueFrequency.getOrDefault(
                valueStatistics.getValue(),
                new HashMap<>());

        if (associationMap.isEmpty()) return;

        double totalAssociations = 0.0;

        for (Double associationFrequency : associationMap.values()) {
            totalAssociations += associationFrequency;
        }
        // Calculate P(associatedValue : value)
        for (Map.Entry<V, Double> associationEntry : associationMap.entrySet()) {
            valueStatistics.getAssociatedValueProbabilities()
                           .put(
                                   associationEntry.getKey(),
                                   associationEntry.getValue() / totalAssociations);
        }

        valueStatistics.setAssociationFrequency(totalAssociations);
    }

    private void calculateAssociativePosteriors(final Map<V, ValueStatistics<V>> trainingRowStatistics) {

        // Calculate P(value : associatedValue)
        // P(V:A) = (P(A:V) * P(V)) / P(A)
        // P(V) = prior
        // P(A:V) = P(A:V) / P(A:!V1 U A:!V2 U A:!V3...)
        for (V value : trainingRowStatistics.keySet()) {

            final ValueStatistics<V> valueStatistics = trainingRowStatistics.get(value);

            checkNotNull(valueStatistics, "valueStatistics");

            for (Map.Entry<V, Double> associatedEntry : valueStatistics.getAssociatedValueProbabilities()
                                                                       .entrySet()) {

                final ValueStatistics<V> associatedStatistics = trainingRowStatistics.get(
                        associatedEntry.getKey());

                checkNotNull(associatedStatistics, "associatedStatistics");

                final double likelihood = findAssociationLikelihood(
                        associatedEntry.getKey(),
                        associatedEntry.getValue(),
                        valueStatistics.getAssociatedValueProbabilities()
                );

                final double posterior = (valueStatistics.getProbability() * likelihood) /
                        associatedStatistics.getProbability();

                valueStatistics.getAssociatedValuePosteriorProbabilities()
                               .put(associatedEntry.getKey(),
                                    posterior);

            }

        }

    }

    private double findAssociationLikelihood(final V key,
                                             final double associatedProbability,
                                             final Map<V, Double> associatedValueProbabilities) {
        if (DoubleMath.fuzzyEquals(associatedProbability, 0.0, 0.00001)) {
            return 0.0; //No likelihood
        }

        if (associatedValueProbabilities.size() == 1) return associatedProbability;

        double denominator = ProbabilityTools
                .independentUnion(associatedValueProbabilities
                                          .entrySet()
                                          .stream()
                                          .filter(entry -> !entry.getKey().equals(key))
                                          .map(Map.Entry::getValue)
                                          .collect(Collectors.toList()));

        return denominator > 0.0 ? associatedProbability / denominator : associatedProbability;
    }

    private void findValueFrequencies() {
        valueFrequency.clear();
        valueToValueFrequency.clear();

        // Find the frequency of all values and count the number of times the values
        // appear alongside one another
        for (TrainingRow<K, V> trainingRow : trainingRows) {

            for (int outerIndex = 0; outerIndex < trainingRow.getValues().size(); outerIndex++) {

                final V outerValue = trainingRow.getValues().get(outerIndex);

                final double outerCount = valueFrequency.getOrDefault(outerValue, 0.0);

                valueFrequency.put(outerValue, outerCount + 1.0);

                // eg. [1, 2, 3, 4]
                // 4 -> {1:1, 2:1, 3:1}
                // 3 -> {1:1, 2:1, 4:1}
                // etc

                for (int innerIndex = 0; innerIndex <
                        trainingRow.getValues().size(); innerIndex++) {

                    if (innerIndex == outerIndex) continue;

                    final V innerValue = trainingRow.getValues().get(innerIndex);

                    final HashMap<V, Double> associationMap = valueToValueFrequency.computeIfAbsent(
                            outerValue,
                            key -> new HashMap<>());

                    final double associationCount = associationMap.getOrDefault(innerValue, 0.0);

                    associationMap.put(innerValue, associationCount + 1.0);
                }
            }

        }
    }
}
