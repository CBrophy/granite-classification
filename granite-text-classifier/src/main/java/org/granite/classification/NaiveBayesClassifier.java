package org.granite.classification;


import com.google.common.collect.ImmutableMap;

import org.granite.base.StringTools;
import org.granite.classification.model.TrainingText;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesClassifier extends BasicClassifier {

    private final HashMap<String, Double> probabilityOfClassification = new HashMap<>();
    private final HashMap<String, HashMap<String, Double>> probabilityOfWordGivenClassification = new HashMap<>();

    public ImmutableMap<String, Double> classify(final String text) {
        if (StringTools.isNullOrEmpty(text)) return ImmutableMap.of();

        final HashMap<String, Integer> allWords = textToFilteredWords(text);

        if (allWords.isEmpty()) {
            return ImmutableMap.of();
        }

        final HashMap<String, Double> result = new HashMap<>();

        return ImmutableMap.copyOf(result);
    }

    public ImmutableMap<String, HashMap<String, Double>> findWordPosteriorProbabilities(final String text) {
        if (StringTools.isNullOrEmpty(text)) return ImmutableMap.of();

        final HashMap<String, Integer> allWords = textToFilteredWords(text);

        if (allWords.isEmpty()) {
            return ImmutableMap.of();
        }

        HashMap<String, HashMap<String, Double>> result = new HashMap<>();

        for (String word : allWords.keySet()) {

            final HashMap<String, Double> classificationNumerators = findPriorTimesLikelihood(word);

            if (classificationNumerators.isEmpty()) {
                // Numerators are all zero, posterior probability is infinitesimal
                continue;
            }

            result.put(word, classificationNumerators);

        }

        return ImmutableMap.copyOf(result);
    }

    private HashMap<String, Double> findPriorTimesLikelihood(final String word) {

        // It may be an unknown word
        final HashMap<String, Double> probabilityPerClassificationMap = probabilityOfWordGivenClassification.get(word);

        final HashMap<String, Double> result = new HashMap<>();

        if (probabilityPerClassificationMap != null) {

            // Determine prior x likelihood for all known classifications
            for (Map.Entry<String, Double> probabilityOfClassificationEntry : probabilityOfClassification.entrySet()) {

                final String classification = probabilityOfClassificationEntry.getKey();

                final double prior = probabilityOfClassificationEntry.getValue();

                final double likelihood = probabilityPerClassificationMap.getOrDefault(classification, Double.MIN_VALUE);

                result.put(classification, prior * likelihood);
            }
        }


        return result;
    }


    @Override
    public void train(ImmutableMap<Integer, TrainingText> trainingSet) {
        checkNotNull(trainingSet, "trainingSet");
        checkArgument(!trainingSet.isEmpty(), "trainingSet is empty");

        final HashMap<String, HashMap<String, Integer>> wordClassificationFrequencyMap = new HashMap<>();

        final HashMap<String, Integer> classificationFrequency = new HashMap<>();

        for (Map.Entry<Integer, TrainingText> trainingTextEntry : trainingSet.entrySet()) {

            final TrainingText trainingText = trainingTextEntry.getValue();

            for (Map.Entry<String, Integer> wordFrequencyEntry : trainingText.getWordFrequencies().entrySet()) {

                final String word = wordFrequencyEntry.getKey();
                final int frequency = wordFrequencyEntry.getValue();

                // Increment this word's classifications' frequency by the number of times this word occurs
                // int this training line
                HashMap<String, Integer> wordClassificationFrequency = wordClassificationFrequencyMap.get(word);

                if (wordClassificationFrequency == null) {
                    wordClassificationFrequency = new HashMap<>();
                    wordClassificationFrequencyMap.put(word, wordClassificationFrequency);
                }

                for (String classification : trainingText
                        .getClassifications()) {
                    // Record the number of times this word appears for each of its classifications
                    final int currentWordClassificationFrequency = wordClassificationFrequency.getOrDefault(classification, 0);

                    wordClassificationFrequency.put(classification, currentWordClassificationFrequency + frequency);

                    // Record the number of words in the corpus that represent this classification
                    final int currentClassificationFrequency = classificationFrequency.getOrDefault(classification, 0);

                    classificationFrequency.put(classification, currentClassificationFrequency + frequency);

                }

            }

        }

        LogTools.info("Training set has {0} classifications and {0} words", String.valueOf(classificationFrequency.size()), String.valueOf(getTrainingSetWordCount()));

        // Find probability of classifications
        for (Map.Entry<String, Integer> classificationWordFrequencyEntry : classificationFrequency.entrySet()) {

            final String classification = classificationWordFrequencyEntry.getKey();

            final int wordCount = classificationWordFrequencyEntry.getValue();

            final double probability = (double) wordCount / (double) getTrainingSetWordCount();

            probabilityOfClassification.put(classification, probability);
        }

        //Find probability of words given classification

        for (Map.Entry<String, HashMap<String, Integer>> wordClassificationFrequencyEntry : wordClassificationFrequencyMap.entrySet()) {

            final String word = wordClassificationFrequencyEntry.getKey();

            HashMap<String, Double> classificationProbabilityMap = probabilityOfWordGivenClassification.get(word);

            if (classificationProbabilityMap == null) {
                classificationProbabilityMap = new HashMap<>();
                probabilityOfWordGivenClassification.put(word, classificationProbabilityMap);
            }

            for (Map.Entry<String, Integer> classificationWordCountEntry : wordClassificationFrequencyEntry.getValue().entrySet()) {

                final String classification = classificationWordCountEntry.getKey();

                final int wordCountInClassification = classificationWordCountEntry.getValue();

                final int classificationTotalWordCount = classificationFrequency.get(classificationWordCountEntry.getKey());

                final double probability = (double) wordCountInClassification / (double) classificationTotalWordCount;

                classificationProbabilityMap.put(classification, probability);
            }


        }


        LogTools.info("Loaded {0} lines from training text", String.valueOf(trainingSet.size()));

    }

    public HashMap<String, Double> getProbabilityOfClassification() {
        return probabilityOfClassification;
    }

    public HashMap<String, HashMap<String, Double>> getProbabilityOfWordGivenClassification() {
        return probabilityOfWordGivenClassification;
    }

}
