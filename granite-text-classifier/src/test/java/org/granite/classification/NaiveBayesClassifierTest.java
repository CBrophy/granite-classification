package org.granite.classification;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.ClassificationScore;
import org.granite.classification.model.TrainingText;
import org.granite.configuration.ApplicationConfiguration;
import org.granite.configuration.ConfigTools;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class NaiveBayesClassifierTest {
    private final ApplicationConfiguration configuration = ConfigTools.readConfiguration("", "classifier.properties", "local-classifier.properties");

    @Test
    public void testTraining() {
        final File stopWordFile = new File(configuration.getString("classifier.stop-words-set"));
        final File trainingFile = new File(configuration.getString("classifier.training-set"));
        final File testingFile = new File(configuration.getString("classifier.test-set"));

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger(stopWordFile);

        final TrainingSetFactory trainingSetFactory = new TrainingSetFactory(trainingFile, luceneWordBagger)
                .withTrainingSetFilter(new WordFrequencyTrainingSetFilter(2));

        final WordBagClassifier naiveBayesClassifier = new NaiveBayesClassifier(luceneWordBagger)
                .withScoreFilter(new StandardDeviationScoreFilter<>(1.1))
                .withContributorScoreFilter(new StandardDeviationScoreFilter<>(-0.2))
                .withScoreEnsembler(new MaxValueScoreEnsembler());

        naiveBayesClassifier.train(trainingSetFactory.createTrainingSet());

        final ImmutableMap<Integer, TrainingText> testingTextMap = TrainingSetFactory.loadTrainingText(testingFile, luceneWordBagger);

        for (TrainingText testingText : testingTextMap
                .values()) {

            ImmutableSet<ClassificationScore> classificationScores = naiveBayesClassifier.classify(testingText.getWordBag());

            System.out.println("test");
        }

    }


}