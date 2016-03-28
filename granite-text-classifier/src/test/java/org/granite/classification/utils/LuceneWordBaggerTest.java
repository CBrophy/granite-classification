package org.granite.classification.utils;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import static org.junit.Assert.*;

public class LuceneWordBaggerTest {

    @Test
    public void testCreateWordBag() throws Exception {
        final String test = "When the former Darth Revan disappeared from known space, she left a lover, lovers and a loved child behind. After their daughter is taken by the Jedi, Carth takes drastic measures that catapult him into the turmoil of the Galactic Cold War and reunites them against impossible odds. In a galaxy that still trembles where she walks, how could one man's influence change the course of the future?";

        final LuceneWordBagger luceneWordBagger = new LuceneWordBagger();

        final ImmutableSet<String> wordBag = luceneWordBagger.createWordBag(test);

        assertEquals(45, wordBag.size());

    }
}