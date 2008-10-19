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
        simple.setConsistency(0.9d);
    }

    @Test public void setEstimate() {
        simple.setEstimate(0.3d);
        assertEquals(simple.getEstimate(), 0.3d, BIG_EPSILON);
    }

    @Test public void setConsistency() {
        simple.setConsistency(0.5d);
        assertEquals(simple.getConsistency(), 0.5d, BIG_EPSILON);
    }

    @Test public void testClone() {
        simple.setEstimate(0.4d);
        simple.setConsistency(0.6d);
        BTS other = simple.clone();
        assertEquals(other.getEstimate(), 0.4d, BIG_EPSILON);
        assertEquals(other.getConsistency(), 0.6d, BIG_EPSILON);
    }

    @Test public void setSample() {
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(simple.getEstimate(), 1.0d, BIG_EPSILON);
        assertTrue(simple.getConsistency() > 0.8d);
        for (int i=0; i<100; i++)
            if (i%2 == 0)
                simple.setSample(1.0d);
            else
                simple.setSample(0.0d);
        for (int i=0; i<10; i++)
            simple.setSample(1.0d);
        assertTrue(simple.getConsistency() < 0.8d);
    }

    @Test public void getConsistencyWith() {
        BTS other = simple.clone();
        assertTrue(simple.getConsistencyWith(other) > 0.5d);
        other.setEstimate(1.0d - simple.getEstimate());
        assertTrue(simple.getConsistencyWith(other) < 0.5d);
    }

}
