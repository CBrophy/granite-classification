package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.TextUtils;
import org.granite.io.FileTools;
import org.granite.log.LogTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class BasicClassifier {

    private HashSet<String> stopWords = new HashSet<>();

    private final Splitter whiteSpaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    public HashMap<String, Integer> textToWords(final String text) {
        final List<String> wordList = new ArrayList<>(whiteSpaceSplitter
                .splitToList(TextUtils.cleanText(text)));

        filterStopWords(wordList);

        if (wordList.isEmpty()) {
            LogTools.info("No significant words in text: {0}", text);
            return new HashMap<>();
        }

        final HashMap<String, Integer> allWords = new HashMap<>();

        TextUtils.updateFrequencyMap(wordList, allWords);

        return allWords;
    }

    public static ImmutableMap<Integer, TrainingText> loadTrainingText(final File trainingTextFile) {
        checkNotNull(trainingTextFile, "trainingFile");
        checkArgument(FileTools.fileExistsAndCanRead(trainingTextFile), "cannot read training file: %s", trainingTextFile.getAbsolutePath());

        final ObjectMapper objectMapper = new ObjectMapper();

        final ImmutableMap.Builder<Integer, TrainingText> result = ImmutableMap.builder();

        try {
            final List<String> lines = Files.readLines(trainingTextFile, Charset.defaultCharset());

            int lineCount = 0;

            for (String line : lines) {
                lineCount++;
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;

                TrainingText trainingText = null;

                try {
                    trainingText = objectMapper.readValue(trimmed, TrainingText.class);
                } catch (IOException e) {
                    LogTools.error("Failed to deserialize json on line [{0}]: {1}", String.valueOf(lineCount), line);
                    throw Throwables.propagate(e);
                }

                result.put(lineCount, trainingText);

            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return result.build();
    }

    public HashSet<String> getStopWords() {
        return stopWords;
    }

    private void filterStopWords(final List<String> words) {
        final int startIndex = words.size() - 1;

        for (int index = startIndex; index >= 0; index--) {
            if (stopWords.contains(words.get(index))) {
                words.remove(index);
            }
        }
    }

    private void loadStopWordFile(final File stopWordFile) {
        stopWords.clear();

        if (stopWordFile == null) return;

        if (!FileTools.fileExistsAndCanRead(stopWordFile)) {
            LogTools.info("Cannot read from stop word file {0}", stopWordFile.getAbsolutePath());
            return;
        }

        try {
            final List<String> lines = Files.readLines(stopWordFile, Charset.defaultCharset());

            for (String line : lines) {
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#")) continue;

                stopWords.add(trimmed);
            }

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        LogTools.info("Loaded {0} stop words", String.valueOf(stopWords.size()));
    }

    public void train(final File trainingFile, final File stopWordFile) {
        checkNotNull(trainingFile, "trainingFile");

        loadStopWordFile(stopWordFile);

        final ImmutableMap<Integer, TrainingText> trainingSet = loadTrainingText(trainingFile);

        checkNotNull(trainingSet, "trainingSet");
        checkState(!trainingSet.isEmpty(), "trainingSet is empty");

        train(trainingSet);
    }

    public abstract void train(final ImmutableMap<Integer, TrainingText> trainingSet);
}
