package org.granite.classification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.TrainingSetFilter;
import org.granite.classification.model.TrainingText;
import org.granite.log.LogTools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class WordFrequencyTrainingSetFilter extends TrainingSetFilter {

    private final HashSet<String> wordFilters = new HashSet<>();
    private final HashMultimap<String, String> wordClassificationFilters = HashMultimap.create();
    private final int minimumFrequency;

    public WordFrequencyTrainingSetFilter(final int minimumFrequency){
        this.minimumFrequency = minimumFrequency;
    }

    @Override
    public void generateFilters(final ImmutableMap<Integer, TrainingText> trainingTexts){
        checkNotNull(trainingTexts, "trainingTexts");

        final HashMap<String, HashMap<String, Double>> wordClassificationCounts = new HashMap<>();

        // First pass counts raw words and classifications
        for (TrainingText trainingText : trainingTexts.values()) {

            trainingText
                    .getClassifications()
                    .forEach(classification -> {

                        // Count the number of occurrences per word per classification
                        // and the total number of words per classification

                        trainingText
                                .getWordBag()
                                .forEach(word -> {

                                    wordClassificationCounts
                                            .computeIfAbsent(word, key -> new HashMap<>())
                                            .merge(classification, 1.0, (v1, v2) -> v1 + v2);

                                });

                    });

        }

        // Second pass to create word filters based on word occurrence

        for (Map.Entry<String, HashMap<String, Double>> wordEntry : wordClassificationCounts.entrySet()) {

            final String word = wordEntry.getKey();

            for (Map.Entry<String, Double> classificationEntry : wordEntry.getValue().entrySet()) {

                final String classification = classificationEntry.getKey();

                if (classificationEntry.getValue() < minimumFrequency) {
                    wordClassificationFilters.put(word, classification);
                }
            }

            // If the word is filtered from all of its classifications, add it to the whole word filter
            if (wordClassificationFilters.get(word).size()  == wordClassificationCounts.get(word).size()) {
                wordFilters.add(word);
            }

        }

        LogTools.info("Filtering {0} words from specific classifications due to too low frequency", String.valueOf(wordClassificationFilters.size()));

        LogTools.info("Filtering {0} from all classifications due to too low frequency", String.valueOf(wordFilters.size()));

        super.generateFilters(trainingTexts);
    }

    @Override
    protected ImmutableSet<String> generateWordFilter(final ImmutableMap<Integer, TrainingText> trainingTexts) {
        return ImmutableSet.copyOf(wordFilters);
    }

    @Override
    protected ImmutableMultimap<String, String> generateWordClassificationFilter(final ImmutableMap<Integer, TrainingText> trainingTexts) {
        return ImmutableMultimap.copyOf(wordClassificationFilters);
    }
}
