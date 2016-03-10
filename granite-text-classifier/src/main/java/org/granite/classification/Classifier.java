package org.granite.classification;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Classifier {

    private HashSet<String> stopWords = new HashSet<>();
    private TreeMap<String, Classification> classificationMap = new TreeMap<>();
    private TreeMap<String, String> textPatchMap = new TreeMap<>();
    private int sharedWordFilterThreshold;

    Classifier(){}

    public Classifier(final int sharedWordFilterThreshold) {
        this.sharedWordFilterThreshold = sharedWordFilterThreshold;
    }

    public Map<String, Double> classify(final String text){
        if(StringTools.isNullOrEmpty(text)) return ImmutableMap.of();

        final String cleanText = TextUtils.cleanText(text);

        return ImmutableMap.of();
    }

    private String applyTextPatches(final String text){

        if(StringTools.isNullOrEmpty(text)) return text;

        String result = text;

        for (Map.Entry<String, String> textPatchEntry : textPatchMap.entrySet()) {
            result = result.replace(textPatchEntry.getKey(), textPatchEntry.getValue());
        }

        return result;

    }

    public static Classifier train(final File trainingFile,
                                   final File stopWordFile,
                                   final File textPatchFile,
                                   final int sharedWordFilterThreshold) {
        checkNotNull(trainingFile, "trainingFile");

        final Classifier result = new Classifier(sharedWordFilterThreshold);

        result.loadStopWordFile(stopWordFile);
        result.loadTextPatchFile(textPatchFile);
        result.loadTrainingFile(trainingFile);
        result.filterSharedWords();

        return result;
    }

    private void filterSharedWords() {
        if (sharedWordFilterThreshold <= 0) {
            LogTools.info("Shared word filter is disabled");
            return;
        }

        for (Classification classification : classificationMap.values()) {

            for (Classification other : classificationMap.values()) {

                if (other.equals(classification)) {
                    continue;
                }

                final Sets.SetView<String> sharedWords = Sets.intersection(classification.getWordFrequencyMap().keySet(), other.getWordFrequencyMap().keySet());

                for (String sharedWord : sharedWords) {

                    filterSharedWord(classification, sharedWord);
                    filterSharedWord(other, sharedWord);
                }

            }

        }

    }

    private void filterSharedWord(final Classification classification, final String word) {
        if (classification.getWordFrequencyMap().get(word) <= sharedWordFilterThreshold) {
            LogTools.info("Discarding shared word {0} from classification {1}", word, classification.getLabel());
            classification.getWordFrequencyMap().remove(word);
        }
    }

    private void loadTextPatchFile(final File textPatchFile){
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

                if(patchParts.size() != 2){
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

    private void loadTrainingFile(final File trainingFile) {
        checkNotNull(trainingFile, "trainingFile");
        checkArgument(FileTools.fileExistsAndCanRead(trainingFile), "cannot read training file");

        try {
            final List<String> lines = Files.readLines(trainingFile, Charset.defaultCharset());

            final ObjectMapper objectMapper = new ObjectMapper();

            final Splitter whiteSpaceSplitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults();

            int lineCount = 0;

            for (String line : lines) {
                final String trimmed = line.trim().toLowerCase();

                if (trimmed.startsWith("#")) continue;

                final TrainingText trainingText = objectMapper.readValue(trimmed, TrainingText.class);

                lineCount++;

                final String cleanText = filterStopWords(TextUtils.cleanText(trainingText.getText()));

                if (cleanText.isEmpty()) {
                    LogTools.info("Skipping empty training text: {0}", String.valueOf(trainingText.getId()));
                    continue;
                }

                for (String label : trainingText.getClassifications()) {

                    Classification classification = classificationMap.get(label);

                    if (classification == null) {
                        classification = new Classification(label);
                        classificationMap.put(label, classification);
                    }

                    classification.setTrainingTextCount(classification.getTrainingTextCount() + 1);

                    classification.addAll(whiteSpaceSplitter.splitToList(cleanText));

                }

            }

            LogTools.info("Loaded {0} lines from training text", String.valueOf(lineCount));

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private String filterStopWords(final String text) {
        String result = text;

        for (String stopWord : stopWords) {
            result = result.replace(stopWord, "");
        }

        return result;
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

}
