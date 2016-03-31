package org.granite.classification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.ClassificationScore;
import org.granite.classification.model.StringScore;
import org.granite.classification.model.TrainingText;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class IncrementalBooster {
    private final ImmutableMap<Integer, TrainingText> initialTrainingTexts;
    private final StandardDeviationScoreFilter<StringScore> scoreFilter = new StandardDeviationScoreFilter<>(0.0);

    public IncrementalBooster(final ImmutableMap<Integer, TrainingText> initialTrainingTexts) {

        this.initialTrainingTexts = checkNotNull(initialTrainingTexts, "initialTrainingTexts");

    }

    public void trainWithBoosting(final WordBagClassifier wordBagClassifier, final ImmutableTrainingSet immutableTrainingSet) {
        checkNotNull(wordBagClassifier, "wordBagClassifier");
        checkNotNull(immutableTrainingSet, "immutableTrainingSet");

        // The initial training set will be adjusted during boosting
        MutableTrainingSet mutableTrainingSet = new MutableTrainingSet(immutableTrainingSet);

        wordBagClassifier.train(immutableTrainingSet);

        int counter = 0;

        MutableTrainingSet lastTrainingSet = mutableTrainingSet;

        while ((lastTrainingSet = boost(wordBagClassifier, mutableTrainingSet)) != null) {

            LogTools.info("Gradient booster pass {0}", String.valueOf(counter++));

            mutableTrainingSet = lastTrainingSet;

            if(counter % 10000 == 0){
                System.out.println();
            }

            wordBagClassifier
                    .train(mutableTrainingSet);

        }

    }

    private MutableTrainingSet boost(
            final WordBagClassifier wordBagClassifier,
            final MutableTrainingSet mutableTrainingSet
    ) {

        final HashMap<Integer, ImmutableSet<ClassificationScore>> lineClassificationScores = scoreTrainingTexts(wordBagClassifier);

        final MutableTrainingSet boostedTrainingSet = new MutableTrainingSet(mutableTrainingSet);

        final HashMultimap<String, String> classificationWordsToBoost = HashMultimap.create();

        final HashMultimap<String, String> classificationWordsToSuppress = HashMultimap.create();

        for (Map.Entry<Integer, ImmutableSet<ClassificationScore>> entry : lineClassificationScores.entrySet()) {

            final HashSet<String> classifications = new HashSet<>();

            for (ClassificationScore classificationScore : entry
                    .getValue()) {

                classifications.add(classificationScore.getKey());


                if (!initialTrainingTexts.get(entry.getKey())
                        .getClassifications().contains(classificationScore.getKey())) {

                    classificationScore
                            .getContributors()
                            .forEach(stringScore -> classificationWordsToSuppress.put(classificationScore.getKey(), stringScore.getKey()));
                }
//                } else {
//
//                    highestScores
//                            .forEach(stringScore -> classificationWordsToBoost.put(classificationScore.getKey(), stringScore.getKey()));
//
//                }

            }

            for (String expectedClassification : initialTrainingTexts.get(entry.getKey()).getClassifications()) {

                if (!classifications.contains(expectedClassification)) {
                    for (String word : initialTrainingTexts.get(entry.getKey()).getWordBag()) {
                        classificationWordsToBoost.put(expectedClassification, word);
                    }

                }
            }


        }

        if (classificationWordsToBoost.isEmpty() && classificationWordsToSuppress.isEmpty())
            return null;

        LogTools.info("Found {0} classification words to boost and {1} to suppress", String.valueOf(classificationWordsToBoost.size()), String.valueOf(classificationWordsToSuppress.size()));

        for (String classification : classificationWordsToBoost.keySet()) {

            for (String word : classificationWordsToBoost.get(classification)) {

//                if (classificationWordsToSuppress.containsEntry(classification, word)) {
//                    LogTools.info("Skipping boost of {0}:{1} because it needs to be suppressed", classification, word);
//                    continue;
//                }

                incrementWordClassification(word, classification, boostedTrainingSet, 1.0);
            }

        }

        for (String classification : classificationWordsToSuppress.keySet()) {

            for (String word : classificationWordsToSuppress.get(classification)) {
                 incrementWordClassification(word, classification, boostedTrainingSet, -1.0);
            }
        }

        return boostedTrainingSet;
    }

    private void incrementWordClassification(
            final String word,
            final String classification,
            final MutableTrainingSet mutableTrainingSet,
            final double increment) {

        mutableTrainingSet
                .getWordClassificationCounts()
                .computeIfAbsent(word, key -> new HashMap<>())
                .merge(classification, increment, (v1, v2) -> v1 + v2);

        cleanIfEmpty(word, classification, mutableTrainingSet.getWordClassificationCounts());

        mutableTrainingSet
                .getClassificationWordCounts()
                .computeIfAbsent(classification, key -> new HashMap<>())
                .merge(word, increment, (v1, v2) -> v1 + v2);

        cleanIfEmpty(classification, word, mutableTrainingSet.getClassificationWordCounts());

        mutableTrainingSet
                .getClassificationTotalWordCounts()
                .merge(classification, increment, (v1, v2) -> v1 + v2);

        mutableTrainingSet
                .getWordTotalCounts()
                .merge(word, increment, (v1, v2) -> v1 + v2);


        if (mutableTrainingSet.getWordTotalCounts().get(word) <= 0.0) {
            mutableTrainingSet.getWordTotalCounts().remove(word);
        }

    }

    private void cleanIfEmpty(final String key, final String secondaryKey, final Map<String, Map<String, Double>> map) {
        Map<String, Double> innerMap = map.get(key);

        if (innerMap != null) {

            Double value = innerMap.get(secondaryKey);

            if (value != null && value <= 0.0) {
                innerMap.remove(secondaryKey);
            }

            if (innerMap.isEmpty()) {
                map.remove(key);
            }

        }
    }

    private HashMap<Integer, ImmutableSet<ClassificationScore>> scoreTrainingTexts(final WordBagClassifier wordBagClassifier) {
        HashMap<Integer, ImmutableSet<ClassificationScore>> lineClassificationScores = new HashMap<>();

        for (Map.Entry<Integer, TrainingText> entry : initialTrainingTexts.entrySet()) {
            lineClassificationScores.put(entry.getKey(), wordBagClassifier.classify(entry.getValue().getText()));
        }

        return lineClassificationScores;
    }
}
