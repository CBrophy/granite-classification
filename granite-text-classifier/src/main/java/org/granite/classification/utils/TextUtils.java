package org.granite.classification.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TextUtils {

    public static String cleanText(final String... text) {
        checkNotNull(text, "text");

        if (text.length == 0) {
            return "";
        }

        return cleanText(Joiner.on(' ').skipNulls().join(text));
    }

    public static String cleanText(final String wildText) {
        checkNotNull(wildText, "wildText");

        if (wildText.isEmpty()) {
            return "";
        }

        // Turn everything that is not a lower-case letter or number
        // into a space and collapse any groups to a single space
        return CharMatcher
                .javaLetter()
                .negate()
                .collapseFrom(wildText
                        .toLowerCase(), ' ').trim();
    }

    public static void updateFrequencyMap(final Iterable<String> words, final Map<String, Integer> frequencyMap) {
        checkNotNull(words, "words");
        checkNotNull(frequencyMap, "frequencyMap");

        checkNotNull(words, "words");

        words
                .forEach(word -> {
                    frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
                });
    }

}
