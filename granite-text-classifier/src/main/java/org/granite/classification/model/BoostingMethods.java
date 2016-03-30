package org.granite.classification.model;

import com.google.common.collect.ImmutableMap;

import org.granite.classification.WordBagClassifier;

import java.util.function.Function;

public final class BoostingMethods {

    public static Function<WordBagClassifier, ImmutableMap<String, ImmutableMap<String, Double>>> MULTIPASS_BOOSTING = wordBagClassifier -> {
      return null;
    };

}
