package org.granite.classification.frequency;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.granite.classification.model.AssociationModel;
import org.granite.classification.model.AssociationStatistics;
import org.granite.math.StatsTools;

public class FrequencyModel<V> extends AssociationModel<V> {

    private final Map<V, AssociationStatistics<V>> associationStatisticsMap;

    FrequencyModel(
        Map<V, AssociationStatistics<V>> associationStatisticsMap,
        double totalValueFrequency) {
        super(totalValueFrequency);
        this.associationStatisticsMap = associationStatisticsMap;
    }

    public Map<V, AssociationStatistics<V>> getAssociationStatisticsMap() {
        return associationStatisticsMap;
    }

    @Override
    public double meanProbability(final V value, List<V> givenAssociations) {
        // Conditional probability of P(A:B)
        // Calculate AVG( P(A ∩ B) / P(B) )

        checkNotNull(value, "value");
        checkNotNull(givenAssociations, "givenAssociations");

        final AssociationStatistics<V> valueStatistics = associationStatisticsMap.get(value);

        if (valueStatistics == null) {
            return 0.0;
        }

        if (givenAssociations.isEmpty()) {
            return valueStatistics.getProbability();
        }

        final List<Double> results = new ArrayList<>();

        for (V associatedValue : givenAssociations) {
            checkNotNull(associatedValue, "givenAssociations cannot contain a null");

            final AssociationStatistics<V> associatedValueStatistics = associationStatisticsMap
                .get(associatedValue);

            if (associatedValueStatistics == null) {
                results.add(0.0);
            } else {

                final Double associationProbability = associatedValueStatistics
                    .getAssociatedValueProbabilities()
                    .get(value);

                if (associationProbability == null) {

                    results.add(0.0);

                } else {

                    results.add(
                        (valueStatistics.getProbability() * associatedValueStatistics
                            .getProbability())
                            / associationProbability);
                }
            }
        }

        return StatsTools.mean(results);
    }
}
