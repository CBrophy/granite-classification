package org.granite.classification;

import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.ScoreFilter;
import org.granite.classification.model.StringScore;
import org.granite.math.StatsTools;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class StandardDeviationScoreFilter<T extends StringScore> extends ScoreFilter<T> {

    private final double standardDeviationsAboveMean;

    public StandardDeviationScoreFilter(final double standardDeviationsAboveMean) {
        this.standardDeviationsAboveMean = standardDeviationsAboveMean;
    }

    @Override
    public ImmutableSet<T> filter(final Iterable<T> stringScores) {
        checkNotNull(stringScores, "stringScores");

        final List<Double> scores = new ArrayList<>();

        for (T stringScore : stringScores) {
            scores.add(stringScore.getScore());
        }

        if (scores.isEmpty()) return ImmutableSet.of();

        final ImmutableSet.Builder<T> builder = ImmutableSet.builder();

        final double mean = StatsTools.mean(scores);

        final double standardDeviation = StatsTools.standardDev(scores, mean);

        for (T stringScore : stringScores) {
            if (stringScore.getScore() > mean + (standardDeviationsAboveMean * standardDeviation)) {
                builder.add(stringScore);
            }
        }

        return builder.build();
    }
}
