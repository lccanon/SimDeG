package simdeg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestRV {

    private final static double BIG_EPSILON = 1E-2d;
    private final static double EPSILON = 1E-5d;
    private final static double EXTREMA_THRESHOLD = 0.3d;

    private static RV rv1, rv2, rv3, rv4;

    @BeforeClass public static void initializeRVs() {
        rv1 = new Beta(0.9d);
        rv2 = new Beta();
        rv2.set(0.0d, 1.0d, 0.9d, 0.05d);
        rv3 = new Beta(0.1d);
        rv4 = new Beta();
        rv4.set(0.0d, 1.0d, 0.1d, 0.05d);
    }

    @Test public void truncateRangeSimple() {
        RV rv = RV.multiply(rv1, 10.0d);
        rv.truncateRange(8.0d, 10.0d);
        assertEquals(9.0d, rv.getMean(), EPSILON);
    }

    @Test public void truncateRange1() {
        RV rv = RV.add(rv1, rv1).subtract(rv1).subtract(rv1);
        rv.truncateRange(0.0d, 1.0d);
        assertEquals(0.0d, rv.getMean(), BIG_EPSILON);
    }

    @Test public void truncateRange2() {
        RV rv = RV.add(rv2, rv2).subtract(rv2).subtract(rv2);
        final double error = rv.getError();
        rv.truncateRange(0.0d, 1.0d);
        assertEquals(error, rv.getError(), 2.0d * BIG_EPSILON);
        assertTrue("Extrema too high: " + rv.getMean(),
                rv.getMean() < EXTREMA_THRESHOLD);
    }

    @Test public void truncateRange3() {
        RV rv = rv2.clone().add(rv4);
        final double error = rv.getError();
        rv.truncateRange(0.0d, 1.0d);
        assertEquals(error, rv.getError(), 2.0d * BIG_EPSILON);
        assertTrue("Extrema too low: " + rv.getMean(),
                rv.getMean() > 1.0d - EXTREMA_THRESHOLD);
    }

    @Test public void truncateRange4() {
        RV rv = rv3.clone().subtract(rv1);
        rv.truncateRange(0.0d, 1.0d);
        assertEquals(0.0d, rv.getMean(), EPSILON);
    }

    @Test public void addScalar() {
        RV rv = rv1.clone().add(1.0d);
        assertEquals(rv1.getMean() + 1.0d, rv.getMean(), EPSILON);
        assertEquals(rv1.getError(), rv.getError(), EPSILON);
    }

    @Test public void multiply() {
        RV rv = rv1.clone().multiply(2.0d);
        assertEquals(rv1.getMean() * 2.d, rv.getMean(), EPSILON);
    }

    @Test public void add() {
        RV rv = RV.add(rv1, rv2);
        assertEquals(rv1.getMean() + rv2.getMean(), rv.getMean(), EPSILON);
    }

    @Test public void subtract() {
        RV rv = RV.subtract(rv3, rv1);
        assertEquals(rv3.getMean() - rv1.getMean(), rv.getMean(), EPSILON);
    }

    @Test public void max() {
        RV rv = RV.max(rv2, rv3);
        assertEquals(rv2.getMean(), rv.getMean(), EPSILON);
        assertEquals(rv2.getError(), rv.getError(), 10.0d * EPSILON);
    }

    @Test public void min() {
        RV rv = RV.min(rv1, rv4);
        assertEquals(rv4.getMean(), rv.getMean(), EPSILON);
        assertEquals(rv4.getError(), rv.getError(), 10.0d * EPSILON);
    }

}
