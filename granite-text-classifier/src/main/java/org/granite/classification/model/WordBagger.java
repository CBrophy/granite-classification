package org.granite.classification.model;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import org.granite.io.FileTools;
import org.granite.log.LogTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WordBagger {
    private HashSet<String> stopWords = new HashSet<>();

    public WordBagger(final File stopWordFile){
        this(loadStopWordFile(stopWordFile));
    }

    public WordBagger(final Iterable<String> stopWords){
        checkNotNull(stopWords, "stopWords");

        stopWords
                .forEach(this.stopWords::add);
    }

    public HashSet<String> getStopWords() {
        return stopWords;
    }

    public static ImmutableSet<String> loadStopWordFile(final File stopWordFile) {
        if (stopWordFile == null) return ImmutableSet.of();

        if (!FileTools.fileExistsAndCanRead(stopWordFile)) {
            LogTools.info("Cannot read from stop word file {0}", stopWordFile.getAbsolutePath());
            return ImmutableSet.of();
        }

        final HashSet<String> result = new HashSet<>();

        try {
            final List<String> lines = Files.readLines(stopWordFile, Charset.defaultCharset());

            for (String line : lines) {
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;

                result.add(trimmed);
            }

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        LogTools.info("Loaded {0} stop words", String.valueOf(result.size()));

        return ImmutableSet.copyOf(result);
    }


    public abstract ImmutableSet<String> generateWordBag(final String text);
}
