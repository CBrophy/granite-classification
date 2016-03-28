package org.granite.classification.utils;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.granite.base.StringTools;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class LuceneWordBagger {
    private final EnglishAnalyzer englishAnalyzer;

    public LuceneWordBagger() {
        this.englishAnalyzer = new EnglishAnalyzer();
    }

    public LuceneWordBagger(final Set<String> additionalStopWords) {
        checkNotNull(additionalStopWords, "additionalStopWords");
        this.englishAnalyzer = new EnglishAnalyzer(new CharArraySet(additionalStopWords, true));
    }

    public ImmutableSet<String> createWordBag(final String text) {
        if(StringTools.isNullOrEmpty(text)) return ImmutableSet.of();

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
