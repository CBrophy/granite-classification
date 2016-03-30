package org.granite.classification.model;

import com.google.common.collect.ComparisonChain;

import org.granite.base.StringTools;

import static com.google.common.base.Preconditions.checkArgument;

public class StringScore implements Comparable<StringScore> {

    private final String key;
    private double score;

    public StringScore(final String key) {
        checkArgument(!StringTools.isNullOrEmpty(key), "key is null or empty");

        this.key = key;
    }

    public StringScore(final String key, final double score) {
        this(key);
        this.score = score;
    }

    public String getKey() {
        return key;
    }

    public double getScore() {
        return score;
    }

    public void incrementScore(final double score){
        this.score += score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(StringScore stringScore) {
        return ComparisonChain
                .start()
                .compare(getScore(), stringScore.getScore())
                .compare(getKey(), stringScore.getKey())
                .result();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringScore
                && key.equalsIgnoreCase(((StringScore) obj).getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
