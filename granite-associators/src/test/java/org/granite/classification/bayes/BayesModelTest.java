package org.granite.classification.bayes;

import com.google.common.collect.ImmutableList;

import org.granite.classification.model.TrainingRow;
import org.granite.classification.model.TrainingSet;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BayesModelTest {

    private List<TrainingRow<Integer, String>> createTrainingRows() {
        final List<TrainingRow<Integer, String>> result = new ArrayList<>();

        // a, b, c, d, e, f, g, h, i
        // 1, 2, 3, 4, 5, 6, 7, 8, 9
        result.add(new TrainingRow<>(100, ImmutableList.of("a", "b", "c", "d")));
        result.add(new TrainingRow<>(200, ImmutableList.of("a", "e", "f", "d")));
        result.add(new TrainingRow<>(300, ImmutableList.of("g", "b", "c", "e")));
        result.add(new TrainingRow<>(400, ImmutableList.of("f", "h", "i", "b")));

        return result;
    }


    @Test
    public void testMeanScoreObservation() throws Exception {

        final BayesModel<String> model = BayesAssociationStatsBuilder
            .build(TrainingSet.build(createTrainingRows()));

        double bProb = (3.0 / 16.0);
        double hProb = (1.0 / 16.0);
        double aProb = (2.0 / 16.0);
        double cProb = 2.0 / 16.0;

        double bCProb = 2.0 / 9.0;
        double aCProb = 1.0 / 6.0;
        double bGProb = 1.0 / 9.0;

        final BayesAssociationStatistics<String> stats = model.getBayesAssociationStatisticsMap()
            .get("b");
        final BayesAssociationStatistics<String> aStats = model.getBayesAssociationStatisticsMap()
            .get("a");

        // P(H:B) = P(B) * L(H:B) / P(H)
        double hBPostProb = bProb * (stats.findAssociationLikelihood("h")) / hProb;
        double cBPostProb = bProb * (stats.findAssociationLikelihood("c")) / cProb;
        double cAPostProb = aProb * (aStats.findAssociationLikelihood("c")) / cProb;
        double hAPostProb = aProb * (aStats.findAssociationLikelihood("h")) / hProb;

        assertEquals(hBPostProb, stats.getAssociatedValuePosteriorProbabilities().get("h"), 0.0001);

        double cMeanProb = model.meanProbability("c", ImmutableList.of("b", "a"));
        double hMeanProb = model.meanProbability("h", ImmutableList.of("b", "a"));

        assertEquals((hBPostProb + hAPostProb) / 2.0, hMeanProb, 0.00001);
        assertEquals((cBPostProb + cAPostProb) / 2.0, cMeanProb, 0.00001);


    }

}