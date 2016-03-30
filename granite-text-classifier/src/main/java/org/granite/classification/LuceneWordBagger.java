package org.granite.classification;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.granite.base.StringTools;
import org.granite.classification.model.WordBagger;
import org.granite.classification.utils.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;


public class LuceneWordBagger extends WordBagger {
    private final EnglishAnalyzer englishAnalyzer;

    public LuceneWordBagger(final File stopWordFile){
        this(loadStopWordFile(stopWordFile));
    }

    public LuceneWordBagger(final Iterable<String> additionalStopWords) {
        super(additionalStopWords);

        this.englishAnalyzer = new EnglishAnalyzer(new CharArraySet(getStopWords(), true));
    }

    @Override
    public ImmutableSet<String> generateWordBag(String text) {
        if (StringTools.isNullOrEmpty(text)) return ImmutableSet.of();

        final HashSet<String> result = new HashSet<>();

        final String cleaned = TextUtils.cleanText(text);

        try (final PorterStemFilter tokenStream = (PorterStemFilter) englishAnalyzer.tokenStream(null, cleaned)) {

            final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                final String currentTerm = charTermAttribute.toString();
                if (!StringTools.isNullOrEmpty(currentTerm)) result.add(currentTerm);
            }

            tokenStream.end();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return ImmutableSet.copyOf(result);
    }
}
