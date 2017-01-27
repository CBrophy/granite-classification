package org.granite.classification.frequency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.granite.classification.model.AssociationModel;
import org.granite.classification.model.AssociationStatistics;

public class FrequencyModel<V> extends AssociationModel<V, AssociationStatistics<V>> {

    FrequencyModel(
        Map<V, AssociationStatistics<V>> associationStatisticsMap,
        double totalValueFrequency) {
        super(totalValueFrequency, associationStatisticsMap);
    }

    @Override
    public Map<V, Double> supportingProbabilities(V value, List<V> givenAssociations) {
        // Conditional probability of P(A:B)
        // Calculate AVG( P(A ∩ B) / P(B) )

        checkNotNull(value, "value");
        checkNotNull(givenAssociations, "givenAssociations");

        final AssociationStatistics<V> valueStatistics = getAssociationStatisticsMap().get(value);

        if (valueStatistics == null) {
            return ImmutableMap.of();
        }

        if (givenAssociations.isEmpty()) {
            return ImmutableMap.of();
        }

        final Map<V, Double> results = new HashMap<V, Double>();

        for (V associatedValue : givenAssociations) {
            checkNotNull(associatedValue, "givenAssociations cannot contain a null");

            final AssociationStatistics<V> associatedValueStatistics = getAssociationStatisticsMap()
                .get(associatedValue);

            if (associatedValueStatistics == null) {
                results.put(associatedValue, 0.0);
            } else {

                final Double associationProbability = associatedValueStatistics
                    .getAssociatedValueProbabilities()
                    .get(value);

                if (associationProbability == null) {

                    results.put(associatedValue, 0.0);

                } else {

                    results.put(associatedValue,
                        (valueStatistics.getProbability() * associatedValueStatistics
                            .getProbability())
                            / associationProbability);
                }
            }
        }

        return results;
    }
}
