package org.granite.nlp.phrases;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

/**
 * User: cbrophy
 * Date: 5/19/17
 * Time: 2:35 PM
 */
public class PhraseTreePathTest {

  @Test
  public void extractComponentPhraseTreePaths() throws Exception {
    final ImmutableList<UUID> source = ImmutableList
        .of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID());

    final PhraseTreePath path1 = PhraseTreePath
        .of(source);

    final Set<PhraseTreePath> components = path1.extractComponentPhraseTreePaths();


    //TODO

  }

}