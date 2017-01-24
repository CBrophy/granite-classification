package org.granite.classification.bayes;

import com.google.common.collect.ImmutableList;

import org.granite.classification.model.TrainingRow;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class NaiveBayesModelTest {

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

        final NaiveBayesModel<String> model = (NaiveBayesModel<String>) new NaiveBayesModelBuilder<Integer, String>()
                .withTrainingRows(createTrainingRows())
                .build();

        final Map<String, Double> result = model.meanScoreObservation(ImmutableList
                                                                              .of("c",
                                                                                  "h",
                                                                                  "a",
                                                                                  "n"));

        assertEquals(4, result.size());
    }

}