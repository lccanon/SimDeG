package simdeg.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class TestBeta {

    private final static double BIG_EPSILON = 0.02d;

    @Test public void beta() {
        Beta b1 = new Beta();
        assertEquals(0.5d, b1.getEstimate(), BIG_EPSILON);
        assertTrue("Error too low: " + b1.getError(), b1.getError() > 0.25d);
        Beta b2 = new Beta(0.0d, 0.01d);
        assertEquals(0.0d, b2.getEstimate(), BIG_EPSILON);
        assertEquals(0.01d, b2.getError(), BIG_EPSILON);
        // The following is slow
        Beta b3 = new Beta(0.2d, 0.02d);
        assertEquals(0.2d, b3.getEstimate(), BIG_EPSILON);
        assertEquals(0.02d, b3.getError(), BIG_EPSILON);
        Beta b4 = new Beta(1.0d, 1.0d, 10.0d, 20.0d);
        assertEquals(15.0d, b4.getEstimate(), BIG_EPSILON);
        assertEquals(100.0d / 12.0d, b4.getVariance(), BIG_EPSILON);
    }

    @Test(expected=IllegalArgumentException.class)
    public void betaException() {
        Beta b4 = new Beta(1.0d, 1.0d, 10.0d, 2.0d);
    }

    @Test public void testClone() {
        Beta simple = new Beta();
        simple.set(0.4d, 0.002d);
        Beta other = simple.clone();
        assertEquals(0.4d, other.getEstimate(), BIG_EPSILON);
        assertEquals(0.002d, other.getVariance(), BIG_EPSILON);
    }

    @Test public void setSample() {
        Beta simple = new Beta();
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(1.0d, simple.getEstimate(), BIG_EPSILON);
        assertTrue("Error too high: " + simple.getError(),
                simple.getError() < 0.01d);
    }

    @Test public void getConsistency() {
        // The following is slow
        Beta simple = new Beta(0.7d);
        Beta other = simple.clone();
        assertTrue(simple.getConsistency(other) > 0.5d);
        other.set(1.0d - simple.getEstimate(), simple.getVariance());
        assertTrue(simple.getConsistency(other) < 0.5d);
    }

    @Test public void set() {
        Beta simple = new Beta();
        simple.set(0.3d, 0.03d);
        assertEquals(0.3d, simple.getEstimate(), BIG_EPSILON);
        assertEquals(0.03d, simple.getVariance(), BIG_EPSILON);
        simple.set(0.0d, 0.0003d);
        assertEquals(0.0d, simple.getEstimate(), BIG_EPSILON);
        assertEquals(0.0003d, simple.getVariance(), BIG_EPSILON);
        simple.set(0.2d, 0.01d);
        assertEquals(0.2d, simple.getEstimate(), BIG_EPSILON);
        assertEquals(0.01d, simple.getVariance(), BIG_EPSILON);
        assertFalse(simple.set(0.3d, 0.3d));
    }

    @Test public void reset() {
        Beta simple = new Beta(1.0d);
        simple.reset();
        assertTrue("Error too low: " + simple.getError(), simple.getError() > 0.25d);
    }

}
