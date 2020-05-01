package org.granite.nlp.phrases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class PhraseTreeTest {

  @Test
  public void testPhraseTree() {
    final String phrase = "the quick brown fox jumped over the lazy dog";
    final String phrase2 = "the lazy dog jumped over the quick brown fox";

    final HashPhraseTree phraseTree = new HashPhraseTree(
        ImmutableSet.of("the"),
        ImmutableSet.of(),
        list -> list
    );

    final Phrase path1 = phraseTree.computeIfAbsent(phrase);
    final Phrase path2 = phraseTree.computeIfAbsent(phrase2);
    Phrase path3 = null;

    for (Phrase phraseTreePath : phraseTree.getAlternativePaths().get(path1)) {
      path3 = phraseTreePath;
      break;
    }

    assertNotNull(path3);

    assertEquals("quick brown fox jumped over lazy dog", phraseTree.getPhraseText(path1));
    assertEquals("quick brown fox jumped over lazy dog", phraseTree.getPhraseText(path2));
    assertEquals("lazy dog jumped over quick brown fox", phraseTree.getPhraseText(path3));
  }

  @Test
  public void testPhraseComponents() {
    final String phrase = "the quick brown fox";
    final String phrase2 = "the brown quick fox";

    final HashPhraseTree phraseTree = new HashPhraseTree(
        ImmutableSet.of("the"),
        ImmutableSet.of(),
        list -> list
    );

    final Phrase path1 = phraseTree.computeIfAbsent(phrase);
    final Phrase path2 = phraseTree.computeIfAbsent(phrase2);

    final Map<Phrase, List<Phrase>> orderedComponentMap = phraseTree
        .generateOrderedComponentMap(2);

    assertEquals(9, orderedComponentMap.size());

    final ImmutableSet<String> componentSet = ImmutableSet.of(
        "quick",
        "brown",
        "fox",
        "quick brown",
        "brown fox",
        "quick fox",
        "brown quick",
        "fox brown",
        "fox quick"
    );

    final Set<String> generatedOrderedComponents = orderedComponentMap
        .keySet()
        .stream()
        .map(phraseTree::getPhraseText)
        .collect(Collectors.toSet());

    final SetView<String> orderedMatched = Sets
        .intersection(componentSet, generatedOrderedComponents);

    assertEquals(9, orderedMatched.size());

    final Map<Phrase, List<Phrase>> identityComponentMap = phraseTree
        .generateIdentityComponentMap(2);

    assertEquals(6, identityComponentMap.size());

    final Set<String> generatedIdentityComponents = identityComponentMap
        .keySet()
        .stream()
        .map(phraseTree::getIdentityPhraseText)
        .collect(Collectors.toSet());

    final SetView<String> identityMatched = Sets
        .intersection(componentSet, generatedIdentityComponents);

    assertEquals(6, identityMatched.size());


  }
}