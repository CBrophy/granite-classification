package org.granite.classification;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.model.TrainingText;
import org.granite.configuration.ApplicationConfiguration;
import org.granite.configuration.ConfigTools;
import org.junit.Test;

import java.io.File;

public class ClassifierTest {
    private final ApplicationConfiguration configuration = ConfigTools.readConfiguration("", "classifier.properties", "local-classifier.properties");

    @Test
    public void testTraining(){
        final File stopWordFile = new File(configuration.getString("classifier.stop-words-set"));
        final File trainingFile = new File(configuration.getString("classifier.training-set"));
        final File testingFile = new File(configuration.getString("classifier.test-set"));

        final Classifier classifier =
                Classifier.train(trainingFile,
                        stopWordFile,
                        null,
                        .70);

        final ImmutableMap<Integer, TrainingText> testingTextMap = Classifier.loadTrainingText(testingFile);

        for (TrainingText testingText : testingTextMap
                .values()) {

            final ImmutableMap<String, Double> classifications = classifier.classify(testingText.getText());

            System.out.println("test");
        }

    }
}