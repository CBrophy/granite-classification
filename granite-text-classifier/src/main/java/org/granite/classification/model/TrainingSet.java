package org.granite.classification.model;

import java.util.Map;

public interface TrainingSet {

    Map<String, Double> getClassificationLineCounts();

    Map<String, Map<String, Double>> getWordClassificationCounts();

    Map<String, Map<String, Double>> getClassificationWordCounts();

    Map<String, Double> getClassificationTotalWordCounts();

    Map<String, Double> getWordTotalCounts();

    double getTrainingSetWordCount();

    double getTrainingSetSize();
}
