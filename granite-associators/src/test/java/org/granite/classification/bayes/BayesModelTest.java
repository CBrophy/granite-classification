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

        final BayesModel<String> model = BayesModelBuilder
            .build(new TrainingSet.Builder<Integer, String>()
                .withStrictAssociation(false)
                .withTrainingRows(createTrainingRows())
                .build());

        double bProb = (3.0 / 16.0);
        double hProb = (1.0 / 16.0);
        double aProb = (2.0 / 16.0);
        double cProb = 2.0 / 16.0;

        double bCProb = 2.0 / 12.0;
        double aCProb = 1.0 / 8.0;
        double bGProb = 1.0 / 12.0;

        final BayesAssociationStatistics<String> stats = model.getAssociationStatisticsMap()
            .get("b");
        final BayesAssociationStatistics<String> aStats = model.getAssociationStatisticsMap()
            .get("a");
        final BayesAssociationStatistics<String> hStats = model.getAssociationStatisticsMap()
            .get("h");
        final BayesAssociationStatistics<String> cStats = model.getAssociationStatisticsMap()
            .get("c");

        // P(H:B) = P(H) * P(B:H) / P(B)
        double hBPostProb = (hProb * stats.getAssociatedValueProbabilities().getOrDefault("h",0.0)) / bProb;
        double cBPostProb = (cProb * stats.getAssociatedValueProbabilities().getOrDefault("c",0.0)) / bProb;
        double cAPostProb = (cProb * aStats.getAssociatedValueProbabilities().getOrDefault("c",0.0)) / aProb;
        double hAPostProb = (hProb * aStats.getAssociatedValueProbabilities().getOrDefault("h",0.0)) / aProb;
        double bHPostProb = (bProb * hStats.getAssociatedValueProbabilities().getOrDefault("b", 0.0)) / hProb;

        assertEquals(hBPostProb, hStats.getAssociatedValuePosteriorProbabilities().get("b"), 0.0001);
        assertEquals(cAPostProb, cStats.getAssociatedValuePosteriorProbabilities().get("a"), 0.0001);
        assertEquals(bHPostProb, stats.getAssociatedValuePosteriorProbabilities().get("h"), 0.0001);

        double cMeanProb = model.meanProbability("c", ImmutableList.of("b", "a"));
        double hMeanProb = model.meanProbability("h", ImmutableList.of("b", "a"));

        assertEquals((hBPostProb + hAPostProb) / 2.0, hMeanProb, 0.00001);
        assertEquals((cBPostProb + cAPostProb) / 2.0, cMeanProb, 0.00001);

        // check identity calculation
        double bBPostProb = (bProb * stats.getAssociatedValueProbabilities().getOrDefault("b",0.0)) / bProb;

        assertEquals(bBPostProb, stats.getAssociatedValuePosteriorProbabilities().get("b"), 0.0001);

    }

}