package org.granite.classification.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TrainingSet<K extends Comparable<K>, V> {

    private ImmutableMap<V, Double> valueFrequency;
    private ImmutableMap<V, HashMap<V, Double>> valueToValueFrequency;
    private double totalValueFrequency = 0.0;
    private List<TrainingRow<K, V>> trainingRows;

    private TrainingSet(final List<TrainingRow<K, V>> trainingRows) {
        this.trainingRows = checkNotNull(trainingRows, "trainingRows");
    }

    public static <K extends Comparable<K>, V> TrainingSet<K, V> build(
        final List<TrainingRow<K, V>> trainingRows) {
        final TrainingSet<K, V> result = new TrainingSet<>(trainingRows);
        result.validateTrainingRows();
        result.findValueFrequencies();
        return result;
    }


    public boolean isEmpty() {
        return trainingRows.size() == 0;
    }

    public ImmutableMap<V, Double> getValueFrequency() {
        return valueFrequency;
    }

    public ImmutableMap<V, HashMap<V, Double>> getValueToValueFrequency() {
        return valueToValueFrequency;
    }

    public double getTotalValueFrequency() {
        return totalValueFrequency;
    }

    public List<TrainingRow<K, V>> getTrainingRows() {
        return trainingRows;
    }

    private void validateTrainingRows() {
        checkState(trainingRows.size() > 0, "No training rows to build from!");

        for (TrainingRow<K, V> trainingRow : trainingRows) {
            final ImmutableSet<V> rowValues = ImmutableSet.copyOf(trainingRow.getValues());

            checkState(rowValues.size() > 0, "Training row %s has no values", trainingRow.getId());

            checkState(rowValues.size() == trainingRow.getValues().size(),
                "Training row %s contains duplicate values",
                trainingRow.getId());

            for (V value : rowValues) {
                checkNotNull(value, "Training row %s contains a null", trainingRow.getId());
            }

        }

    }

    private void findValueFrequencies() {
        final Map<V, Double> valueFrequency = new HashMap<>();
        final Map<V, HashMap<V, Double>> valueToValueFrequency = new HashMap<>();

        this.totalValueFrequency = 0.0;

        // Find the frequency of all values and count the number of times the values
        // appear alongside one another
        for (TrainingRow<K, V> trainingRow : trainingRows) {

            for (int outerIndex = 0; outerIndex < trainingRow.getValues().size(); outerIndex++) {

                final V outerValue = trainingRow.getValues().get(outerIndex);

                final double outerCount = valueFrequency.getOrDefault(outerValue, 0.0);

                valueFrequency.put(outerValue, outerCount + 1.0);

                this.totalValueFrequency += 1.0;

                // Find value-to-value frequency
                // eg. [1, 2, 3, 4]
                // 4 -> {1:1, 2:1, 3:1}
                // 3 -> {1:1, 2:1, 4:1}
                // etc
                for (int innerIndex = 0; innerIndex <
                    trainingRow.getValues().size(); innerIndex++) {

                    if (innerIndex == outerIndex) {
                        continue;
                    }

                    final V innerValue = trainingRow.getValues().get(innerIndex);

                    final HashMap<V, Double> associationMap = valueToValueFrequency.computeIfAbsent(
                        outerValue,
                        key -> new HashMap<>());

                    final double associationCount = associationMap.getOrDefault(innerValue, 0.0);

                    associationMap.put(innerValue, associationCount + 1.0);
                }
            }

        }

        this.valueFrequency = ImmutableMap.copyOf(valueFrequency);
        this.valueToValueFrequency = ImmutableMap.copyOf(valueToValueFrequency);
    }
}
