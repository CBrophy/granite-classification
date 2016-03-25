package org.granite.classification;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;

import org.granite.base.StringTools;
import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.TextUtils;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesClassifier extends BasicClassifier {

    private final HashMap<String, Double> probabilityOfClassification = new HashMap<>();
    private final HashMap<String, HashMap<String, Double>> probabilityOfWordGivenClassification = new HashMap<>();

    public ImmutableMap<String, Double> classify(final String text) {

        findWordPosteriorProbabilities(text);

        final HashMap<String, Double> result = new HashMap<>();

        findWordPosteriorProbabilities(text)
                .values()
                .forEach(classificationPosteriorMap -> {
                    classificationPosteriorMap
                            .entrySet()
                            .forEach(entry -> {
                                double existingMaxValue = result.getOrDefault(entry.getKey(), Double.MIN_VALUE);

                                result.put(entry.getKey(), Math.max(existingMaxValue, entry.getValue()));
                            });
                });

        final ImmutableMap.Builder<String, Double> builder = ImmutableMap.builder();

        double total = 0.0;

        for (Double posterior : result.values()) {
            total += posterior;
        }

        final double mean = total / (double) result.size();

        double variance = 0.0;

        for (Double posterior : result.values()) {
            variance += Math.pow(mean - posterior, 2) / ((double) result.size() - 1.0);
        }

        double standardDev = Math.sqrt(variance);

        result
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > mean + standardDev)
                .forEach(entry -> builder.put(entry.getKey(), entry.getValue()));

        return builder.build();
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

        final HashMultimap<String, TrainingText> classificationTrainingText = HashMultimap.create();

        trainingSet
                .values()
                .forEach(trainingText -> {
                    trainingText
                            .getClassifications()
                            .forEach(classification -> classificationTrainingText.put(classification, trainingText));
                });

        for (String classification : classificationTrainingText
                .keySet()) {

            final Set<TrainingText> trainingTexts = classificationTrainingText.get(classification);

            if (trainingTexts.size() == 1) {
                LogTools.warn("Very few training text for classification: {0}", classification);
            }

            final double trainingTextCount = (double) trainingTexts.size();

            probabilityOfClassification.put(classification, trainingTextCount / (double) trainingSet.size());

            final HashMap<String, Integer> wordFrequency = new HashMap<>();

            for (TrainingText trainingText : trainingTexts) {
                TextUtils.updateFrequencyMap(trainingText.getWordFrequencies().keySet(), wordFrequency);
            }

            for (Map.Entry<String, Integer> wordFrequencyEntry : wordFrequency
                    .entrySet()) {

                final String word = wordFrequencyEntry.getKey();

                final double frequency = (double) wordFrequencyEntry.getValue();

                final String directedClassification = getWordToClassificationDirection().get(word);

                if (directedClassification != null && !directedClassification.equals(classification)) {
                    // This word has a direction, do not add it to this classification unless it
                    // it is the one being directed to
                    continue;
                }

                HashMap<String, Double> classificationMap = probabilityOfWordGivenClassification.get(word);

                if (classificationMap == null) {
                    classificationMap = new HashMap<>();
                    probabilityOfWordGivenClassification.put(word, classificationMap);
                }

                classificationMap.put(classification, frequency / trainingTextCount);
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
