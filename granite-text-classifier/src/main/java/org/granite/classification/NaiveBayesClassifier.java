package org.granite.classification;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.granite.base.StringTools;
import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.TextUtils;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesClassifier extends BasicClassifier {

    private final TreeMap<String, Double> probabilityOfClassification = new TreeMap<>();
    private final TreeMap<String, TreeMap<String, Double>> probabilityOfWordGivenClassification = new TreeMap<>();

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

        final ImmutableSet<String> allWords = textToFilteredWordBag(text);

        if (allWords.isEmpty()) {
            return ImmutableMap.of();
        }

        HashMap<String, HashMap<String, Double>> result = new HashMap<>();

        for (String word : allWords) {

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
        final TreeMap<String, Double> probabilityPerClassificationMap = probabilityOfWordGivenClassification.get(word);

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

    private double findPrior(final String classification){
        checkNotNull(classification, "classification");

        return getProbabilityOfClassification().get(classification);
    }

    private double findLikelihood(final String word, final String classification){
        final TreeMap<String, Double> classificationProbalities = this.getProbabilityOfWordGivenClassification().get(word);

        final Double numerator = classificationProbalities.get(classification);

        if(numerator == null) {
            return 0.0; // No likelihood, zilch
        }

        if(classificationProbalities.size() == 1){
            return numerator;
        }

        //Probabilities get very nasty at this point

        for (Map.Entry<String, Double> entry : classificationProbalities.entrySet()) {
            final String otherClassification = entry.getKey();
            final double otherProbability = entry.getValue();

            if(classificationProbalities.size() == 2){
                //take a short cut!
                return numerator / otherProbability;
            }


        }

        return 0.0;

    }

    @Override
    public void train() {

        for (Map.Entry<String, Integer> entry : getTrainingSetClassificationCounts().entrySet()) {
            final String classification = entry.getKey();

            final int count = entry.getValue();

            final double probability = (double) count / (double) getTrainingSetCount();

            LogTools.info("Probability of {0} is {1}", classification, String.valueOf(probability));

            this.getProbabilityOfClassification().put(classification, probability);
        }

        for (Map.Entry<String, TreeMap<String, Integer>> entry : getTrainingSetClassificationWordCounts().entrySet()) {

            final String classification = entry.getKey();

            final TreeMap<String, Integer> classificationWordCounts = entry.getValue();

            final int classificationTotalWordCount = getTrainingSetClassificationTotalWordCounts().get(classification);

            for (Map.Entry<String, Integer> wordEntry : classificationWordCounts.entrySet()) {

                final String word = wordEntry.getKey();

                final int count = wordEntry.getValue();

                final double probability = (double) count / (double) classificationTotalWordCount;

                getNestedMap(word, this.probabilityOfWordGivenClassification).put(word, probability);
            }

        }

    }

    public TreeMap<String, Double> getProbabilityOfClassification() {
        return probabilityOfClassification;
    }

    public TreeMap<String, TreeMap<String, Double>> getProbabilityOfWordGivenClassification() {
        return probabilityOfWordGivenClassification;
    }
}
