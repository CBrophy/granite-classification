package org.granite.classification.utils;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapUtils {

    public static ImmutableMap<String, Map<String, Double>> buildImmutableCopy(
            final Map<String, Map<String, Double>> sourceMap
    ) {
        checkNotNull(sourceMap, "sourceMap");

        final ImmutableMap.Builder<String, Map<String, Double>> builder = ImmutableMap.builder();

        sourceMap
                .entrySet()
                .forEach(entry -> builder.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue())));

        return builder.build();
    }
}
