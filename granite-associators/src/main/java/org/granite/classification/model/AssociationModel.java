package org.granite.classification.model;


import java.util.List;

public abstract class AssociationModel<V> {

    private final double totalValueFrequency;

    public AssociationModel(double totalValueFrequency) {
        this.totalValueFrequency = totalValueFrequency;
    }

    public double getTotalValueFrequency() {
        return totalValueFrequency;
    }

    public abstract double meanProbability(final V value, final List<V> givenAssociations);
}
