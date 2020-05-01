package org.granite.classification.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TrainingRow<K extends Comparable<K>, V> implements Comparable<TrainingRow<K, V>> {

  private K id;
  private List<V> values = new ArrayList<>();

  TrainingRow() {
  }

  public TrainingRow(K id, Collection<V> values) {
    this(id);
    this.values.addAll(checkNotNull(values, "values"));
  }

  public TrainingRow(K id) {
    this.id = checkNotNull(id, "id");
  }

  public K getId() {
    return id;
  }

  public List<V> getValues() {
    return values;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TrainingRow && getId().equals(((TrainingRow) obj).getId());
  }

  @Override
  public int compareTo(TrainingRow<K, V> trainingRow) {
    return getId().compareTo(trainingRow.getId());
  }
}
