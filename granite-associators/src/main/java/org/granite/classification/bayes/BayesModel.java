package org.granite.classification.bayes;

import java.util.ArrayList;
import org.granite.classification.model.AssociationModel;

import java.util.List;
import java.util.Map;
import org.granite.math.StatsTools;

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
    public double meanProbability(V value, List<V> givenAssociations) {
        checkNotNull(value, "value");
        checkNotNull(givenAssociations, "givenAssociations");

        final BayesAssociationStatistics<V> valueStatistics = bayesAssociationStatisticsMap
            .get(value);

        if (valueStatistics == null) {
            return 0.0;
        }

        if (givenAssociations.isEmpty()) {
            return valueStatistics.getLikelihood();
        }

        final List<Double> results = new ArrayList<>();

        for (V associatedValue : givenAssociations) {
            checkNotNull(associatedValue, "givenAssociations cannot contain a null");

            final BayesAssociationStatistics<V> associatedValueStatistics = bayesAssociationStatisticsMap
                .get(associatedValue);

            if (associatedValueStatistics == null) {
                results.add(0.0);
            } else {
                results.add(associatedValueStatistics
                    .getAssociatedValuePosteriorProbabilities()
                    .getOrDefault(value, 0.0));
            }

        }

        return StatsTools.mean(results);
    }
}
