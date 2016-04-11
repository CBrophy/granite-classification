package org.granite.classification;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

import org.granite.classification.model.ClassificationScore;
import org.granite.classification.model.TrainingSet;
import org.granite.classification.model.WordBagger;
import org.granite.classification.utils.MapUtils;
import org.granite.log.LogTools;
import org.granite.math.ProbabilityTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class NaiveBayesClassifier extends WordBagClassifier {

    private ImmutableMap<String, Double> classificationProbabilities = ImmutableMap.of();

    private ImmutableMap<String, Map<String, Double>> wordGivenClassificationProbabilities = ImmutableMap.of();

    public NaiveBayesClassifier(final WordBagger wordBagger) {
        super(wordBagger);
    }

    @Override
    protected ImmutableSet<ClassificationScore> doClassify(final Set<String> wordBag) {
        checkNotNull(wordBag, "wordBag");

        if (wordBag.isEmpty()) {
            return ImmutableSet.of();
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

        return convertHashMap(result);
    }

    private HashMap<String, Double> findPriorTimesLikelihood(final String word) {

        // It may be an unknown word
        final Map<String, Double> probabilityPerClassificationMap = getWordGivenClassificationProbabilities().get(word);

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
        final Map<String, Double> probabilityPerClassificationMap = getWordGivenClassificationProbabilities().get(word);

        final double numerator = probabilityPerClassificationMap.getOrDefault(classification, 0.0);

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

    @Override
    public void train(final TrainingSet trainingSet) {
        checkNotNull(trainingSet, "trainingSet");

        final HashMap<String, Double> classificationProbabilities = new HashMap<>();

        // First determine how probable each classification is from it's frequency in the training set
        for (Map.Entry<String, Double> entry : trainingSet.getClassificationLineCounts().entrySet()) {

            final String classification = entry.getKey();

            final double probability = entry.getValue() / trainingSet.getTrainingSetSize();

            classificationProbabilities.put(classification, probability);
        }

        // Next, determine the probability of each word given a classification
        final Map<String, Map<String, Double>> wordGivenClassificationProbabilities = new HashMap<>();

        for (Map.Entry<String, Map<String, Double>> classificationEntry : trainingSet.getClassificationWordCounts().entrySet()) {

            final String classification = classificationEntry.getKey();

            final Map<String, Double> classificationWordCounts = classificationEntry.getValue();

            if(classificationWordCounts.isEmpty()) {
                continue;
            }

            final double classificationTotalWordCount = trainingSet.getClassificationTotalWordCounts().get(classification);

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
    }

    @Override
    public Set<String> getClassifications() {
        return getClassificationProbabilities().keySet();
    }

    @Override
    public double getMaxClassificationScore(final String classification) {
        checkNotNull(classification, "classification");
        // The highest score in naive bayes is the probability of the classification itself
        // P(Class) * L(Word:Class) = P(Class:Word)
        return getClassificationProbabilities().getOrDefault(classification, 0.0);
    }

    public ImmutableMap<String, Double> getClassificationProbabilities() {
        return classificationProbabilities;
    }

    public ImmutableMap<String, Map<String, Double>> getWordGivenClassificationProbabilities() {
        return wordGivenClassificationProbabilities;
    }

}
