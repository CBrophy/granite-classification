package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.TextUtils;
import org.granite.io.FileTools;
import org.granite.log.LogTools;
import org.granite.math.MathTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class BasicClassifier {

    private HashSet<String> stopWords = new HashSet<>();
    private HashSet<String> trainingSetStopWords = new HashSet<>();
    private TreeMap<String, Integer> trainingSetWordFrequencies = new TreeMap<>();
    private int trainingSetWordCount = 0;

    BasicClassifier() {
    }

    private final Splitter whiteSpaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    public HashMap<String, Integer> textToFilteredWords(final String text) {
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

    public HashSet<String> getTrainingSetStopWords() {
        return trainingSetStopWords;
    }

    public int getTrainingSetWordCount() {
        return trainingSetWordCount;
    }

    public TreeMap<String, Integer> getTrainingSetWordFrequencies() {
        return trainingSetWordFrequencies;
    }

    private void filterStopWords(final List<String> words) {
        final int startIndex = words.size() - 1;

        for (int index = startIndex; index >= 0; index--) {
            final String word = words.get(index);
            if (stopWords.contains(word) || trainingSetStopWords.contains(word)) {
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

        train(filterTrainingSet(trainingSet));
    }

    protected ImmutableMap<Integer, TrainingText> filterTrainingSet(final ImmutableMap<Integer, TrainingText> trainingSet) {

        trainStopWords(trainingSet);

        final ImmutableMap.Builder<Integer, TrainingText> builder = ImmutableMap.builder();

        for (Map.Entry<Integer, TrainingText> trainingSetEntry : trainingSet.entrySet()) {

            final int lineNumber = trainingSetEntry.getKey();

            final TrainingText trainingText = trainingSetEntry.getValue();

            // The stop word list provides the first filtration
            final HashMap<String, Integer> trainingLineWordFrequency = textToFilteredWords(trainingText.getText());

            if (trainingLineWordFrequency.isEmpty()) {
                LogTools.info("Training text on line {0} filtered entirely by trained stop word list", String.valueOf(lineNumber));
                continue;
            }

            for (Map.Entry<String, Integer> lineWordFrequencyEntry : trainingLineWordFrequency.entrySet()) {

                final String word = lineWordFrequencyEntry.getKey();

                final int frequency = lineWordFrequencyEntry.getValue();

                trainingText.getWordFrequencies().put(word, frequency);

                final int currentTrainingSetFrequency = trainingSetWordFrequencies.getOrDefault(word, 0);

                trainingSetWordFrequencies.put(word, currentTrainingSetFrequency + frequency);

                trainingSetWordCount += frequency;
            }

            builder.put(lineNumber, trainingText);
        }

        return builder.build();
    }

    private void trainStopWords(final ImmutableMap<Integer, TrainingText> trainingSet){
        final Joiner csv = Joiner.on(',').skipNulls();
        final TreeSet<String> highlyProbableWords = new TreeSet<>();
        final TreeMap<Integer, Integer> wordProbabilityHistogram = new TreeMap<>();

        final HashMap<String, Integer> initialWordFrequencies = new HashMap<>();
        int initialTotalWordCount = 0;

        // The first pass counts the number of training lines the words arrive in
        // And all
        for (Map.Entry<Integer, TrainingText> trainingSetEntry : trainingSet.entrySet()) {

            final int lineNumber = trainingSetEntry.getKey();

            final TrainingText trainingText = trainingSetEntry.getValue();

            // The stop word list provides the first filtration
            final HashMap<String, Integer> trainingLineWordFrequency = textToFilteredWords(trainingText.getText());

            if (trainingLineWordFrequency.isEmpty()) {
                LogTools.info("Training text on line {0} filtered entirely by known stop word list", String.valueOf(lineNumber));
                continue;
            }

            for (String word : trainingLineWordFrequency.keySet()) {

                final int lineWordfrequency = trainingLineWordFrequency.get(word);

                final int currentTrainingSetWordFrequency = initialWordFrequencies.getOrDefault(word, 0);

                initialWordFrequencies.put(word, currentTrainingSetWordFrequency + lineWordfrequency);

                initialTotalWordCount += lineWordfrequency;

            }

        }

        final HashMap<String, Double> wordProbabilities = new HashMap<>();
        double totalProbability = 0.0;

        for (String word : initialWordFrequencies.keySet()) {

            final double wordProbability = (double) initialWordFrequencies.get(word) / (double) initialTotalWordCount;

            wordProbabilities.put(word, wordProbability);

            totalProbability += wordProbability;
        }

        final double mean = totalProbability / (double) wordProbabilities.size();

        double variance = 0.0;

        for (Double probability : wordProbabilities.values()) {
            variance += Math.pow(mean - probability, 2);
        }

        variance = variance / ((double) wordProbabilities.size() - 1.0);

        final double standardDeviation = Math.sqrt(variance);

        LogTools.info("Word probability mean: {0}, standard dev: {1}", String.valueOf(mean), String.valueOf(standardDeviation));

        for (Map.Entry<String, Double> wordProbabilityEntry : wordProbabilities.entrySet()) {

            final String word = wordProbabilityEntry.getKey();
            final double wordProbability = wordProbabilityEntry.getValue();

            double difference = mean - wordProbability;

            int distanceFromMean = (int) (difference < 0.0 ?
                    Math.ceil(difference / standardDeviation) :
                    Math.floor(difference / standardDeviation));

            if(distanceFromMean == 0){
                getTrainingSetStopWords().add(word);
                highlyProbableWords.add(word);
            }

            wordProbabilityHistogram.put(distanceFromMean,  wordProbabilityHistogram.getOrDefault(distanceFromMean, 0) + 1);
        }

        LogTools.info("Histogram of word probability stdevs from the mean:");

        wordProbabilityHistogram
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .forEach(System.out::println);

        LogTools.info("{0} highly probable words in the training set add to stop words: {1}", String.valueOf(highlyProbableWords.size()), csv.join(highlyProbableWords));

    }

    public abstract void train(final ImmutableMap<Integer, TrainingText> trainingSet);
}
