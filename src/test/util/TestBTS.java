package simdeg.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class TestBTS {

    private BTS simple;

    private final static double BIG_EPSILON = 0.1d;

    @Before public void initialize() {
        simple = new BTS();
        simple.setEstimate(0.1d);
        simple.setError(0.1d);
    }

    @Test public void setEstimate() {
        simple.setEstimate(0.3d);
        assertEquals(0.3d, simple.getEstimate(), BIG_EPSILON);
    }

    @Test public void setError() {
        simple.setError(0.5d);
        assertEquals(0.5d, simple.getError(), BIG_EPSILON);
    }

    @Test public void testClone() {
        simple.setEstimate(0.4d);
        simple.setError(0.4d);
        BTS other = simple.clone();
        assertEquals(0.4d, other.getEstimate(), BIG_EPSILON);
        assertEquals(0.4d, other.getError(), BIG_EPSILON);
    }

    @Test public void setSample() {
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(1.0d, simple.getEstimate(), BIG_EPSILON);
        assertTrue("Error too high: " + simple.getError(),
                simple.getError() < 0.2d);
        for (int i=0; i<100; i++)
            if (i%2 == 0)
                simple.setSample(1.0d);
            else
                simple.setSample(0.0d);
        assertEquals(0.5d, simple.getEstimate(), BIG_EPSILON);
        for (int i=0; i<10; i++)
            simple.setSample(1.0d);
        assertTrue("Error too low: " + simple.getError(),
                simple.getError() > 0.2d);
    }

    @Test public void getConsistency() {
        BTS other = simple.clone();
        assertTrue(simple.getConsistency(other) > 0.5d);
        other.setEstimate(1.0d - simple.getEstimate());
        assertTrue(simple.getConsistency(other) < 0.5d);
    }

}
