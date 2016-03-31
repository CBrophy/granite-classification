package org.granite.classification.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.TrainingSetFactory;

public abstract class TrainingSetFilter {
    private ImmutableSet<String> wordFilter = ImmutableSet.of();
    private ImmutableMultimap<String, String> wordClassificationFilter = ImmutableMultimap.of();

    public void generateFilters(final ImmutableMap<Integer, TrainingText> trainingTexts){
        final ImmutableSet<String> wordFilter = generateWordFilter(trainingTexts);
        final ImmutableMultimap<String, String> wordClassificationFilter = generateWordClassificationFilter(trainingTexts);

        if(wordFilter != null){
            this.wordFilter = wordFilter;
        }

        if(wordClassificationFilter != null){
            this.wordClassificationFilter = wordClassificationFilter;
        }
    }

    protected abstract ImmutableSet<String> generateWordFilter(final ImmutableMap<Integer, TrainingText> trainingTexts);
    protected abstract ImmutableMultimap<String, String> generateWordClassificationFilter(final ImmutableMap<Integer, TrainingText> trainingTexts);

    public ImmutableSet<String> getWordFilter() {
        return wordFilter;
    }

    public ImmutableMultimap<String, String> getWordClassificationFilter() {
        return wordClassificationFilter;
    }
}
