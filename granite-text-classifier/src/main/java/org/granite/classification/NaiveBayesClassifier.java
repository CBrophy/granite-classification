package org.granite.classification;


import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;

import org.granite.classification.model.TrainingSet;
import org.granite.classification.utils.MapUtils;
import org.granite.log.LogTools;
import org.granite.math.ProbabilityTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesClassifier extends WordBagClassifier {

    private ImmutableMap<String, Double> classificationProbabilities = ImmutableMap.of();
    private ImmutableMap<String, ImmutableMap<String, Double>> wordGivenClassificationProbabilities = ImmutableMap.of();
    private ImmutableMap<String, ImmutableMap<String, Double>> wordClassificationBoosts = ImmutableMap.of();

    public NaiveBayesClassifier(final TrainingSet trainingSet) {
        super(trainingSet);
    }

    public ImmutableMap<String, ImmutableMap<String, Double>> classify(final Set<String> wordBag) {
        checkNotNull(wordBag, "wordBag");

        if (wordBag.isEmpty()) {
            return ImmutableMap.of();
        }

        final HashMap<String, HashMap<String, Double>> result = new HashMap<>();

        for (String word : wordBag) {

            final HashMap<String, Double> classificationNumerators = findPriorTimesLikelihood(word);

            if (classificationNumerators.isEmpty()) {
                // Numerators are all zero, posterior probability is infinitesimal
                continue;
            }

            // Turn the result into classification -> word:posterior
            classificationNumerators
                    .entrySet()
                    .forEach(entry ->
                            result
                                    .computeIfAbsent(entry.getKey(), key -> new HashMap<>())
                                    .put(word, entry.getValue())
                    );

        }

        return MapUtils.buildImmutableCopy(result);
    }

    private HashMap<String, Double> findPriorTimesLikelihood(final String word) {

        // It may be an unknown word
        final ImmutableMap<String, Double> probabilityPerClassificationMap = getWordGivenClassificationProbabilities().get(word);

        final HashMap<String, Double> result = new HashMap<>();

        if (probabilityPerClassificationMap != null) {

            // Determine prior x likelihood for all known classifications
            for (Map.Entry<String, Double> probabilityOfClassificationEntry : getClassificationProbabilities().entrySet()) {

                final String classification = probabilityOfClassificationEntry.getKey();

                final double prior = probabilityOfClassificationEntry.getValue();

                final double likelihood = findLikelihood(word, classification);

                result.put(classification, prior * likelihood);
            }
        }

        return result;
    }

    private double findLikelihood(final String word, final String classification) {
        // Finding
        // P(C1) / P(C2 u C3 u ...)
        final ImmutableMap<String, Double> probabilityPerClassificationMap = getWordGivenClassificationProbabilities().get(word);

        final double boost = getWordClassificationBoosts()
                .getOrDefault(word, ImmutableMap.of())
                .getOrDefault(classification, 1.0);

        final double numerator = probabilityPerClassificationMap.getOrDefault(classification, 0.0) * boost;

        if (DoubleMath.fuzzyEquals(numerator, 0.0, 0.0001)) {
            return 0.0; // No likelihood
        }

        // If this classification has the only probability, return the value
        if (probabilityPerClassificationMap.size() == 1) {
            return numerator;
        }

        //Probabilities get very nasty at this point

        final ArrayList<Double> otherClassificationProbabilities = new ArrayList<>();

        for (Map.Entry<String, Double> entry : probabilityPerClassificationMap.entrySet()) {
            final String otherClassification = entry.getKey();
            final double otherProbability = entry.getValue();

            // Do not count the numerator classification
            if (classification.equalsIgnoreCase(otherClassification)) continue;

            if (probabilityPerClassificationMap.size() == 2) {
                // take a short cut if there are only two,
                // the numerator and the denominator
                return numerator / otherProbability;
            }

            otherClassificationProbabilities.add(otherProbability);

        }

        final double denominator = ProbabilityTools.independentUnion(otherClassificationProbabilities);

        return denominator > 0.0 ? numerator / denominator : numerator;

    }

    public NaiveBayesClassifier train() {

        final HashMap<String, Double> classificationProbabilities = new HashMap<>();

        // First determine how probable each classification is from it's frequency in the training set
        for (Map.Entry<String, Double> entry : getTrainingSet().getClassificationLineCounts().entrySet()) {

            final String classification = entry.getKey();

            final double probability = entry.getValue() / getTrainingSet().getTrainingSetSize();

            LogTools.info("Probability of {0} is {1}", classification, String.valueOf(probability));

            classificationProbabilities.put(classification, probability);
        }

        // Next, determine the probability of each word given a classification
        final HashMap<String, HashMap<String, Double>> wordGivenClassificationProbabilities = new HashMap<>();

        for (Map.Entry<String, ImmutableMap<String, Double>> classificationEntry : getTrainingSet().getClassificationWordCounts().entrySet()) {

            final String classification = classificationEntry.getKey();

            final ImmutableMap<String, Double> classificationWordCounts = classificationEntry.getValue();

            final double classificationTotalWordCount = getTrainingSet().getClassificationTotalWordCounts().get(classification);

            for (Map.Entry<String, Double> wordEntry : classificationWordCounts.entrySet()) {

                final String word = wordEntry.getKey();

                final double probability = wordEntry.getValue() / classificationTotalWordCount;

                wordGivenClassificationProbabilities
                        .computeIfAbsent(word, key -> new HashMap<>())
                        .put(classification, probability);
            }

        }

        this.wordGivenClassificationProbabilities = MapUtils.buildImmutableCopy(wordGivenClassificationProbabilities);
        this.classificationProbabilities = ImmutableMap.copyOf(classificationProbabilities);
        return this;
    }

    public NaiveBayesClassifier withBoost(final Function<WordBagClassifier, ImmutableMap<String, ImmutableMap<String, Double>>> boostFunction) {
        checkNotNull(boostFunction, "boostFunction");

        this.wordClassificationBoosts = boostFunction.apply(this);

        return this;
    }

    public ImmutableMap<String, Double> getClassificationProbabilities() {
        return classificationProbabilities;
    }

    public ImmutableMap<String, ImmutableMap<String, Double>> getWordGivenClassificationProbabilities() {
        return wordGivenClassificationProbabilities;
    }

    public ImmutableMap<String, ImmutableMap<String, Double>> getWordClassificationBoosts() {
        return wordClassificationBoosts;
    }
}
