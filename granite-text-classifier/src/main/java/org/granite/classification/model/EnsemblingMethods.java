package org.granite.classification.model;

import com.google.common.collect.ImmutableMap;

import org.granite.math.StatsTools;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class EnsemblingMethods {

    public static class StandardDeviationEnsembler implements Function<ImmutableMap<String, ImmutableMap<String, Double>>, ImmutableMap<String, Double>> {

        private final double standardDeviationsAboveMean;

        public StandardDeviationEnsembler(final double standardDeviationsAboveMean) {
            this.standardDeviationsAboveMean = standardDeviationsAboveMean;
        }

        @Override
        public ImmutableMap<String, Double> apply(ImmutableMap<String, ImmutableMap<String, Double>> classificationScores) {
            checkNotNull(classificationScores, "classificationPosteriors");

            final HashMap<String, Double> result = new HashMap<>();

            for (Map.Entry<String, ImmutableMap<String, Double>> classificationWordPosteriorEntry : classificationScores.entrySet()) {

                final String classification = classificationWordPosteriorEntry.getKey();

                for (Double posterior : classificationWordPosteriorEntry.getValue().values()) {

                    double existingMaxValue = result.getOrDefault(classification, Double.MIN_VALUE);

                    result.put(classification, Math.max(existingMaxValue, posterior));
                }

            }

            final ImmutableMap.Builder<String, Double> builder = ImmutableMap.builder();

            final double mean = StatsTools.mean(result.values());

            final double standardDeviation = StatsTools.standardDev(result.values(), mean);

            result
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > mean + (standardDeviationsAboveMean * standardDeviation))
                    .forEach(entry -> builder.put(entry.getKey(), entry.getValue()));

            return builder.build();
        }
    }
}
