package org.granite.classification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.lucene.analysis.util.CharArraySet;
import org.granite.classification.model.TrainingText;
import org.granite.classification.utils.LuceneWordBagger;
import org.granite.configuration.ApplicationConfiguration;
import org.granite.configuration.ConfigTools;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class NaiveBayesClassifierTest {
    private final ApplicationConfiguration configuration = ConfigTools.readConfiguration("", "classifier.properties", "local-classifier.properties");

    @Test
    public void testTraining() {
        final File stopWordFile = new File(configuration.getString("classifier.stop-words-set"));
        final File trainingFile = new File(configuration.getString("classifier.training-set"));
        final File testingFile = new File(configuration.getString("classifier.test-set"));

        final NaiveBayesClassifier classifier = new NaiveBayesClassifier();

        classifier.train(trainingFile,
                stopWordFile);

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger(classifier.getStopWords());

        classifier
                .getTrainingSetWordClassificationCounts()
                .keySet()
                .forEach(System.out::println);

        final ImmutableMap<Integer, TrainingText> testingTextMap = NaiveBayesClassifier.loadTrainingText(testingFile);

        for (TrainingText testingText : testingTextMap
                .values()) {

            final ImmutableSet<String> wordBag = luceneWordBagger.createWordBag(testingText.getText());

            final ImmutableMap<String, Double> classifications = classifier.classify(wordBag);

            System.out.println("test");
        }

    }

    @Test
    public void testProbabilityOfOtherClassification(){
        final ImmutableList<Double> test = ImmutableList.of(0.5, 0.4, 0.6);

        double result = NaiveBayesClassifier.calculateProbabilityOfOtherClassifications(test);

        // assumed to be independent vars
        double expected = 0.5 + 0.4 + 0.6 - (0.5 * 0.4) - (0.5 * 0.6) - (0.4 * 0.6) + (0.5 * 0.4 * 0.6);

        assertEquals(expected, result, 0.01);
    }
}