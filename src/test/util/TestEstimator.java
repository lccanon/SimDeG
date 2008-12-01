package simdeg.util;

import java.util.Arrays;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class TestEstimator {

    private final static double BIG_EPSILON = 1E-2d;

    private static Estimator e1, e2, e3, e4;

    @BeforeClass public static void initializeEstimators() {
        e1 = new BTS(0.9d, 0.01d);
        e2 = new BTS(0.9d, 0.3d);
        e3 = new BTS(0.1d, 0.01d);
        e4 = new BTS(0.1d, 0.3d);
    }

    @Test public void max() {
        Estimator e = Estimator.max(e2, e3);
        assertEquals(e2.getEstimate(), e.getEstimate(), BIG_EPSILON);
        assertEquals(e2.getError(), e.getError(), BIG_EPSILON);
    }

    @Test public void min() {
        Estimator e = Estimator.min(e1, e4);
        assertEquals(e4.getEstimate(), e.getEstimate(), BIG_EPSILON);
        assertEquals(e4.getError(), e.getError(), BIG_EPSILON);
    }

    @Test public void add() {
        Estimator e = Estimator.add(e1, e2);
        assertEquals(e1.getEstimate() + e2.getEstimate(), e.getEstimate(),
                BIG_EPSILON);
    }

    @Test public void subtract() {
        Estimator e = Estimator.subtract(e3, e1);
        assertEquals(e3.getEstimate() - e1.getEstimate(), e.getEstimate(),
                BIG_EPSILON);
    }

/*
    @Test public void mean() {
        Estimator e = Estimator.mean(Arrays.asList(e1, e2, e3, e4));
        assertEquals((e1.getEstimate() + e4.getEstimate())/2.0d,
                e.getEstimate(), 2 * BIG_EPSILON);
        assertEquals((e1.getError() + e4.getError())/2.0d,
                e.getError(), 2 * BIG_EPSILON);
    }
*/

}
