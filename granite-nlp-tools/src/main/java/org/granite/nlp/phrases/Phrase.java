package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.granite.collections.CombinationGenerator;
import org.granite.collections.ListTools;

public abstract class Phrase implements Serializable {

  private ImmutableList<UUID> orderedPath;
  private ImmutableSortedSet<UUID> identitySet;
  private int hashCode;

  protected Phrase(
      ImmutableList<UUID> orderedPath,
      ImmutableSortedSet<UUID> identitySet,
      int hashCode
  ) {
    this.orderedPath = checkNotNull(orderedPath, "orderedPath");
    this.identitySet = checkNotNull(identitySet, "identitySet");
    this.hashCode = hashCode;
  }


  public ImmutableList<UUID> getOrderedPath() {
    return orderedPath;
  }

  public ImmutableSortedSet<UUID> getIdentitySet() {
    return identitySet;
  }

  @Override
  public int hashCode() {
    return this.hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Phrase
        &&
        getIdentitySet().size() == ((Phrase) obj).getIdentitySet().size();
  }

  public boolean divergesFrom(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    return !ListTools.listsMatch(getOrderedPath(), phrase.getOrderedPath());
  }

  public List<Phrase> componentize(int maxComponentLength) {
    if (getIdentitySet().isEmpty() || maxComponentLength < 1) {
      return ImmutableList.of();
    }

    int maxLength =
        maxComponentLength > getIdentitySet().size() ? getIdentitySet().size() : maxComponentLength;

    final ImmutableList<UUID> identityElements = getIdentitySet().asList();

    final HashSet<Phrase> results = new HashSet<>();

    for (int currentLength = 1; currentLength <= maxLength; currentLength++) {

      final CombinationGenerator combinationGenerator = new CombinationGenerator(
          identityElements.size(),
          currentLength
      );

      List<UUID> currentPath;

      while (combinationGenerator.hasMore()) {

        currentPath = new ArrayList<>();

        final int[] indices = combinationGenerator.getNext();

        for (int index = 0; index < indices.length; index++) {
          currentPath.add(identityElements.get(indices[index]));
        }

        final PermutationIterator<UUID> iterator = new PermutationIterator<>(currentPath);

        while (iterator.hasNext()) {
          results.add(create(iterator.next()));
        }
      }

    }

    return new ArrayList<>(results);
  }

  protected abstract Phrase create(final Iterable<UUID> path);

  public abstract boolean isComponentOf(final Phrase phrase);

}
