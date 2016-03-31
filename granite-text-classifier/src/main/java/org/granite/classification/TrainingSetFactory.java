package org.granite.classification;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.classification.model.TrainingSetFilter;
import org.granite.classification.model.TrainingText;
import org.granite.classification.model.WordBagger;
import org.granite.io.FileTools;
import org.granite.log.LogTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TrainingSetFactory {
    private WordBagger wordBagger;
    private TrainingSetFilter trainingSetFilter;
    private final File trainingTextFile;

    public TrainingSetFactory(final File trainingTextFile, final WordBagger wordBagger) {
        this.trainingTextFile = checkNotNull(trainingTextFile, "trainingTextFile");
        this.wordBagger = checkNotNull(wordBagger, "wordBagger");
    }

    public ImmutableTrainingSet createTrainingSet(){
        final ImmutableMap<Integer, TrainingText> trainingTexts = loadTrainingText(trainingTextFile, wordBagger);
        checkNotNull(trainingTexts, "trainingTexts");
        checkArgument(trainingTexts.size() > 0, "trainingTexts map is empty");

        double trainingSetSize = (double) trainingTexts.size();

        ImmutableSet<String> wordFilter;
        ImmutableMultimap<String, String> wordClassificationFilter;

        if (trainingSetFilter != null) {
            trainingSetFilter.generateFilters(trainingTexts);

            wordFilter = trainingSetFilter.getWordFilter();
            wordClassificationFilter = trainingSetFilter.getWordClassificationFilter();

        } else {

            wordFilter = ImmutableSet.of();
            wordClassificationFilter = ImmutableMultimap.of();

        }

        final HashMap<String, Double> classificationLineCounts = new HashMap<>();

        final HashMap<String, Map<String, Double>> wordClassificationCounts = new HashMap<>();

        final HashMap<String, Double> classificationTotalWordCounts = new HashMap<>();

        final HashMap<String, Double> wordTotalCounts = new HashMap<>();

        final HashMap<String, Map<String, Double>> classificationWordCounts = new HashMap<>();

        // First pass counts raw words and classifications
        for (TrainingText trainingText : trainingTexts.values()) {

            for (String classification : trainingText.getClassifications()) {

                // Set the number of training set rows per classification
                classificationLineCounts
                        .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                final Map<String, Double> wordCounts = classificationWordCounts
                        .computeIfAbsent(classification, key -> new HashMap<>());

                for (String word : trainingText.getWordBag()) {

                    // Skip anything designated by the filter as noise
                    if (wordFilter.contains(word) || wordClassificationFilter.containsEntry(word, classification)) {
                        continue;
                    }
                    // Count the number of occurrences per word per classification
                    // and the total number of words per classification

                    wordClassificationCounts
                            .computeIfAbsent(word, key -> new HashMap<>())
                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                    classificationTotalWordCounts
                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                    // The number of times the word appears with the classification
                    wordCounts
                            .merge(word, 1.0, (v1, v2) -> v1 + v2);
                }

            }

            trainingText
                    .getWordBag()
                    .stream()
                    .filter(word -> !wordFilter.contains(word))
                    .forEach(word -> wordTotalCounts
                            .merge(word, 1.0, (v1, v2) -> v1 + v2));

        }

        double trainingSetWordCount = 0.0;

        for (Double count : wordTotalCounts
                .values()) {
            trainingSetWordCount += count;
        }

        final Sets.SetView<String> classificationsFilteredEntirely = Sets.difference(classificationLineCounts.keySet(), classificationTotalWordCounts.keySet());

        checkState(classificationsFilteredEntirely.isEmpty() , "Classifications have been filtered due to no frequent words: %s", Joiner.on(',').join(classificationsFilteredEntirely));

        return new ImmutableTrainingSet(
                classificationLineCounts,
                wordClassificationCounts,
                classificationWordCounts,
                classificationTotalWordCounts,
                wordTotalCounts,
                trainingSetSize,
                trainingSetWordCount
        );
    }


    public WordBagger getWordBagger() {
        return wordBagger;
    }

    public TrainingSetFilter getTrainingSetFilter() {
        return trainingSetFilter;
    }

    public File getTrainingTextFile() {
        return trainingTextFile;
    }

    public TrainingSetFactory withWordBagger(final WordBagger wordBagger) {
        this.wordBagger = checkNotNull(wordBagger, "wordBagger");
        return this;
    }

    public TrainingSetFactory withTrainingSetFilter(final TrainingSetFilter trainingSetFilter) {
        this.trainingSetFilter = trainingSetFilter;
        return this;
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
