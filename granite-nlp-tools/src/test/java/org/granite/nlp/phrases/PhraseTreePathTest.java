package org.granite.nlp.phrases;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * User: cbrophy
 * Date: 5/26/17
 * Time: 10:33 AM
 */
public class PhraseTreePathTest {

  @Test
  public void componentize() throws Exception {

    final HashMap<String, UUID> testMap = new HashMap<>();

    testMap.put("quick", UUID.randomUUID());
    testMap.put("brown", UUID.randomUUID());
    testMap.put("fox", UUID.randomUUID());
    testMap.put("lazy", UUID.randomUUID());
    testMap.put("dog", UUID.randomUUID());

    final Map<UUID, String> inverseMap = testMap
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    final PhraseTreePath phraseTreePath = PhraseTreePath.of(testMap.values());

    final List<PhraseTreePath> componentPaths = phraseTreePath
        .componentize(3);

    //expected size = sum(n! / ((n - r)! * r!)) for all r
    // n = 5
    // r = 3,2,1
    // n! = 120
    // 3! = 6
    // 2! = 2
    // size for 3 = 10
    // size for 2 = 10
    // size for 1 = 5
    // sum = 25
    assertEquals(25, componentPaths.size());

    final AtomicInteger threeCount = new AtomicInteger(0);
    final AtomicInteger twoCount = new AtomicInteger(0);
    final AtomicInteger oneCount = new AtomicInteger(0);
    final AtomicInteger otherCount = new AtomicInteger(0);

    componentPaths
        .forEach(path -> {
          if(path.getIdentitySet().size() == 3){
            threeCount.incrementAndGet();
          } else if(path.getIdentitySet().size() == 2){
            twoCount.incrementAndGet();
          } else if(path.getIdentitySet().size() == 1){
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

}