package org.granite.classification.model;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.classification.utils.MapUtils;
import org.granite.io.FileTools;
import org.granite.log.LogTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TrainingSet {
    private final ImmutableMap<Integer, TrainingText> trainingTexts;
    private final int minimumFrequency;
    private ImmutableMap<String, Double> classificationLineCounts;
    private ImmutableMap<String, ImmutableMap<String, Double>> wordClassificationCounts;
    private ImmutableMap<String, ImmutableMap<String, Double>> classificationWordCounts;
    private ImmutableMap<String, Double> classificationTotalWordCounts;
    private ImmutableMap<String, Double> wordTotalCounts;
    private final double trainingSetSize;
    private double trainingSetWordCount;
    private final WordBagger wordBagger;

    public TrainingSet(final File trainingTextFile,
                       final WordBagger wordBagger,
                       final int minimumFrequency) {
        this(loadTrainingText(trainingTextFile, wordBagger), wordBagger, minimumFrequency);
    }

    public TrainingSet(final ImmutableMap<Integer, TrainingText> trainingTexts, final WordBagger wordBagger, final int minimumFrequency) {
        this.trainingTexts = checkNotNull(trainingTexts, "trainingTexts");
        this.wordBagger = checkNotNull(wordBagger, "wordBagger");

        checkArgument(trainingTexts.size() > 0, "trainingTexts map is empty");

        this.trainingSetSize = (double) trainingTexts.size();

        this.minimumFrequency = minimumFrequency;

        generateTrainingTextStats();

        checkState(classificationLineCounts != null && !classificationLineCounts.isEmpty(), "classificationLineCounts is not populated");
        checkState(wordClassificationCounts != null && !wordClassificationCounts.isEmpty(), "wordClassificationCounts is not populated");
        checkState(classificationWordCounts != null && !classificationWordCounts.isEmpty(), "classificationWordCounts is not populated");
        checkState(classificationTotalWordCounts != null && !classificationTotalWordCounts.isEmpty(), "classificationTotalWordCounts is not populated");
        checkState(wordTotalCounts != null && !wordTotalCounts.isEmpty(), "wordTotalCounts is not populated");
        checkState(trainingSetWordCount > 0.0, "No words in training set");
    }

    private void generateTrainingTextStats() {

        final HashMap<String, Double> classificationLineCounts = new HashMap<>();

        final HashMap<String, HashMap<String, Double>> wordClassificationCounts = new HashMap<>();

        final HashMap<String, Double> classificationTotalWordCounts = new HashMap<>();

        // First pass counts raw words and classifications
        for (TrainingText trainingText : trainingTexts.values()) {

            trainingText
                    .getClassifications()
                    .forEach(classification -> {

                        // Set the number of training set rows per classification

                        classificationLineCounts
                                .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                        // Count the number of occurrences per word per classification
                        // and the total number of words per classification

                        trainingText
                                .getWordBag()
                                .forEach(word -> {

                                    wordClassificationCounts
                                            .computeIfAbsent(word, key -> new HashMap<>())
                                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                                    classificationTotalWordCounts
                                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);


                                });

                    });

        }

        // Second pass to create word filters based on word occurrence

        final HashMultimap<String, String> wordClassificationFilters = HashMultimap.create();

        final HashSet<String> wordFilter = new HashSet<>();

        for (Map.Entry<String, HashMap<String, Double>> wordEntry : wordClassificationCounts.entrySet()) {

            final String word = wordEntry.getKey();

            for (Map.Entry<String, Double> classificationEntry : wordEntry.getValue().entrySet()) {

                final String classification = classificationEntry.getKey();

                if (classificationEntry.getValue() < minimumFrequency) {
                    wordClassificationFilters.put(word, classification);
                }
            }

            // If the word is filtered from all classifications, add it to the whole word filter
            if (classificationLineCounts.size() == wordClassificationFilters.get(word).size()) {
                wordFilter.add(word);
            }

        }

        LogTools.info("Filtering {0} words from specific classifications due to too low frequency", String.valueOf(wordClassificationFilters.size()));

        LogTools.info("Filtering {0} from all classifications due to too low frequency", String.valueOf(wordFilter.size()));

        // Third pass applies the word filters and computes the final frequencies

        final HashMap<String, Double> wordTotalCounts = new HashMap<>();
        final HashMap<String, HashMap<String, Double>> classificationWordCounts = new HashMap<>();

        // Re-count words per classification after filter is applied
        classificationTotalWordCounts.clear();
        wordClassificationCounts.clear();

        for (final TrainingText trainingText : trainingTexts.values()) {

            trainingText
                    .getWordBag()
                    .stream()
                    .filter(word -> !wordFilter.contains(word)) // apply primary filter
                    .forEach(word -> {

                        wordTotalCounts
                                .merge(word, 1.0, (v1, v2) -> v1 + v2);

                        final HashMap<String, Double> wordClassificationMap = wordClassificationCounts
                                .computeIfAbsent(word, key -> new HashMap<>());

                        trainingText
                                .getClassifications()
                                .stream()
                                .filter(classification -> !wordClassificationFilters.containsEntry(word, classification))
                                .forEach(classification -> {

                                    // The total number of words in the classification
                                    classificationTotalWordCounts
                                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                                    // The number of times the word appears with the classification
                                    classificationWordCounts
                                            .computeIfAbsent(classification, key -> new HashMap<>())
                                            .merge(word, 1.0, (v1, v2) -> v1 + v2);

                                    // The number of times the classification appears with this word
                                    wordClassificationMap
                                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                                });

                    });
        }

        this.classificationWordCounts = MapUtils.buildImmutableCopy(classificationWordCounts);
        this.wordClassificationCounts = MapUtils.buildImmutableCopy(wordClassificationCounts);
        this.classificationLineCounts = ImmutableMap.copyOf(classificationLineCounts);
        this.classificationTotalWordCounts = ImmutableMap.copyOf(classificationTotalWordCounts);
        this.wordTotalCounts = ImmutableMap.copyOf(wordTotalCounts);

        for (Double count : this.wordTotalCounts
                .values()) {
            this.trainingSetWordCount += count;
        }

    }

    public ImmutableMap<String, Double> getClassificationLineCounts() {
        return classificationLineCounts;
    }

    public ImmutableMap<String, ImmutableMap<String, Double>> getWordClassificationCounts() {
        return wordClassificationCounts;
    }

    public ImmutableMap<String, ImmutableMap<String, Double>> getClassificationWordCounts() {
        return classificationWordCounts;
    }

    public ImmutableMap<String, Double> getClassificationTotalWordCounts() {
        return classificationTotalWordCounts;
    }

    public ImmutableMap<String, Double> getWordTotalCounts() {
        return wordTotalCounts;
    }

    public double getTrainingSetWordCount() {
        return trainingSetWordCount;
    }

    public double getTrainingSetSize() {
        return trainingSetSize;
    }

    public ImmutableMap<Integer, TrainingText> getTrainingTexts() {
        return trainingTexts;
    }

    public WordBagger getWordBagger() {
        return wordBagger;
    }

    public double getMinimumFrequency() {
        return minimumFrequency;
    }

    public static ImmutableMap<Integer, TrainingText> loadTrainingText(final File trainingTextFile,
                                                                       final WordBagger wordBagger) {
        checkNotNull(trainingTextFile, "trainingFile");
        checkNotNull(wordBagger, "wordBagger");
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

                final ImmutableSet<String> wordBag = wordBagger.generateWordBag(trainingText.getText());

                checkState(!wordBag.isEmpty(), "training text filtered entirely as stop words: %s", line);

                trainingText
                        .getWordBag()
                        .addAll(wordBag);

            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return result.build();
    }
}
