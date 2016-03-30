package org.granite.classification.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.granite.classification.LuceneWordBagger;
import org.junit.Test;

import static org.junit.Assert.*;

public class LuceneWordBaggerTest {

    @Test
    public void testCreateWordBag() throws Exception {

        final String test = "The quick jumping brown fox jumped quick over the lazy, lazy dog";

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger(ImmutableSet.of("the","of"));

        final ImmutableSet<String> wordBag = luceneWordBagger.generateWordBag(test);

        assertEquals(7, wordBag.size());

        assertTrue(wordBag.containsAll(ImmutableList.of("quick", "brown", "fox", "dog", "lazi", "jump", "over")));

    }
}