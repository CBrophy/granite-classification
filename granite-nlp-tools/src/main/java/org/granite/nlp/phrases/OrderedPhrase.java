package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.granite.collections.ListTools;

/**
 * User: cbrophy
 * Date: 5/30/17
 * Time: 10:10 AM
 */
public class OrderedPhrase extends Phrase {

  public OrderedPhrase(
      ImmutableList<UUID> orderedPath,
      ImmutableSortedSet<UUID> identitySet,
      int hashCode) {
    super(orderedPath, identitySet, hashCode);
  }

  public static OrderedPhrase of(final Phrase phrase) {
    checkNotNull(phrase, "phrase");

    return of(phrase.getOrderedPath());
  }

  public static OrderedPhrase of(final Iterable<UUID> path) {
    checkNotNull(path, "path");

    final List<UUID> orderedPath = new ArrayList<>();
    final TreeSet<UUID> identitySet = new TreeSet<>();

    for (UUID uuid : path) {
      orderedPath.add(uuid);
      identitySet.add(uuid);
    }

    // Ordered phrase hashcode is derived from the ordered path
    // rather than the identity set
    final int hashCode = Arrays.hashCode(orderedPath.toArray());

    return new OrderedPhrase(
        ImmutableList.copyOf(orderedPath),
        ImmutableSortedSet.copyOf(identitySet),
        hashCode
    );

  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OrderedPhrase
        && ListTools.listsMatch(
        getOrderedPath(),
        ((Phrase) obj).getOrderedPath()
    );
  }

  @Override
  protected Phrase create(Iterable<UUID> path) {
    return of(path);
  }

  @Override
  public boolean isComponentOf(Phrase phrase) {
    checkNotNull(phrase, "phrase");

    if(getOrderedPath().size() > phrase.getOrderedPath().size()) {
      return false;
    }

    int currentIndex = 0;

    for (UUID uuid : phrase
        .getOrderedPath()) {

      if(getIdentitySet().contains(uuid)){

        if(getOrderedPath().get(currentIndex).equals(uuid)){

          currentIndex++;

        } else {

          return false;

        }

      }

      if(currentIndex >= getOrderedPath().size()){
        // Found all phrase parts in order
        return true;
      }
    }

    return false;

  }

  public static OrderedPhrase extractOrderedComponent(
      final IdentityPhrase component,
      final Phrase container
      ) {
    checkNotNull(component, "component");
    checkNotNull(container, "container");

    if(!component.isComponentOf(container)){
      return null;
    }

    if(component.getIdentitySet().size() == 1){
      return OrderedPhrase.of(component);
    }

    final List<UUID> orderedPath = container
        .getOrderedPath()
        .stream()
        .filter(component.getIdentitySet()::contains)
        .collect(Collectors.toList());

    return OrderedPhrase.of(orderedPath);
  }

}
