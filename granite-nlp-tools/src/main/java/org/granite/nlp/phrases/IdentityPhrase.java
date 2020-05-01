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

/**
 * User: cbrophy Date: 5/30/17 Time: 11:45 AM
 */
public class IdentityPhrase extends Phrase {

  protected IdentityPhrase(
      ImmutableList<UUID> orderedPath,
      ImmutableSortedSet<UUID> identitySet,
      int hashCode) {
    super(orderedPath, identitySet, hashCode);
  }

  public static IdentityPhrase of(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    return of(phrase.getOrderedPath());
  }

  public static IdentityPhrase of(final Iterable<UUID> path) {
    checkNotNull(path, "path");

    final List<UUID> orderedPath = new ArrayList<>();
    final TreeSet<UUID> identitySet = new TreeSet<>();

    for (UUID uuid : path) {
      orderedPath.add(uuid);
      identitySet.add(uuid);
    }

    final int hashCode = Arrays.hashCode(identitySet.toArray());

    return new IdentityPhrase(
        ImmutableList.copyOf(orderedPath),
        ImmutableSortedSet.copyOf(identitySet),
        hashCode
    );

  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj)
        && Sets.intersection(getIdentitySet(), ((Phrase) obj).getIdentitySet())
        .size() ==
        getIdentitySet().size();
  }

  @Override
  protected Phrase create(Iterable<UUID> path) {
    return of(path);
  }

  @Override
  public boolean isComponentOf(Phrase phrase) {
    checkNotNull(phrase, "phrase");

    if (getIdentitySet().size() > phrase.getIdentitySet().size()) {
      return false;
    }

    return Sets.intersection(
        getIdentitySet(),
        phrase.getIdentitySet())
        .size() == getIdentitySet().size();
  }

}
