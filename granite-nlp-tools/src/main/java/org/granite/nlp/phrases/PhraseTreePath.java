package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

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

        final int hashCode = Objects.hash(identitySet);

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
}
