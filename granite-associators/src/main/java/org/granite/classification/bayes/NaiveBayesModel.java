package org.granite.classification.bayes;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.model.AssociativeModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesModel<V> implements AssociativeModel<V> {
    private final Map<V, ValueStatistics<V>> valueStatisticsMap;
    private final double totalValueFrequency;

    public NaiveBayesModel(
            final Map<V, ValueStatistics<V>> valueStatisticsMap,
            final double totalValueFrequency) {
        this.valueStatisticsMap = checkNotNull(valueStatisticsMap, "valueStatisticsMap");
        this.totalValueFrequency = totalValueFrequency;
    }

    @Override
    public Map<V, Double> meanScoreObservation(List<V> values) {

        checkNotNull(values, "values");

        if (values.size() == 0) return ImmutableMap.of();

        final HashMap<V, Double> result = new HashMap<>();

        for (V value : values) {
            checkNotNull(value, "values cannot contain a null");

            final ValueStatistics<V> statistics = valueStatisticsMap.get(value);

            if (statistics == null) {
                result.put(value, 0.0);
                continue;
            }

            double totalPosteriorProbability = statistics.getLikelihood();

            for (V otherValue : values) {

                if (value.equals(otherValue)) continue;

                final ValueStatistics<V> otherValueStatistics = valueStatisticsMap.get(otherValue);

                if (otherValueStatistics == null) continue;

                totalPosteriorProbability += otherValueStatistics
                        .getAssociatedValuePosteriorProbabilities()
                        .getOrDefault(value, 0.0);
            }

            result.put(value, totalPosteriorProbability / (double) values.size());

        }

        return result;
    }


    public Map<V, ValueStatistics<V>> getValueStatisticsMap() {
        return valueStatisticsMap;
    }

    public double getTotalValueFrequency() {
        return totalValueFrequency;
    }
}
