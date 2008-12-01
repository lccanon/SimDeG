package simdeg.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class TestBTS {

    private final static double BIG_EPSILON = 0.02d;

    @Test public void testClone() {
        BTS simple = new BTS();
        simple.set(0.4d, 0.04d);
        BTS other = simple.clone();
        assertEquals(0.4d, other.getEstimate(), BIG_EPSILON);
        assertEquals(0.4d, other.getError(), BIG_EPSILON);
    }

    @Test public void reinit() {
        BTS simple = new BTS();
        /* Bernoulli probability's is 1 */
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(1.0d, simple.getEstimate(), BIG_EPSILON);
        /* Bernoulli probability's is 0.5 */
        for (int i=0; i<100; i++)
            if (i%2 == 0)
                simple.setSample(1.0d);
            else
                simple.setSample(0.0d);
        assertEquals(0.5d, simple.getEstimate(), BIG_EPSILON);
        /* Bernoulli probability's is 1 again */
        for (int i=0; i<10; i++)
            simple.setSample(1.0d);
        assertTrue("Estimate should have been reinit " + simple.getEstimate(),
                simple.getEstimate() > 0.5d + BIG_EPSILON);
    }

}
