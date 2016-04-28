package org.granite.classification.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

import org.granite.base.StringTools;
import org.granite.math.MathTools;
import org.granite.math.VectorTools;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
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

    public static double[] asciiPositionVector(final String word) {
        checkNotNull(word, "word");

        final double[] result = new double[26];

        final char[] chars = cleanText(word).toCharArray();

        for (int index = 0; index < chars.length; index++) {

            int currentChar = asciiToInt(chars[index]);

            checkArgument(currentChar >= 0, "Word %s contains a non-ascii character", word);

            if (result[currentChar] > 0.0) {
                continue;
            }

            result[currentChar] = index + 1;

        }

        return result;
    }

    public static double[] asciiFrequencyVector(final String word) {
        checkNotNull(word, "word");

        final double[] result = new double[26];

        final char[] chars = cleanText(word).toCharArray();

        for (int index = 0; index < chars.length; index++) {

            int currentChar = asciiToInt(chars[index]);

            checkArgument(currentChar >= 0, "Word %s contains a non-ascii character", word);

            result[currentChar]++;
        }

        return result;
    }

    static int asciiToInt(final char a){
        return ((int) a) - 97;
    }

    public static double wordSimilarity(final String word1, final String word2){
        final double[] positionVector1 = asciiPositionVector(word1);
        final double[] positionVector2 = asciiPositionVector(word2);
        final double[] frequencyVector1 = asciiFrequencyVector(word1);
        final double[] frequencyVector2 = asciiFrequencyVector(word2);

        return (VectorTools.cosine(positionVector1, positionVector2) +
                VectorTools.cosine(frequencyVector1, frequencyVector2)) / 2.0;
    }
}
