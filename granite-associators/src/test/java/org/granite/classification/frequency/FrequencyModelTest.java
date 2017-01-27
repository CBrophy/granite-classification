package org.granite.classification.frequency;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.granite.classification.model.AssociationStatistics;
import org.granite.classification.model.TrainingRow;
import org.granite.classification.model.TrainingSet;
import org.junit.Test;

public class FrequencyModelTest {

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
    public void meanProbability() throws Exception {
        final FrequencyModel<String> model = FrequencyModelBuilder.build(
            TrainingSet.build(createTrainingRows())
        );

        final AssociationStatistics<String> stats = model
            .getAssociationStatisticsMap().get("b");

        double bProb = (3.0 / 16.0);
        double gProb = (1.0 / 16.0);
        double aProb = (2.0 / 16.0);
        double cProb = 2.0 / 16.0;

        assertEquals(bProb, stats.getProbability(), 0.0001);

        double bCProb = 2.0 / 9.0;
        // P(C:B) = 2.0 / 9.0
        assertEquals(bCProb, stats.getAssociatedValueProbabilities().get("c"), 0.0001);

        double aCProb = 1.0 / 6.0;
        double bGProb = 1.0 / 9.0;

        double cMeanProb = model.meanProbability("c", ImmutableList.of("b", "a"));
        double gMeanProb = model.meanProbability("g", ImmutableList.of("b", "a"));

        double bCAssocProb = (cProb * bProb) / bCProb;
        double aCAssocProb = (cProb * aProb) / aCProb;
        double bGAssocProb = (gProb * bProb) / bGProb;

        assertEquals(
            (bCAssocProb + aCAssocProb) / 2.0, cMeanProb, 0.0001
        );

        // no association in training set so prob is zero
        //double aGAssocProb = (gProb * aProb) / aGProb;

        assertEquals(
            bGAssocProb / 2.0, gMeanProb, 0.0001
        );
    }

}