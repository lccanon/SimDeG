package simdeg.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestBeta {

    private final static double BIG_EPSILON = 1E-2d;
    private final static double EPSILON = 1E-5d;
    private final static double ERROR_THRESHOLD = 0.25d;

    @Test public void beta() {
        new Beta();
        new Beta(0.0d);
        new Beta(0.2d);
        new Beta(10.0d, 20.0d, 1.0d, 1.0d);
    }

    @Test(expected=IllegalArgumentException.class)
    public void betaException() {
        new Beta(10.0d, 20.0d, -1.0d, 1.0d);
    }

    @Test public void getMean() {
        Beta b1 = new Beta();
        assertEquals(0.5d, b1.getMean(), EPSILON);
        Beta b2 = new Beta(0.0d);
        assertEquals(0.0d, b2.getMean(), BIG_EPSILON);
        Beta b3 = new Beta(0.2d);
        assertEquals(0.2d, b3.getMean(), EPSILON);
        Beta b4 = new Beta(10.0d, 20.0d, 1.0d, 1.0d);
        assertEquals(15.0d, b4.getMean(), EPSILON);
    }

    @Test public void getVariance() {
        Beta b4 = new Beta(10.0d, 20.0d, 1.0d, 1.0d);
        assertEquals(100.0d / 12.0d, b4.getVariance(), EPSILON);
    }

    @Test public void getError() {
        /* Test default initialization */
        Beta b1 = new Beta();
        assertTrue("Error too low: " + b1.getError(),
                b1.getError() > ERROR_THRESHOLD);
        /* Test with high precision */
        Beta b2 = new Beta(0.0d);
        assertTrue("Error too high: " + b2.getError(),
                b2.getError() < BIG_EPSILON);
        /* Test the validity of gaussian method for computing error in another
         * range */
        Beta b5 = new Beta(10.0d, 20.0d, 100.0d, 100000.0d);
        assertTrue("Error too high: " + b5.getError(),
                b5.getError() < BIG_EPSILON);
        /* Test with a high variance in a large range */
        Beta b6 = new Beta(0.0d, 100.0d, 1.0d, 10.0d);
        assertTrue("Error out of range: " + b6.getError(),
                b6.getError() < 25.0d && b6.getError() > 10.0d);
        /* Test with negative value (standard approach) */
        Beta b7 = new Beta(-1.0d, 0.0d, 1.0d, 20.0d);
        assertTrue("Error too high: " + b7.getError(),
                b7.getError() < ERROR_THRESHOLD);
        /* Test with negative value (normal approach) */
        Beta b8 = new Beta(-1.0d, 0.0d, 1.0d, 200.0d);
        assertTrue("Error too high: " + b8.getError(),
                b8.getError() < BIG_EPSILON);
    }

    @Test public void testClone() {
        Beta simple = new Beta();
        simple.set(0.0d, 1.0d, 0.4d, 0.001d);
        Beta other = simple.clone();
        assertEquals(0.4d, other.getMean(), EPSILON);
        assertEquals(0.001d, other.getVariance(), EPSILON);
    }

    @Test public void set() {
        Beta simple = new Beta();
        /* Standard case */
        simple.set(0.0d, 1.0d, 0.3d, 0.03d);
        assertEquals(0.3d, simple.getMean(), EPSILON);
        assertEquals(0.03d, simple.getVariance(), EPSILON);
        /* Lower limit for the variance */
        simple.set(0.0d, 1.0d, 0.01d, 0.0000001d);
        assertEquals(0.01d, simple.getMean(), EPSILON);
        assertEquals(Beta.DEFAULT_VARIANCE, simple.getVariance(), EPSILON);
        /* Feasible variance but mean too low */
        simple.set(0.0d, 1.0d, 0.01d, 0.1d);
        assertTrue("Mean out of range: " + simple.getMean(),
                simple.getMean() > 0.01d && simple.getMean() < 0.5d);
        assertEquals(0.1d, simple.getVariance(), EPSILON);
        /* Upper limit for the variance */
        simple.set(0.0d, 1.0d, 0.5d, 0.8d);
        assertEquals(0.5d, simple.getMean(), EPSILON);
        assertEquals(0.25d, simple.getVariance(), EPSILON);
    }

}
