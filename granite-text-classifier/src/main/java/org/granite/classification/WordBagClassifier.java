package org.granite.classification;

import com.google.common.collect.ImmutableSet;

import org.granite.classification.model.ClassificationScore;
import org.granite.classification.model.ScoreEnsembler;
import org.granite.classification.model.ScoreFilter;
import org.granite.classification.model.StringScore;
import org.granite.classification.model.TrainingSet;
import org.granite.classification.model.WordBagger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WordBagClassifier {

    private final WordBagger wordBagger;
    private ScoreEnsembler<ClassificationScore> scoreEnsembler;
    private ScoreFilter<StringScore> contributorScoreFilter;
    private ScoreFilter<ClassificationScore> scoreFilter;

    protected WordBagClassifier(final WordBagger wordBagger) {
        this.wordBagger = checkNotNull(wordBagger, "wordBagger");
    }

    public WordBagger getWordBagger() {
        return wordBagger;
    }

    public ImmutableSet<ClassificationScore> classify(final String text) {
        return classify(getWordBagger().generateWordBag(text));
    }

    public ImmutableSet<ClassificationScore> classify(final Set<String> wordBag) {
        ImmutableSet<ClassificationScore> result = this.doClassify(wordBag);

        if(contributorScoreFilter != null){
            for (ClassificationScore classificationScore : result) {

                ImmutableSet<StringScore> filtered = contributorScoreFilter.filter(classificationScore.getContributors());
                classificationScore.getContributors().clear();
                classificationScore.getContributors().addAll(filtered);

            }

        }

        if(scoreEnsembler != null){
            result = scoreEnsembler.ensemble(result);
        }

        if(scoreFilter != null){
            result = scoreFilter.filter(result);
        }

        return result;
    }

    protected ImmutableSet<ClassificationScore> convertHashMap(final HashMap<String, HashMap<String, Double>> scoreMap) {
        checkNotNull(scoreMap, "scoreMap");

        final HashSet<ClassificationScore> result = new HashSet<>();

        for (Map.Entry<String, HashMap<String, Double>> classificationEntry : scoreMap.entrySet()) {
            final ClassificationScore classificationScore = new ClassificationScore(classificationEntry.getKey());
            result.add(classificationScore);

            for (Map.Entry<String, Double> wordEntry : classificationEntry.getValue().entrySet()) {
                final StringScore wordScore = new StringScore(wordEntry.getKey(), wordEntry.getValue());
                classificationScore.getContributors().add(wordScore);
            }

        }

        return ImmutableSet.copyOf(result);
    }

    public WordBagClassifier withScoreEnsembler(final ScoreEnsembler<ClassificationScore> scoreEnsembler){
        this.scoreEnsembler = scoreEnsembler;
        return this;
    }

    public WordBagClassifier withScoreFilter(final ScoreFilter<ClassificationScore> scoreFilter){
        this.scoreFilter = scoreFilter;
        return this;
    }

    public WordBagClassifier withContributorScoreFilter(final ScoreFilter<StringScore> scoreFilter){
        this.contributorScoreFilter = scoreFilter;
        return this;
    }

    public abstract void train(final TrainingSet trainingSet);

    public abstract Set<String> getClassifications();

    public abstract double getMaxClassificationScore(final String classification);

    protected abstract ImmutableSet<ClassificationScore> doClassify(final Set<String> wordBag);


}
