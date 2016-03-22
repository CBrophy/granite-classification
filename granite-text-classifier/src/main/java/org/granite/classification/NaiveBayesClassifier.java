package org.granite.classification;


import com.google.common.collect.ImmutableMap;

import org.granite.base.StringTools;
import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.TextUtils;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class NaiveBayesClassifier extends BasicClassifier {

    private final HashMap<String, Integer> classificationIndices = new HashMap<>();
    private final HashMap<String, Double> classificationProbabilities = new HashMap<>();
    private final HashMap<String, Double> wordProbabilities = new HashMap<>();
    private final HashMap<String, double[]> wordClassificationProbabilities = new HashMap<>();

    public ImmutableMap<String, Double> classify(final String text) {
        if (StringTools.isNullOrEmpty(text)) return ImmutableMap.of();

        final HashMap<String, Integer> allWords = textToWords(text);

        if (allWords.isEmpty()) {
            return ImmutableMap.of();
        }

        final ImmutableMap.Builder<String, Double> result = new ImmutableMap.Builder<>();

        double[] classificationProbability = new double[classificationIndices.size()];

        for(int index = 0; index < classificationProbability.length; index++){
            classificationProbability[index] = Double.MIN_VALUE; // smooth zeros
        }

        for (String word : allWords.keySet()) {

            final double[] probabilityVector = wordClassificationProbabilities.get(word);

            if(probabilityVector == null){
                // Unknown word, skip
                continue;
            }

            for(int index = 0; index < probabilityVector.length; index++){
                classificationProbability[index] *= probabilityVector[index];
            }
        }

        for (Map.Entry<String, Integer> classificationIndexEntry : classificationIndices.entrySet()) {

            result.put(classificationIndexEntry.getKey(), classificationProbability[classificationIndexEntry.getValue()]);
        }

        return result.build();
    }


    @Override
    public void train(ImmutableMap<Integer, TrainingText> trainingSet) {
        checkNotNull(trainingSet, "trainingSet");
        checkArgument(!trainingSet.isEmpty(), "trainingSet is empty");

        int trainingSetCount = 0;
        int totalWordCount = 0;

        final HashMap<String, Integer> classificationFrequencyMap = new HashMap<>();
        final HashMap<String, Integer> wordFrequencyMap = new HashMap<>();
        final HashMap<String, HashMap<String, Integer>> classificationWordFrequencyMap = new HashMap<>();
        final TreeSet<String> classifications = new TreeSet<>();

        for (Map.Entry<Integer, TrainingText> trainingTextEntry : trainingSet.entrySet()) {

            final int lineNumber = trainingTextEntry.getKey();

            final TrainingText trainingText = trainingTextEntry.getValue();

            final HashMap<String, Integer> trainingWordFrequency = textToWords(trainingText.getText());

            if (trainingWordFrequency.isEmpty()) {
                LogTools.info("Skipping useless training text on line [{0}]: {1}", String.valueOf(lineNumber), trainingText.getText());
                continue;
            }

            trainingSetCount++;

            for (Integer count : trainingWordFrequency.values()) {
                totalWordCount += count;
            }

            TextUtils.updateFrequencyMap(trainingText.getClassifications(), classificationFrequencyMap);

            TextUtils.updateFrequencyMap(trainingWordFrequency, wordFrequencyMap);

            for (String classification : trainingText.getClassifications()) {

                classifications.add(classification);

                HashMap<String, Integer> wordFrequency = classificationWordFrequencyMap.get(classification);

                if (wordFrequency == null) {
                    wordFrequency = new HashMap<>();
                    classificationWordFrequencyMap.put(classification, wordFrequency);
                }

                TextUtils.updateFrequencyMap(trainingWordFrequency, wordFrequency);
            }

        }

        checkState(totalWordCount > 0, "No words in training set");
        checkState(!classifications.isEmpty(), "No classifications in training set");

        LogTools.info("Training set has {0} classifications and {0} words", String.valueOf(classifications.size()), String.valueOf(wordFrequencyMap.size()));

        int index = 0;

        for (Map.Entry<String, Integer> wordFrequencyEntry : wordFrequencyMap.entrySet()) {

            wordProbabilities.put(wordFrequencyEntry.getKey(), (double) wordFrequencyEntry.getValue() / (double) totalWordCount);

        }

        final HashMap<String, Integer> classificationTotalWordCountMap = new HashMap<>();

        for (String classification : classifications) {

            classificationIndices.put(classification, index++);

            final double classificationProbability = (double) classificationFrequencyMap.get(classification) / (double) trainingSetCount;

            classificationProbabilities.put(classification, classificationProbability);

            final HashMap<String, Integer> classificationWordFrequency = classificationWordFrequencyMap.get(classification);

            int totalClassificationWordCount = 0;

            for (Integer count : classificationWordFrequency.values()) {
                totalClassificationWordCount += count;
            }

            classificationTotalWordCountMap.put(classification, totalClassificationWordCount);

        }

        for (Map.Entry<String, Double> wordProbabilityEntry : wordProbabilities.entrySet()) {

            final String word = wordProbabilityEntry.getKey();
            final double wordProbability = wordProbabilityEntry.getValue();

            double[] classificationPosteriorProbabilities = new double[classifications.size()];

            wordClassificationProbabilities.put(word, classificationPosteriorProbabilities);

            for (String classification : classifications) {

                final int classificationIndex = classificationIndices.get(classification);

                final double classificationProbability = classificationProbabilities.get(classification);

                final double classificationTotalWordCount = (double) classificationTotalWordCountMap.get(classification);

                final HashMap<String, Integer> classificationWordFrequency = classificationWordFrequencyMap.get(classification);

                Integer frequencyInClassification = classificationWordFrequency.get(word);

                double wordInClassificationProbability = frequencyInClassification == null ? Double.MIN_VALUE : (((double) frequencyInClassification) / classificationTotalWordCount);

                classificationPosteriorProbabilities[classificationIndex] = Math.max(Double.MIN_VALUE, (wordInClassificationProbability * classificationProbability) / wordProbability);
            }

        }

        LogTools.info("Loaded {0} lines from training text", String.valueOf(trainingSetCount));

    }
}
