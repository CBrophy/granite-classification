package org.granite.classification.model;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;


public class TrainingSetTest {

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
        final TrainingSet<Integer, String> trainingSet = TrainingSet.build(createTrainingRows());

        assertEquals(16, (int) trainingSet.getTotalValueFrequency());
        assertEquals(3, (int) ((double) trainingSet.getValueFrequency().get("b")));
        assertEquals(2, (int) ((double) trainingSet.getValueToValueFrequency().get("b").get("c")));
        assertEquals(3, (int) ((double) trainingSet.getValueToValueFrequency().get("b").get("b")));

    }

}