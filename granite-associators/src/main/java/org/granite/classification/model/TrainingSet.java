package org.granite.classification.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TrainingSet<K extends Comparable<K>, V> {

  private final ImmutableMap<V, Double> valueFrequency;
  private final ImmutableMap<V, ImmutableMap<V, Double>> valueToValueFrequency;
  private final double totalValueFrequency;
  private final ImmutableList<TrainingRow<K, V>> trainingRows;

  TrainingSet(
      final ImmutableList<TrainingRow<K, V>> trainingRows,
      final ImmutableMap<V, Double> valueFrequency,
      final ImmutableMap<V, ImmutableMap<V, Double>> valueToValueFrequency,
      final double totalValueFrequency
  ) {
    this.trainingRows = checkNotNull(trainingRows, "trainingRows");
    this.totalValueFrequency = totalValueFrequency;
    this.valueFrequency = checkNotNull(valueFrequency, "valueFrequency");
    this.valueToValueFrequency = checkNotNull(valueToValueFrequency, "valueToValueFrequency");

  }

  public boolean isEmpty() {
    return trainingRows.size() == 0;
  }

  public ImmutableMap<V, Double> getValueFrequency() {
    return valueFrequency;
  }

  public ImmutableMap<V, ImmutableMap<V, Double>> getValueToValueFrequency() {
    return valueToValueFrequency;
  }

  public double getTotalValueFrequency() {
    return totalValueFrequency;
  }

  public ImmutableList<TrainingRow<K, V>> getTrainingRows() {
    return trainingRows;
  }

  public static class Builder<K extends Comparable<K>, V> {

    private Collection<TrainingRow<K, V>> trainingRows = ImmutableList.of();
    private boolean strictAssociation = false;
    private double totalValueFrequency = 0.0;
    private Map<V, Double> valueFrequency;
    private Map<V, HashMap<V, Double>> valueToValueFrequency;

    public Builder() {

    }

    public Collection<TrainingRow<K, V>> getTrainingRows() {
      return trainingRows;
    }

    public boolean isStrictAssociation() {
      return strictAssociation;
    }

    public Builder<K, V> withTrainingRows(final Collection<TrainingRow<K, V>> trainingRows) {
      this.trainingRows = trainingRows;
      return this;
    }

    public Builder<K, V> withStrictAssociation(final boolean strictAssociation) {
      this.strictAssociation = strictAssociation;
      return this;
    }

    public TrainingSet<K, V> build() {
      this.validateTrainingRows();
      this.findValueFrequencies();

      return new TrainingSet<K, V>(
          ImmutableList.copyOf(trainingRows),
          ImmutableMap.copyOf(valueFrequency),
          createImmutableValueToValue(),
          totalValueFrequency
      );
    }

    private ImmutableMap<V, ImmutableMap<V, Double>> createImmutableValueToValue() {
      ImmutableMap.Builder<V, ImmutableMap<V, Double>> builder = ImmutableMap.builder();

      for (Entry<V, HashMap<V, Double>> mapEntry : valueToValueFrequency.entrySet()) {
        builder.put(mapEntry.getKey(), ImmutableMap.copyOf(mapEntry.getValue()));
      }

      return builder.build();
    }

    private void validateTrainingRows() {
      checkState(trainingRows.size() > 0, "No training rows to build from!");

      for (TrainingRow<K, V> trainingRow : trainingRows) {
        final ImmutableSet<V> rowValues = ImmutableSet.copyOf(trainingRow.getValues());

        checkState(rowValues.size() > 0, "Training row %s has no values",
            trainingRow.getId());

        checkState(rowValues.size() == trainingRow.getValues().size(),
            "Training row %s contains duplicate values",
            trainingRow.getId());

        for (V value : rowValues) {
          checkNotNull(value, "Training row %s contains a null", trainingRow.getId());
        }

      }

    }

    private void findValueFrequencies() {
      this.valueFrequency = new HashMap<>();
      this.valueToValueFrequency = new HashMap<>();

      this.totalValueFrequency = 0.0;

      // Find the frequency of all values and count the number of times the values
      // appear alongside one another
      for (TrainingRow<K, V> trainingRow : trainingRows) {

        for (int outerIndex = 0; outerIndex < trainingRow.getValues().size();
            outerIndex++) {

          final V outerValue = trainingRow.getValues().get(outerIndex);

          final double outerCount = valueFrequency.getOrDefault(outerValue, 0.0);

          valueFrequency.put(outerValue, outerCount + 1.0);

          this.totalValueFrequency += 1.0;

          // Find value-to-value frequency (without strict association)
          // eg. [1, 2, 3, 4]
          // 4 -> {1:1, 2:1, 3:1, 4:1}
          // 3 -> {1:1, 2:1, 3:1, 4:1}
          // etc

          // Find value-to-value frequency (with strict association)
          // eg. [1, 2, 3, 4]
          // 4 -> {1:1, 2:1, 3:1}
          // 3 -> {1:1, 2:1, 4:1}
          for (int innerIndex = 0; innerIndex <
              trainingRow.getValues().size(); innerIndex++) {

            if (isStrictAssociation() && innerIndex == outerIndex) {
              continue;
            }

            final V innerValue = trainingRow.getValues().get(innerIndex);

            final HashMap<V, Double> associationMap = valueToValueFrequency
                .computeIfAbsent(
                    outerValue,
                    key -> new HashMap<>());

            final double associationCount = associationMap
                .getOrDefault(innerValue, 0.0);

            associationMap.put(innerValue, associationCount + 1.0);
          }
        }

      }

    }
  }
}
