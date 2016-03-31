package org.granite.classification;

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

public class GradientBooster {
    private final ImmutableMap<Integer, TrainingText> initialTrainingTexts;
    private final StandardDeviationScoreFilter<StringScore> scoreFilter = new StandardDeviationScoreFilter<>(0.0);

    public GradientBooster(final ImmutableMap<Integer, TrainingText> initialTrainingTexts) {

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
        }

        wordBagClassifier
                .train(mutableTrainingSet);
    }

    private MutableTrainingSet boost(
                          final WordBagClassifier wordBagClassifier,
                          final MutableTrainingSet mutableTrainingSet
    ) {

        final HashMap<Integer, ImmutableSet<ClassificationScore>> lineClassificationScores = scoreTrainingTexts(wordBagClassifier);

        final MutableTrainingSet boostedTrainingSet = new MutableTrainingSet(mutableTrainingSet);

        for (Map.Entry<Integer, ImmutableSet<ClassificationScore>> entry : lineClassificationScores.entrySet()) {

            final HashMap<String, Double> maxScores = new HashMap<>();

            // Determine how high each target classification score can be
            initialTrainingTexts.get(entry.getKey())
                    .getClassifications()
                    .forEach(classification -> maxScores.put(classification, wordBagClassifier.getMaxClassificationScore(classification)));

            final HashSet<String> lineClassifications = new HashSet<>();

            for (ClassificationScore classificationScore : entry
                    .getValue()) {

                lineClassifications.add(classificationScore.getKey());


                if(!maxScores.containsKey(classificationScore.getKey())){
                    LogTools.info("Training set line {0} classified as additional target {1}. Training set requires adjustment",
                            String.valueOf(entry.getKey()),
                            classificationScore.getKey());
                    continue;
                }

                // Get the words that score above the mean
                ImmutableSet<StringScore> wordScores = this.scoreFilter.filter(classificationScore.getContributors());

                //TODO

            }


        }


        return null;
    }

    private HashMap<Integer, ImmutableSet<ClassificationScore>> scoreTrainingTexts(final WordBagClassifier wordBagClassifier) {
        HashMap<Integer, ImmutableSet<ClassificationScore>> lineClassificationScores = new HashMap<>();

        for (Map.Entry<Integer, TrainingText> entry : initialTrainingTexts.entrySet()) {
            lineClassificationScores.put(entry.getKey(), wordBagClassifier.classify(entry.getValue().getText()));
        }

        return lineClassificationScores;
    }
}
