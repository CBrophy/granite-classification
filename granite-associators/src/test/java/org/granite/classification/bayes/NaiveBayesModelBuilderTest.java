package org.granite.classification.bayes;

import com.google.common.collect.ImmutableList;

import org.granite.classification.model.TrainingRow;
import org.granite.math.ProbabilityTools;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class NaiveBayesModelBuilderTest {

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
    public void build() throws Exception {
        final NaiveBayesModel<String> model = (NaiveBayesModel<String>) new NaiveBayesModelBuilder<Integer, String>()
                .withTrainingRows(createTrainingRows())
                .build();

        assertEquals(9, model.getValueStatisticsMap().size());
        assertEquals(16, (int) model.getTotalValueFrequency());

        final ValueStatistics<String> valueStatistics = model.getValueStatisticsMap().get("b");

        // probability = 3 / 16 = 0.1875

        assertEquals(3.0 / 16.0, valueStatistics.getProbability(), 0.00001);

        // likelihood = p(B) / U(P(!B))

        final List<Double> otherProbabilities = model.getValueStatisticsMap()
                                                     .values()
                                                     .stream()
                                                     .filter(stats -> !"b".equals(stats.getValue()))
                                                     .map(ValueStatistics::getProbability)
                                                     .collect(Collectors.toList());

        assertEquals(
                (3.0 / 16.0) / ProbabilityTools.independentUnion(otherProbabilities),
                valueStatistics.getLikelihood(),
                0.00001
        );

        assertEquals(3, (int) valueStatistics.getFrequency());
        assertEquals(9, (int) valueStatistics.getAssociationFrequency());
        assertEquals(8,
                     valueStatistics.getAssociatedValueProbabilities().size()); // "c" occurs twice
        assertEquals(8, valueStatistics.getAssociatedValuePosteriorProbabilities().size());
        // P(c:b) = 2.0 / 9.0 = 0.222...
        assertEquals((2.0 / 9.0),
                     valueStatistics.getAssociatedValueProbabilities().get("c"),
                     0.0001);

        // P(b:c) = (P(b) * (P(c:b)/Union(P(C:x))) / P(C)
        // P(C) = 2 / 16 = 0.125
        //        = (0.1875 * (0.222/U(P(b:!c)))) / 0.125

        final List<Double> otherAssociatedProbabilities = valueStatistics
                .getAssociatedValueProbabilities()
                .entrySet()
                .stream()
                .filter(entry -> !"c".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        assertEquals((0.1875 *
                              (0.2222222 /
                                       ProbabilityTools.independentUnion(
                                               otherAssociatedProbabilities))) / (2.0 / 16.0),
                     valueStatistics.getAssociatedValuePosteriorProbabilities().get("c"), 0.0001);
    }

}