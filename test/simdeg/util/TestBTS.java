package simdeg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestBTS {

    private final static double BIG_EPSILON = 1E-2d;
    private final static double EPSILON = 1E-5d;

    @Test public void testClone() {
        BTS simple = new BTS();
        simple.set(0.0d, 1.0d, 0.4d, 0.04d);
        BTS other = simple.clone();
        assertEquals(0.4d, other.getMean(), EPSILON);
        assertEquals(0.04d, other.getVariance(), EPSILON);
    }

    @Test public void reinit() {
        BTS simple = new BTS();
        /* Bernoulli probability's is 1 */
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(1.0d, simple.getMean(), BIG_EPSILON);
        /* Bernoulli probability's is 0.5 */
        for (int i=0; i<100; i++)
            if (i%2 == 0)
                simple.setSample(1.0d);
            else
                simple.setSample(0.0d);
        assertEquals(0.5d, simple.getMean(), EPSILON);
        /* Bernoulli probability's is 1 again */
        for (int i=0; i<10; i++)
            simple.setSample(1.0d);
        assertTrue("Estimate should have been reinit " + simple.getMean(),
                simple.getMean() > 0.5d + BIG_EPSILON);
    }

}
