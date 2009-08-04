package simdeg.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestBetaEstimator {

    private final static double BIG_EPSILON = 1E-2d;
    private final static double EPSILON = 1E-5d;
    private final static double ERROR_THRESHOLD = 0.25d;

    @Test public void betaEstimator() {
        new BetaEstimator();
        new BetaEstimator(0.0d);
        new BetaEstimator(0.2d);
        new BetaEstimator(1.0d, 1.0d);
    }

    @Test(expected=IllegalArgumentException.class)
    public void betaEstimatorException() {
        new BetaEstimator(0.5d, 1.0d);
    }

    @Test public void testClone() {
        BetaEstimator simple = new BetaEstimator();
        simple.set(0.0d, 1.0d, 0.4d, 0.001d);
        BetaEstimator other = simple.clone();
        assertEquals(0.4d, other.getMean(), EPSILON);
        assertEquals(0.001d, other.getVariance(), EPSILON);
    }

    @Test public void setSample() {
        BetaEstimator simple = new BetaEstimator();
        for (int i=0; i<1000; i++)
            simple.setSample(1.0d);
        assertEquals(1.0d, simple.getMean(), BIG_EPSILON);
        assertTrue("Error too high: " + simple.getError(),
                simple.getError() < BIG_EPSILON);
    }

    @Test public void set() {
        BetaEstimator simple = new BetaEstimator();
        /* Standard case */
        simple.set(0.0d, 1.0d, 0.3d, 0.03d);
        assertEquals(0.3d, simple.getMean(), EPSILON);
        assertEquals(0.03d, simple.getVariance(), EPSILON);
        /* Feasible variance but mean too low */
        simple.set(0.0d, 1.0d, 0.01d, 0.01d);
        assertTrue("Mean out of range: " + simple.getMean(),
                simple.getMean() > 0.01d && simple.getMean() < 0.5d);
        assertEquals(0.01d, simple.getVariance(), EPSILON);
    }

    @Test public void truncateRange() {
        BetaEstimator simple = new BetaEstimator(0.5d);
        simple.subtract(simple).truncateRange(0.0d, 1.0d);
        assertEquals(0.0d, simple.getLowerEndpoint(), EPSILON);
        assertEquals(1.0d, simple.getUpperEndpoint(), EPSILON);
        assertEquals(0.0d, simple.getMean(), BIG_EPSILON);
    }

    @Test public void truncateRange1() {
        BetaEstimator e1 = new BetaEstimator(3d, 15d);
        BetaEstimator e2 = new BetaEstimator(9d, 12d);
        BetaEstimator e3 = new BetaEstimator(4d, 15d);
        BetaEstimator e4 = new BetaEstimator(5d, 12d);
        e1.add(e2).subtract(e3).subtract(e4).truncateRange(0.0d, 1.0d);
        assertTrue("Alpha is out of range: " + e1.getAlpha(),
                e1.getAlpha() >= 1.0d);
        assertTrue("Beta is out of range: " + e1.getBeta(),
                e1.getBeta() >= 1.0d);
    }

    @Test public void getSampleCount() {
        BetaEstimator simple = new BetaEstimator();
        simple.set(0.0d, 1.0d, 0.3d, 0.01d);
        final double count = simple.getSampleCount();
        assertTrue("Not enough samples: " + count, count > 10.0d);
        simple.setSample(1.0d);
        assertEquals(count + 1.0d, simple.getSampleCount(), EPSILON);
        simple.set(0.0d, 1.0d, 0.3d, 0.003d);
        assertTrue("Not more samples: " + simple.getSampleCount(),
                simple.getSampleCount() > count);
    }

    @Test public void sampleCountLimit() {
        BetaEstimator simple = new BetaEstimator();
        /* High error */
        final double count1 = simple.sampleCountLimit(0.2d);
        for (int i = 0; i < count1; i++)
            simple.setSample((i % 2 == 0) ? 0.0d : 1.0d);
        assertEquals(0.2d, simple.getError(), BIG_EPSILON);
        /* Low error */
        final double count2 = simple.sampleCountLimit(0.01d);
        for (int i = 0; i < count2; i++)
            simple.setSample((i % 2 == 0) ? 0.0d : 1.0d);
        assertEquals(0.01d, simple.getError(), 0.01d * BIG_EPSILON);
    }

    @Test public void clear() {
        BetaEstimator simple = new BetaEstimator(1.0d);
        simple.clear();
        assertTrue("Error too low: " + simple.getError(),
                simple.getError() > ERROR_THRESHOLD);
    }

    @Test public void mergeSimple() {
        BetaEstimator simple = new BetaEstimator();
        simple.set(0.0d, 1.0d, 0.4d, 0.01d);
        assertEquals(0.4d, simple.getMean(), EPSILON);
        assertEquals(0.01d, simple.getVariance(), EPSILON);
        simple.merge(simple);
        assertEquals(0.4d, simple.getMean(), EPSILON);
        assertEquals(0.01d, simple.getVariance(), EPSILON);
    }

    @Test public void merge() {
        BetaEstimator e1 = new BetaEstimator();
        e1.set(0.0d, 1.0d, 0.2d, 0.01d);
        BetaEstimator e2 = new BetaEstimator();
        e2.set(0.0d, 1.0d, 0.8d, 0.01d);
        e1.merge(e2);
        assertEquals(0.5d, e1.getMean(), EPSILON);
        assertTrue("Variance too high: " + e1.getVariance(),
                e1.getVariance() < 2.0d * 0.01d);
    }

}
