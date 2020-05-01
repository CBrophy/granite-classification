package org.granite.nlp.phrases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * User: cbrophy Date: 5/26/17 Time: 10:33 AM
 */
public class IdentityPhraseTest {

  @Test
  public void componentize() throws Exception {

    final HashMap<String, UUID> testMap = new HashMap<>();

    testMap.put("quick", UUID.randomUUID());
    testMap.put("brown", UUID.randomUUID());
    testMap.put("fox", UUID.randomUUID());
    testMap.put("lazy", UUID.randomUUID());
    testMap.put("dog", UUID.randomUUID());

    final Phrase phrase = IdentityPhrase.of(testMap.values());

    final List<Phrase> componentPaths = phrase
        .componentize(3);

    // sum(nCr)
    // n = 5
    // nCr for r = 3 = 10
    // nCr for r = 2 = 10
    // nCr for r = 1 = 5

    assertEquals(25, componentPaths.size());

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

    assertEquals(10, threeCount.get());
    assertEquals(10, twoCount.get());
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

    final IdentityPhrase p1 = IdentityPhrase.of(l1);
    final IdentityPhrase p2 = IdentityPhrase.of(l2);
    final IdentityPhrase p3 = IdentityPhrase.of(l3);

    assertTrue(p2.isComponentOf(p1));
    assertFalse(p1.isComponentOf(p2));
    assertFalse(p3.isComponentOf(p1));
  }

}