package org.granite.classification.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class TextUtilsTest {

    @Test
    public void testAsciiPositionVector() throws Exception {
        double[] vector = TextUtils.asciiPositionVector("alighted");

        assertEquals(vector[TextUtils.asciiToInt('a')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('l')], 2.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('i')], 3.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('g')], 4.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('h')], 5.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('t')], 6.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('e')], 7.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('d')], 8.0, 0.0001);

        double[] vector2 = TextUtils.asciiPositionVector("8x11");

        assertEquals(vector2[TextUtils.asciiToInt('x')], 1.0, 0.0001);
        try {
            TextUtils.asciiPositionVector("this word");
        }catch (Exception e){
            assertTrue(e instanceof IllegalArgumentException);
        }



    }

    @Test
    public void testAsciiFrequencyVector() throws Exception {
        double[] vector = TextUtils.asciiFrequencyVector("alighted");

        assertEquals(vector[TextUtils.asciiToInt('a')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('l')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('i')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('g')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('h')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('t')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('e')], 1.0, 0.0001);
        assertEquals(vector[TextUtils.asciiToInt('d')], 1.0, 0.0001);

        double[] vector2 = TextUtils.asciiFrequencyVector("bookkeeper");

        assertEquals(vector2[TextUtils.asciiToInt('b')], 1.0, 0.0001);
        assertEquals(vector2[TextUtils.asciiToInt('o')], 2.0, 0.0001);
        assertEquals(vector2[TextUtils.asciiToInt('k')], 2.0, 0.0001);
        assertEquals(vector2[TextUtils.asciiToInt('e')], 3.0, 0.0001);
        assertEquals(vector2[TextUtils.asciiToInt('p')], 1.0, 0.0001);
        assertEquals(vector2[TextUtils.asciiToInt('r')], 1.0, 0.0001);

    }

}