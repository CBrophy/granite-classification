package org.granite.classification.bayes;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.granite.classification.frequency.FrequencyModel;
import org.granite.classification.frequency.FrequencyModelBuilder;
import org.granite.classification.model.TrainingRow;
import org.granite.classification.model.TrainingSet;
import org.granite.math.ProbabilityTools;
import org.junit.Test;

public class BayesAssociationStatisticsTest {

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
    public void findAssociationLikelihood() throws Exception {
        FrequencyModel<String> model = FrequencyModelBuilder
            .build(new TrainingSet.Builder<Integer, String>()
                .withStrictAssociation(false)
                .withTrainingRows(createTrainingRows())
                .build());

        BayesAssociationStatistics<String> stats = new BayesAssociationStatistics<String>(
            model.getAssociationStatisticsMap().get("b"));

        final double likelihood = stats.findAssociationLikelihood("g");
        // likelihood function
        // L(b:h) = P(b) / P(i ∪ f)

        double prob = 1.0 / 12.0;
        double prob2 = 2.0 / 12.0;
        double prob3 = 3.0 / 12.0;

        assertEquals(
            prob / ProbabilityTools.independentUnion(ImmutableList.of(
                prob2,
                prob3,
                prob,
                prob,
                prob,
                prob,
                prob,
                prob)),
            likelihood,
            0.0001);
    }

}