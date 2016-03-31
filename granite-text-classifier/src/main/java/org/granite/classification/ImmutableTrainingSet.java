package org.granite.classification;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.model.TrainingSet;
import org.granite.classification.utils.MapUtils;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImmutableTrainingSet implements TrainingSet {
    private final ImmutableMap<String, Double> classificationLineCounts;
    private final ImmutableMap<String, Map<String, Double>> wordClassificationCounts;
    private final ImmutableMap<String, Map<String, Double>> classificationWordCounts;
    private final ImmutableMap<String, Double> classificationTotalWordCounts;
    private final ImmutableMap<String, Double> wordTotalCounts;
    private final double trainingSetSize;
    private final double trainingSetWordCount;

    public ImmutableTrainingSet(
            final Map<String, Double> classificationLineCounts,
            final Map<String, Map<String, Double>> wordClassificationCounts,
            final Map<String, Map<String, Double>> classificationWordCounts,
            final Map<String, Double> classificationTotalWordCounts,
            final Map<String, Double> wordTotalCounts,
            final double trainingSetSize,
            final double trainingSetWordCount) {
        this.classificationLineCounts = ImmutableMap.copyOf(checkNotNull(classificationLineCounts, "classificationLineCounts"));
        this.wordClassificationCounts = MapUtils.buildImmutableCopy(checkNotNull(wordClassificationCounts, "wordClassificationCounts"));
        this.classificationWordCounts = MapUtils.buildImmutableCopy(checkNotNull(classificationWordCounts, "classificationWordCounts"));
        this.classificationTotalWordCounts = ImmutableMap.copyOf(checkNotNull(classificationTotalWordCounts, "classificationTotalWordCounts"));
        this.wordTotalCounts = ImmutableMap.copyOf(checkNotNull(wordTotalCounts, "wordTotalCounts"));
        this.trainingSetSize = trainingSetSize;
        this.trainingSetWordCount = trainingSetWordCount;
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
