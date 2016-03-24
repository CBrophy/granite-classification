package org.granite.classification;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.model.TrainingText;
import org.granite.configuration.ApplicationConfiguration;
import org.granite.configuration.ConfigTools;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

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

        final ImmutableMap<Integer, TrainingText> testingTextMap = NaiveBayesClassifier.loadTrainingText(testingFile);

        for (TrainingText testingText : testingTextMap
                .values()) {

            final ImmutableMap<String, HashMap<String, Double>> posteriorProbailities = classifier.findWordPosteriorProbabilities(testingText.getText());

            System.out.println("test");
        }

    }
}