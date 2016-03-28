package org.granite.classification;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.LuceneWordBagger;
import org.granite.classification.utils.TextUtils;
import org.granite.io.FileTools;
import org.granite.log.LogTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
    private TreeSet<TrainingText> trainingTexts = new TreeSet<>();
    private TreeMap<String, TreeMap<String, Integer>> trainingSetClassificationWordCounts = new TreeMap<>();
    private TreeMap<String, TreeMap<String, Integer>> trainingSetWordClassificationCounts = new TreeMap<>();
    private TreeMap<String, Integer> trainingSetClassificationTotalWordCounts = new TreeMap<>();
    private TreeMap<String, Integer> trainingSetClassificationCounts = new TreeMap<>();
    private int trainingSetCount = 0;

    BasicClassifier() {
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

    private void loadStopWordFile(final File stopWordFile) {
        if (stopWordFile == null) return;

        if (!FileTools.fileExistsAndCanRead(stopWordFile)) {
            LogTools.info("Cannot read from stop word file {0}", stopWordFile.getAbsolutePath());
            return;
        }

        try {
            final List<String> lines = Files.readLines(stopWordFile, Charset.defaultCharset());

            for (String line : lines) {
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;

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

        transformTrainingSet(trainingSet);

        checkState(!this.getTrainingTexts().isEmpty(), "No training set loaded after restructuring");
        checkState(!this.getTrainingSetClassificationCounts().isEmpty(), "No classifications found in training set");
        checkState(!this.getTrainingSetClassificationWordCounts().isEmpty(), "No classification word counts found in training set");
        checkState(!this.getTrainingSetWordClassificationCounts().isEmpty(), "No word classification counts found in training set");
        checkState(!this.getTrainingSetClassificationTotalWordCounts().isEmpty(), "No classification total word counts found in training set");
        checkState(this.getTrainingSetCount() > 0, "Training set count is zero");

        train();
    }

    private void transformTrainingSet(final ImmutableMap<Integer, TrainingText> trainingSet) {
        if (getStopWords().isEmpty()) {
            LogTools.warn("Training without stop words!");
        }

        final Joiner csv = Joiner.on(',').skipNulls();

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger(this.getStopWords());

        this.trainingSetCount = trainingSet.size();

        for (TrainingText trainingText : trainingSet.values()) {

            final ImmutableSet<String> wordBag = luceneWordBagger.createWordBag(trainingText.getText());

            this.trainingTexts.add(trainingText);

            for (String word : wordBag) {
                // No matter what, add it to the original set-row's word bag
                trainingText.getWordBag().add(word);

                final TreeMap<String, Integer> classificationCounts = getNestedMap(word, this.trainingSetWordClassificationCounts);

                for (String classification : trainingText.getClassifications()) {

                    // Increment the classification counter for this word in the overall training set
                    TextUtils.updateFrequencyMap(classification, classificationCounts, 1);

                }

            }

            trainingText
                    .getClassifications()
                    .forEach(classification -> TextUtils.updateFrequencyMap(classification, this.trainingSetClassificationCounts, 1));

        }

        // Look for words that only exist once in each classification and remove those
        final HashSet<String> wordsToDiscard = new HashSet<>();

        final HashMultimap<String, String> wordClassificationDiscards = HashMultimap.create();

        for (Map.Entry<String, TreeMap<String, Integer>> wordEntry : this.trainingSetWordClassificationCounts.entrySet()) {

            final String word = wordEntry.getKey();

            wordEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .filter(categoryEntry -> categoryEntry.getValue() == 1)
                    .forEach(entry -> wordClassificationDiscards.put(word, entry.getKey()));


            final Set<String> classificationsToDiscard = wordClassificationDiscards.get(word);

            if (classificationsToDiscard != null) {
                classificationsToDiscard
                        .forEach(wordEntry.getValue()::remove);
            }

            if (wordEntry.getValue().isEmpty())
                wordsToDiscard.add(wordEntry.getKey());

        }

        wordsToDiscard
                .forEach(this.trainingSetWordClassificationCounts::remove);

        LogTools.info("{0} words discarded completely: {1}", String.valueOf(wordsToDiscard.size()), csv.join(wordsToDiscard));

        for (String word : wordClassificationDiscards.keySet()) {
            LogTools.info("Word {0} discarded from classifications: {1}", word, csv.join(wordClassificationDiscards.get(word)));
        }


        // Now populate the other statistics

        for (Map.Entry<String, TreeMap<String, Integer>> wordEntry : this.trainingSetWordClassificationCounts.entrySet()) {
            final String word = wordEntry.getKey();

            for (Map.Entry<String, Integer> classificationEntry : wordEntry.getValue().entrySet()) {

                final String classification = classificationEntry.getKey();

                final int classificationWordCount = classificationEntry.getValue();

                final TreeMap<String, Integer> wordCounts = getNestedMap(classification, this.trainingSetClassificationWordCounts);

                TextUtils.updateFrequencyMap(word, wordCounts, classificationWordCount);

                TextUtils.updateFrequencyMap(classification, this.trainingSetClassificationTotalWordCounts, classificationWordCount);
            }

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
