package org.granite.classification.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

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
                .javaLetterOrDigit()
                .negate()
                .collapseFrom(wildText
                        .toLowerCase()
                        .trim(), ' ');
    }

}
