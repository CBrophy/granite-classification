package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.base.StringTools;
import org.granite.classification.model.Classification;
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
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Classifier {

    private HashSet<String> stopWords = new HashSet<>();
    private TreeMap<String, Classification> classificationMap = new TreeMap<>();
    private TreeMap<String, String> textPatchMap = new TreeMap<>();
    private TreeMap<String, Integer> wordFrequencyMap = new TreeMap<>();
    private int trainingTextCount = 0;
    private final double noiseProbabilityMaximum;
    final Splitter whiteSpaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

    Classifier(double noiseProbabilityMaximum) {
        this.noiseProbabilityMaximum = noiseProbabilityMaximum;
    }

    public ImmutableMap<String, Double> classify(final String text) {
        if (StringTools.isNullOrEmpty(text)) return ImmutableMap.of();

        final HashSet<String> allWords = textToWords(text);

        if (allWords.isEmpty()) {
            return ImmutableMap.of();
        }

        final ImmutableMap.Builder<String, Double> result = new ImmutableMap.Builder<>();

        for (Classification classification : classificationMap.values()) {

            if (classification.getWordProbabilityMap().isEmpty()) {
                continue;
            }

            final double score = classification.score(allWords);

            if (score > Double.MIN_VALUE) {
                result.put(classification.getLabel(), score);
            }

        }

        return result.build();
    }

    private String applyTextPatches(final String text) {

        if (StringTools.isNullOrEmpty(text)) return text;

        String result = text;

        for (Map.Entry<String, String> textPatchEntry : textPatchMap.entrySet()) {
            result = result.replace(textPatchEntry.getKey(), textPatchEntry.getValue());
        }

        return result;

    }

    public static Classifier train(final File trainingFile,
                                   final File stopWordFile,
                                   final File textPatchFile,
                                   final double noiseProbabilityMaximum) {
        checkNotNull(trainingFile, "trainingFile");

        final Classifier result = new Classifier(noiseProbabilityMaximum);

        result.loadStopWordFile(stopWordFile);
        result.loadTextPatchFile(textPatchFile);
        result.loadTrainingFile(trainingFile);

        result.calculateProbabilities();

        return result;
    }

    private void loadTextPatchFile(final File textPatchFile) {
        this.textPatchMap.clear();

        if (textPatchFile == null) return;

        if (!FileTools.fileExistsAndCanRead(textPatchFile)) {
            LogTools.info("Cannot read from text patch file {0}", textPatchFile.getAbsolutePath());
            return;
        }

        int lineCount = 0;

        try {

            final Splitter tabSplitter = Splitter.on('\t').trimResults();

            final List<String> lines = Files.readLines(textPatchFile, Charset.defaultCharset());

            for (String line : lines) {
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#")) continue;

                lineCount++;

                final List<String> patchParts = tabSplitter.splitToList(trimmed);

                if (patchParts.size() != 2) {
                    LogTools.warn("Skipping text patch {0} due to too many or too few tab chars", line);
                    continue;
                }

                textPatchMap.put(patchParts.get(0), patchParts.get(1));
            }

            LogTools.info("Loaded {0} lines from text patch file", String.valueOf(lineCount));

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

    }

    HashSet<String> textToWords(final String text) {
        final List<String> wordList = new ArrayList<>(whiteSpaceSplitter
                .splitToList(TextUtils.cleanText(text)));

        filterStopWords(wordList);

        if (wordList.isEmpty()) {
            LogTools.info("No significant words in text: {0}", text);
            return new HashSet<>();
        }

        final HashSet<String> allWords = new HashSet<>();

        allWords.addAll(wordList);

        allWords.addAll(createWordGroups(wordList));

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

    private void loadTrainingFile(final File trainingFile) {

        final ImmutableMap<Integer, TrainingText> trainingTextLineMap = loadTrainingText(trainingFile);

        for (Map.Entry<Integer, TrainingText> trainingTextEntry : trainingTextLineMap.entrySet()) {
            final int lineNumber = trainingTextEntry.getKey();
            final TrainingText trainingText = trainingTextEntry.getValue();


            final HashSet<String> allWords = textToWords(trainingText.getText());

            if (allWords.isEmpty()) {
                LogTools.info("Skipping useless training text on line [{0}]: {1}", String.valueOf(lineNumber), trainingText.getText());
                continue;
            }

            this.trainingTextCount++;

            final HashSet<Classification> sharedClassifications = new HashSet<>();

            TextUtils.updateFrequencyMap(allWords, wordFrequencyMap);

            for (String label : trainingText.getClassifications()) {

                Classification classification = classificationMap.get(label);

                if (classification == null) {
                    classification = new Classification(label);
                    classificationMap.put(label, classification);
                }

                sharedClassifications.add(classification);

                classification.setClassificationFrequency(classification.getClassificationFrequency() + 1);

                classification.addAll(allWords);

            }

            for (Classification classification : sharedClassifications) {
                sharedClassifications
                        .stream()
                        .filter(sharedClassification -> !sharedClassification.equals(classification))
                        .forEach(classification.getComplimentClassifications()::add);
            }

        }

        LogTools.info("Loaded {0} lines from training text", String.valueOf(trainingTextLineMap.size()));


    }

    private void calculateProbabilities() {
        checkState(trainingTextCount > 0, "No training text loaded");

        for (Classification classification : classificationMap.values()) {

            classification.setLikelihood((double) classification.getClassificationFrequency() / (double) getTrainingTextCount());

            LogTools.info("Set classification {0} likelihood to {1}", classification.getLabel(), String.valueOf(classification.getLikelihood()));

            for (Map.Entry<String, Integer> wordFrequencyEntry : classification
                    .getWordFrequencyMap()
                    .entrySet()) {

                final double corpusProbability = (double) wordFrequencyMap.get(wordFrequencyEntry.getKey()) / (double) trainingTextCount;

                if (corpusProbability > noiseProbabilityMaximum) {
                    // The word is found everywhere in the training set - ignore it
                    continue;
                }

                final double classificationProbability = (double) wordFrequencyEntry.getValue() / (double) classification.getClassificationFrequency();

                if ((1.0 - classificationProbability) >= noiseProbabilityMaximum) {
                    // The word is not common enough in this classification, drop as noise
                    continue;
                }

                classification.getWordProbabilityMap().put(wordFrequencyEntry.getKey(), (1.0 - corpusProbability) * classificationProbability);

            }

            if (classification.getWordProbabilityMap().isEmpty()) {
                LogTools.warn("Dropping label {0} due to low training significance. Either add more training data or consider that the label may be too vague.", classification.getLabel());
            }

        }

    }

    private HashSet<String> createWordGroups(final List<String> wordList) {
        final HashSet<String> result = new HashSet<>();

        for (int index = 0; index < wordList.size(); index++) {

            if (index < wordList.size() - 1) {
                result.add(wordList.get(index) + "_" + wordList.get(index + 1));
            }

        }

        return result;
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

    public HashSet<String> getStopWords() {
        return stopWords;
    }

    public TreeMap<String, Classification> getClassificationMap() {
        return classificationMap;
    }

    public TreeMap<String, String> getTextPatchMap() {
        return textPatchMap;
    }

    public TreeMap<String, Integer> getWordFrequencyMap() {
        return wordFrequencyMap;
    }

    public int getTrainingTextCount() {
        return trainingTextCount;
    }
}
