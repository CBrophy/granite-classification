package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.granite.collections.CombinationGenerator;

public class PhraseTreePath {

  private ImmutableList<UUID> orderedPath;
  private ImmutableSortedSet<UUID> identitySet;
  private int hashCode;

  private PhraseTreePath(
      ImmutableList<UUID> orderedPath,
      ImmutableSortedSet<UUID> identitySet,
      int hashCode
  ) {
    this.orderedPath = checkNotNull(orderedPath, "orderedPath");
    this.identitySet = checkNotNull(identitySet, "identitySet");
    this.hashCode = hashCode;
  }

  public static PhraseTreePath of(final Iterable<UUID> path) {
    checkNotNull(path, "path");

    final List<UUID> orderedPath = new ArrayList<>();
    final TreeSet<UUID> identitySet = new TreeSet<>();

    for (UUID uuid : path) {
      orderedPath.add(uuid);
      identitySet.add(uuid);
    }

    final int hashCode = Arrays.hashCode(identitySet.toArray());

    return new PhraseTreePath(
        ImmutableList.copyOf(orderedPath),
        ImmutableSortedSet.copyOf(identitySet),
        hashCode
    );

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
    return obj instanceof PhraseTreePath
        &&
        getIdentitySet().size() == ((PhraseTreePath) obj).getIdentitySet().size()
        &&
        Sets.intersection(getIdentitySet(), ((PhraseTreePath) obj).getIdentitySet())
            .size() ==
            getIdentitySet().size();
  }

  public boolean divergesFrom(final PhraseTreePath phraseTreePath) {
    checkNotNull(phraseTreePath, "phraseTreePath");

    return !listsMatch(getOrderedPath(), phraseTreePath.getOrderedPath());
  }

  private boolean listsMatch(ImmutableList<UUID> orderedPath1, ImmutableList<UUID> orderedPath2) {
    checkNotNull(orderedPath1, "orderedPath1");
    checkNotNull(orderedPath2, "orderedPath2");

    if (orderedPath1.size() != orderedPath2.size()) {
      return false;
    }

    for (int index = 0; index < orderedPath1.size(); index++) {
      if (!orderedPath1.get(index).equals(orderedPath2.get(index))) {
        return false;
      }
    }

    return true;
  }

  public List<PhraseTreePath> componentize(int maxComponentLength) {
    if (identitySet.isEmpty() || maxComponentLength < 1) {
      return ImmutableList.of();
    }

    int maxLength =
        maxComponentLength > identitySet.size() ? identitySet.size() : maxComponentLength;

    final ImmutableList<UUID> identityElements = identitySet.asList();

    final List<PhraseTreePath> results = new ArrayList<>();

    for (int currentLength = 1; currentLength <= maxLength; currentLength++) {

      final CombinationGenerator combinationGenerator = new CombinationGenerator(
          identityElements.size(),
          currentLength
      );

      List<UUID> currentPath;

      while (combinationGenerator.hasMore()) {
        currentPath = new ArrayList<>();

        final int[] indices = combinationGenerator.getNext();

        for(int index = 0; index < indices.length; index++){
          currentPath.add(identityElements.get(indices[index]));
        }

        results.add(PhraseTreePath.of(currentPath));
      }

    }

    return results;
  }

}
