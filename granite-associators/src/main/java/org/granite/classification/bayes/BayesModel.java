package org.granite.classification.bayes;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import org.granite.classification.model.AssociationModel;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class BayesModel<V> extends AssociationModel<V> {

    private final Map<V, BayesAssociationStatistics<V>> bayesAssociationStatisticsMap;

    BayesModel(
        final Map<V, BayesAssociationStatistics<V>> bayesAssociationStatisticsMap,
        final double totalValueFrequency
    ) {
        super(totalValueFrequency);
        this.bayesAssociationStatisticsMap = checkNotNull(bayesAssociationStatisticsMap,
            "bayesAssociationStatisticsMap");

    }

    public Map<V, BayesAssociationStatistics<V>> getBayesAssociationStatisticsMap() {
        return bayesAssociationStatisticsMap;
    }

    @Override
    public Map<V, Double> supportingProbabilities(V value, List<V> givenAssociations) {
        checkNotNull(value, "value");
        checkNotNull(givenAssociations, "givenAssociations");

        final BayesAssociationStatistics<V> valueStatistics = bayesAssociationStatisticsMap
            .get(value);

        if (valueStatistics == null) {
            return ImmutableMap.of();
        }

        if (givenAssociations.isEmpty()) {
            return ImmutableMap.of();
        }

        final HashMap<V, Double> results = new HashMap<>();

        for (V associatedValue : givenAssociations) {
            checkNotNull(associatedValue, "givenAssociations cannot contain a null");

            final BayesAssociationStatistics<V> associatedValueStatistics = bayesAssociationStatisticsMap
                .get(associatedValue);

            if (associatedValueStatistics == null) {
                results.put(associatedValue, 0.0);
            } else {
                results.put(associatedValue, associatedValueStatistics
                    .getAssociatedValuePosteriorProbabilities()
                    .getOrDefault(value, 0.0));
            }

        }

        return results;
    }
}
