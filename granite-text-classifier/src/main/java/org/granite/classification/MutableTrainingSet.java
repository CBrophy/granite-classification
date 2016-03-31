package org.granite.classification;

import org.granite.classification.model.TrainingSet;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MutableTrainingSet implements TrainingSet {
    private final Map<String, Double> classificationLineCounts = new HashMap<>();
    private final Map<String, Map<String, Double>> wordClassificationCounts = new HashMap<>();
    private final Map<String, Map<String, Double>> classificationWordCounts = new HashMap<>();
    private final Map<String, Double> classificationTotalWordCounts = new HashMap<>();
    private final Map<String, Double> wordTotalCounts = new HashMap<>();
    private final double trainingSetSize;
    private final double trainingSetWordCount;

    public MutableTrainingSet(final TrainingSet trainingSet){
        checkNotNull(trainingSet, "trainingSet");

        classificationLineCounts.putAll(trainingSet.getClassificationLineCounts());
        classificationTotalWordCounts.putAll(trainingSet.getClassificationTotalWordCounts());
        wordTotalCounts.putAll(trainingSet.getWordTotalCounts());

        trainingSet
                .getClassificationWordCounts()
                .entrySet()
                .forEach(entry -> classificationWordCounts.put(entry.getKey(), new HashMap<>(entry.getValue())));

        trainingSet
                .getWordClassificationCounts()
                .entrySet()
                .forEach(entry -> wordClassificationCounts.put(entry.getKey(), new HashMap<>(entry.getValue())));

        this.trainingSetSize = trainingSet.getTrainingSetSize();
        this.trainingSetWordCount = trainingSet.getTrainingSetWordCount();
    }

    @Override
    public Map<String, Double> getClassificationLineCounts() {
        return classificationLineCounts;
    }

    @Override
    public Map<String, Map<String, Double>> getWordClassificationCounts() {
        return wordClassificationCounts;
    }

    @Override
    public Map<String, Map<String, Double>> getClassificationWordCounts() {
        return classificationWordCounts;
    }

    @Override
    public Map<String, Double> getClassificationTotalWordCounts() {
        return classificationTotalWordCounts;
    }

    @Override
    public Map<String, Double> getWordTotalCounts() {
        return wordTotalCounts;
    }

    @Override
    public double getTrainingSetWordCount() {
        return trainingSetWordCount;
    }

    @Override
    public double getTrainingSetSize() {
        return trainingSetSize;
    }

}
