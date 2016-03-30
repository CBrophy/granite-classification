package org.granite.classification.utils;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

public class MapUtils {

    public static ImmutableMap<String, ImmutableMap<String, Double>> buildImmutableCopy(
            final HashMap<String, HashMap<String, Double>> sourceMap
    ) {
        final ImmutableMap.Builder<String, ImmutableMap<String, Double>> builder = ImmutableMap.builder();

        sourceMap
                .entrySet()
                .forEach(entry -> builder.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue())));

        return builder.build();
    }
}
