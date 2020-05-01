package org.granite.nlp.phrases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.granite.collections.ListTools;
import org.junit.Test;

/**
 * User: cbrophy Date: 5/30/17 Time: 12:18 PM
 */
public class OrderedPhraseTest {

  @Test
  public void testComponentize() {
    final HashMap<String, UUID> testMap = new HashMap<>();

    testMap.put("quick", UUID.randomUUID());
    testMap.put("brown", UUID.randomUUID());
    testMap.put("fox", UUID.randomUUID());
    testMap.put("lazy", UUID.randomUUID());
    testMap.put("dog", UUID.randomUUID());

    final Phrase phrase = OrderedPhrase.of(testMap.values());

    final List<Phrase> componentPaths = phrase
        .componentize(3);

    // sum(nPr)
    // n = 5
    // nPr for r = 3 = 60
    // nPr for r = 2 = 20
    // nPr for r = 1 = 5

    assertEquals(85, componentPaths.size());

    final AtomicInteger threeCount = new AtomicInteger(0);
    final AtomicInteger twoCount = new AtomicInteger(0);
    final AtomicInteger oneCount = new AtomicInteger(0);
    final AtomicInteger otherCount = new AtomicInteger(0);

    componentPaths
        .forEach(path -> {
          if (path.getIdentitySet().size() == 3) {
            threeCount.incrementAndGet();
          } else if (path.getIdentitySet().size() == 2) {
            twoCount.incrementAndGet();
          } else if (path.getIdentitySet().size() == 1) {
            oneCount.incrementAndGet();
          } else {
            otherCount.incrementAndGet();
          }
        });

    assertEquals(60, threeCount.get());
    assertEquals(20, twoCount.get());
    assertEquals(5, oneCount.get());
    assertEquals(0, otherCount.get());
  }

  @Test
  public void testComponentOf() {
    final ImmutableList<UUID> l1 = ImmutableList.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID()
    );

    final ImmutableList<UUID> l2 = ImmutableList.of(
        l1.get(2),
        l1.get(3),
        l1.get(4)
    );

    final ImmutableList<UUID> l3 = ImmutableList.of(
        l1.get(2),
        l1.get(3),
        l1.get(4),
        UUID.randomUUID()
    );

    final ImmutableList<UUID> l4 = ImmutableList.of(
        l1.get(4),
        l1.get(3),
        l1.get(2)
    );

    final OrderedPhrase p1 = OrderedPhrase.of(l1);
    final OrderedPhrase p2 = OrderedPhrase.of(l2);
    final OrderedPhrase p3 = OrderedPhrase.of(l3);
    final OrderedPhrase p4 = OrderedPhrase.of(l4);

    assertTrue(p2.isComponentOf(p1));
    assertFalse(p1.isComponentOf(p2));
    assertFalse(p3.isComponentOf(p1));
    assertFalse(p4.isComponentOf(p1));
  }

  @Test
  public void testExtractOrderedComponent() {
    final HashMap<String, UUID> testMap = new HashMap<>();

    testMap.put("quick", UUID.randomUUID());
    testMap.put("brown", UUID.randomUUID());
    testMap.put("fox", UUID.randomUUID());
    testMap.put("lazy", UUID.randomUUID());
    testMap.put("dog", UUID.randomUUID());

    final ImmutableList<UUID> orderedExpression = ImmutableList.of(
        testMap.get("quick"),
        testMap.get("brown"),
        testMap.get("fox"),
        testMap.get("lazy"),
        testMap.get("dog")
    );

    final Phrase phrase = OrderedPhrase.of(orderedExpression);

    final IdentityPhrase componentPhrase = IdentityPhrase.of(
        ImmutableList.of(
            testMap.get("dog"),
            testMap.get("brown"),
            testMap.get("lazy")
        )
    );

    final ImmutableList<UUID> expected = ImmutableList.of(
        testMap.get("brown"),
        testMap.get("lazy"),
        testMap.get("dog")
    );

    final OrderedPhrase orderedComponent = OrderedPhrase
        .extractOrderedComponent(
            componentPhrase,
            phrase
        );

    assertNotNull(orderedComponent);

    assertTrue(ListTools.listsMatch(expected, orderedComponent.getOrderedPath()));
  }

}