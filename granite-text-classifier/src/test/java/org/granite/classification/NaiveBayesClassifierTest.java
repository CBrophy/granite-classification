package org.granite.classification;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.EnsemblingMethods;
import org.granite.classification.model.TrainingSet;
import org.granite.classification.model.TrainingText;
import org.granite.configuration.ApplicationConfiguration;
import org.granite.configuration.ConfigTools;
import org.junit.Test;

import java.io.File;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class NaiveBayesClassifierTest {
    private final ApplicationConfiguration configuration = ConfigTools.readConfiguration("", "classifier.properties", "local-classifier.properties");

    @Test
    public void testTraining() {
        final File stopWordFile = new File(configuration.getString("classifier.stop-words-set"));
        final File trainingFile = new File(configuration.getString("classifier.training-set"));
        final File testingFile = new File(configuration.getString("classifier.test-set"));

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger(stopWordFile);

        final TrainingSet trainingSet = new TrainingSet(trainingFile, luceneWordBagger,0.01);

        final EnsemblingMethods.StandardDeviationEnsembler ensembler = new EnsemblingMethods.StandardDeviationEnsembler(1.0);

        final NaiveBayesClassifier classifier = new NaiveBayesClassifier(trainingSet).train();

        final ImmutableMap<Integer, TrainingText> testingTextMap = TrainingSet.loadTrainingText(testingFile, luceneWordBagger);

        for (TrainingText testingText : testingTextMap
                .values()) {

            final ImmutableMap<String, ImmutableMap<String, Double>> posteriors = classifier.classify(testingText.getWordBag());

            final ImmutableMap<String, Double> classifications =
                    ensembler.apply(posteriors);

            System.out.println("test");
        }

    }


}