package simdeg.util;

import java.util.Arrays;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class TestEstimator {

    private final static double EPSILON = 1E-2d;

    private static Estimator e1, e2, e3, e4;

    @BeforeClass public static void initializeEstimators() {
        e1 = new BTS(0.9d, 0.99d);
        e2 = new BTS(0.9d, 0.5d);
        e3 = new BTS(0.1d, 0.99d);
        e4 = new BTS(0.1d, 0.5d);
    }

    @Test public void max() {
        Estimator e = Estimator.max(e2, e3);
        assertEquals(e.getEstimate(), e2.getEstimate(), EPSILON);
        assertEquals(e.getConsistency(), e2.getConsistency(), EPSILON);
    }

    @Test public void min() {
        Estimator e = Estimator.min(e1, e4);
        assertEquals(e.getEstimate(), e4.getEstimate(), EPSILON);
        assertEquals(e.getConsistency(), e4.getConsistency(), EPSILON);
    }

    @Test public void add() {
        Estimator e = Estimator.add(e1, e2);
        assertEquals(e.getEstimate(), e1.getEstimate() + e2.getEstimate(),
                EPSILON);
    }

    @Test public void subtract() {
        Estimator e = Estimator.subtract(e3, e1);
        assertEquals(e.getEstimate(), e3.getEstimate() - e1.getEstimate(),
                EPSILON);
    }

    @Test public void mean() {
        Estimator e = Estimator.mean(Arrays.asList(e1, e2, e3, e4));
        assertEquals(e.getEstimate(),
                (e1.getEstimate() + e4.getEstimate())/2.0d, 2 * EPSILON);
        assertEquals(e.getConsistency(),
                (e1.getConsistency() + e4.getConsistency())/2.0d, 2 * EPSILON);
    }

}
