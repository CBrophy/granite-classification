package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class BasicClassifier {

    private HashSet<String> stopWords = new HashSet<>();
    private TreeSet<TrainingText> trainingTexts = new TreeSet<>();
    private TreeMap<String, Integer> trainingSetWordCounts = new TreeMap<>();
    private TreeMap<String, TreeMap<String, Integer>> trainingSetClassificationWordCounts = new TreeMap<>();
    private TreeMap<String, TreeMap<String, Integer>> trainingSetWordClassificationCounts = new TreeMap<>();
    private TreeMap<String, Integer> trainingSetClassificationTotalWordCounts = new TreeMap<>();
    private TreeMap<String, Integer> trainingSetClassificationCounts = new TreeMap<>();
    private int trainingSetWordCount = 0;
    private int trainingSetCount = 0;

    BasicClassifier() {
    }

    private final Splitter whiteSpaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    public ImmutableSet<String> textToFilteredWordBag(final String text) {
        final List<String> wordList = new ArrayList<>(whiteSpaceSplitter
                .splitToList(TextUtils.cleanText(text)));

        filterStopWords(wordList);

        if (wordList.isEmpty()) {
            LogTools.info("No significant words in text: {0}", text);
            return ImmutableSet.of();
        }

        return ImmutableSet.copyOf(wordList);
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

    public int getTrainingSetWordCount() {
        return trainingSetWordCount;
    }

    public TreeMap<String, Integer> getTrainingSetWordCounts() {
        return trainingSetWordCounts;
    }

    public TreeMap<String, TreeMap<String, Integer>> getTrainingSetClassificationWordCounts() {
        return trainingSetClassificationWordCounts;
    }

    public TreeMap<String, TreeMap<String, Integer>> getTrainingSetWordClassificationCounts() {
        return trainingSetWordClassificationCounts;
    }

    public TreeMap<String, Integer> getTrainingSetClassificationCounts() {
        return trainingSetClassificationCounts;
    }

    public TreeSet<TrainingText> getTrainingTexts() {
        return trainingTexts;
    }

    public TreeMap<String, Integer> getTrainingSetClassificationTotalWordCounts() {
        return trainingSetClassificationTotalWordCounts;
    }

    public int getTrainingSetCount() {
        return trainingSetCount;
    }

    private void filterStopWords(final List<String> words) {
        final int startIndex = words.size() - 1;

        for (int index = startIndex; index >= 0; index--) {
            final String word = words.get(index);
            if (stopWords.contains(word)) {
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

        restructureTrainingSet(trainingSet);

        checkState(!this.getTrainingTexts().isEmpty(), "No training set loaded after restructuring");
        checkState(!this.getTrainingSetWordCounts().isEmpty(), "No words found in training set");
        checkState(!this.getTrainingSetClassificationCounts().isEmpty(), "No classifications found in training set");
        checkState(!this.getTrainingSetClassificationWordCounts().isEmpty(), "No classification word counts found in training set");
        checkState(!this.getTrainingSetWordClassificationCounts().isEmpty(), "No word classification counts found in training set");
        checkState(!this.getTrainingSetClassificationTotalWordCounts().isEmpty(), "No classification total word counts found in training set");
        checkState(this.getTrainingSetWordCount() > 0, "Training set word count is zero");
        checkState(this.getTrainingSetCount() > 0, "Training set count is zero");

        train();
    }

    private void restructureTrainingSet(final ImmutableMap<Integer, TrainingText> trainingSet) {
        this.trainingSetCount = trainingSet.size();

        for (TrainingText trainingText : trainingSet.values()) {

            final ImmutableSet<String> wordBag = textToFilteredWordBag(trainingText.getText());

            this.trainingTexts.add(trainingText);

            for (String word : wordBag) {

                trainingText.getWordBag().add(word);

                TextUtils.updateFrequencyMap(word, this.trainingSetWordCounts, 1);

                this.trainingSetWordCount++;

                final TreeMap<String, Integer> classificationCounts = getNestedMap(word, this.trainingSetWordClassificationCounts);

                for (String classification : trainingText.getClassifications()) {

                    TextUtils.updateFrequencyMap(classification, classificationCounts, 1);

                    TextUtils.updateFrequencyMap(classification, this.trainingSetClassificationTotalWordCounts, 1);

                    final TreeMap<String, Integer> wordCounts = getNestedMap(classification, this.trainingSetClassificationWordCounts);

                    TextUtils.updateFrequencyMap(word, wordCounts, 1);

                }

            }

            trainingText
                    .getClassifications()
                    .forEach(classification -> TextUtils.updateFrequencyMap(classification, this.trainingSetClassificationCounts, 1));

        }

    }

    protected static <CK, NK, NV> TreeMap<NK, NV> getNestedMap(final CK key, final TreeMap<CK, TreeMap<NK, NV>> containerMap) {
        checkNotNull(key, "key");
        checkNotNull(containerMap, "containerMap");

        TreeMap<NK, NV> nestedMap = containerMap.get(key);

        if (nestedMap == null) {
            nestedMap = new TreeMap<>();
            containerMap.put(key, nestedMap);
        }

        return nestedMap;
    }

    public abstract void train();
}
