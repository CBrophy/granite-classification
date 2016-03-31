package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.WordBagger;

import java.io.File;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class SpaceSplitWordBagger extends WordBagger {

    private final Splitter whitespaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    public SpaceSplitWordBagger(File stopWordFile) {
        super(stopWordFile);
    }

    @Override
    public ImmutableSet<String> generateWordBag(String text) {
        checkNotNull(text, "text");

        return ImmutableSet.copyOf(
                whitespaceSplitter
                        .splitToList(text)
                        .stream()
                        .filter(word -> !getStopWords().contains(word))
                        .collect(Collectors.toList())
        );
    }
}
