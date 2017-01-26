package org.granite.classification.model;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;

public abstract class AssociationModel<V> {

    private final double totalValueFrequency;

    public AssociationModel(double totalValueFrequency) {
        this.totalValueFrequency = totalValueFrequency;
    }

    public double getTotalValueFrequency() {
        return totalValueFrequency;
    }

    public abstract double meanProbability(final V value, final List<V> givenAssociations);

    public Map<V, Double> meanProbability(final List<V> values, final List<V> givenAssociations){
        checkNotNull(values, "values");
        checkNotNull(givenAssociations, "givenAssociations");

        final HashMap<V, Double> result = new HashMap<>();

        for (V value : values) {
            result.put(value, meanProbability(value, givenAssociations));
        }

        return result;

    }

    public Pair<V, Double> mostProbable(final List<V> values, final List<V> givenAssociations){
        checkNotNull(values, "values");
        checkNotNull(givenAssociations, "givenAssociations");

        final HashMap<V, Double> result = new HashMap<>();

        V highestValue = null;
        double highestProbability = -1.0;

        for (V value : values) {
            double probability = meanProbability(value, givenAssociations);

            if(probability > highestProbability){
                highestProbability = probability;
                highestValue = value;
            }
        }

        return new Pair<>(highestValue, highestProbability);

    }
}
