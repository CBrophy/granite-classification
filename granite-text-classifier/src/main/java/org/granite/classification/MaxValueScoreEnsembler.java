package org.granite.classification;

import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.ClassificationScore;
import org.granite.classification.model.ScoreEnsembler;
import org.granite.classification.model.StringScore;

import static com.google.common.base.Preconditions.checkNotNull;

public class MaxValueScoreEnsembler extends ScoreEnsembler<ClassificationScore> {

    @Override
    public ImmutableSet<ClassificationScore> ensemble(final Iterable<ClassificationScore> classificationScores) {
        checkNotNull(classificationScores, "classificationScores");

        final ImmutableSet.Builder<ClassificationScore> builder = ImmutableSet.builder();

        for (ClassificationScore classificationScore : classificationScores) {

            for (StringScore stringScore : classificationScore.getContributors()) {
                classificationScore.setScore(Math.max(classificationScore.getScore(), stringScore.getScore()));
            }

            builder.add(classificationScore);
        }

        return builder.build();
    }
}
