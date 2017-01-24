package org.granite.classification.model;

import java.util.List;
import java.util.Map;

public interface AssociativeModel<V> {

    Map<V, Double> meanScoreObservation(final List<V> values);
}
