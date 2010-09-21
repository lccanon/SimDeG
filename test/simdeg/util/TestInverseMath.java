package simdeg.util;

import static org.junit.Assert.assertEquals;
import static simdeg.util.InverseMath.inverseIncompleteBeta;
import static simdeg.util.InverseMath.inverseNormal;
import static simdeg.util.InverseMath.inverseStandardNormal;

import org.junit.Test;

public class TestInverseMath {

    private final static double EPSILON = 1E-7d;

    @Test public void boundsNormal() {
        assertEquals(Double.NEGATIVE_INFINITY, inverseNormal(0.0d, 0.0d, 1.0d), EPSILON);
        assertEquals(Double.POSITIVE_INFINITY, inverseNormal(1.0d, 0.0d, 1.0d), EPSILON);
    }

    @Test public void boundsIncompleteBeta() {
        assertEquals(0.0d, inverseIncompleteBeta(0.0d, 1.0d, 1.0d), EPSILON);
        assertEquals(1.0d, inverseIncompleteBeta(1.0d, 1.0d, 1.0d), EPSILON);
        assertEquals(0.0d, inverseIncompleteBeta(0.0d, 10.0d, 10.0d), EPSILON);
        assertEquals(1.0d, inverseIncompleteBeta(1.0d, 10.0d, 10.0d), EPSILON);
        assertEquals(0.0d, inverseIncompleteBeta(0.0d, 1.0d, 100.0d), EPSILON);
        assertEquals(1.0d, inverseIncompleteBeta(1.0d, 1.0d, 100.0d), EPSILON);
    }

    @Test(expected=OutOfRangeException.class)
    public void exceptionNormal() {
        inverseNormal(-0.5d, 0.0d, 1.0d);
    }

    @Test(expected=OutOfRangeException.class)
    public void exceptionIncompleteBeta() {
        inverseIncompleteBeta(0.5d, 0.0d, -1.0d);
    }

    @Test public void symetryNormal() {
        for (int i=0; i<1E3; i++)
            assertEquals(inverseStandardNormal(1E-3 * i),
                    -inverseStandardNormal(1.0d - 1E-3 * i), EPSILON);
    }

    @Test public void symetryIncompleteBeta() {
        for (int i=1; i<1000; i++)
            assertEquals(0.5d, inverseIncompleteBeta(0.5d, i, i), EPSILON);
    }

    @Test public void extremeRegionNormal() {
        assertEquals(-4.264890794d, inverseStandardNormal(0.00001d), EPSILON);
        assertEquals(-3.719016485d, inverseStandardNormal(0.0001d), EPSILON);
        assertEquals(-3.090232306d, inverseStandardNormal(0.001d), EPSILON);
        assertEquals(-2.326347874d, inverseStandardNormal(0.01d), EPSILON);
    }

    @Test public void centralRegionNormal() {
        assertEquals(-1.2815515655d, inverseStandardNormal(0.1d), EPSILON);
        assertEquals(-0.8416212336d, inverseStandardNormal(0.2d), EPSILON);
        assertEquals(-0.5244005127d, inverseStandardNormal(0.3d), EPSILON);
        assertEquals(-0.2533471031d, inverseStandardNormal(0.4d), EPSILON);
        assertEquals(0.0d, inverseStandardNormal(0.5d), EPSILON);
    }

    @Test public void smallIncompleteBeta() {
        assertEquals(0.29289322d, inverseIncompleteBeta(0.5d, 1.0d, 2.0d), EPSILON);
        assertEquals(0.12944944d, inverseIncompleteBeta(0.5d, 1.0d, 5.0d), EPSILON);
        assertEquals(0.06696701d, inverseIncompleteBeta(0.5d, 1.0d, 10.0d), EPSILON);
        assertEquals(0.03406367d, inverseIncompleteBeta(0.5d, 1.0d, 20.0d), EPSILON);
        assertEquals(0.7937005d, inverseIncompleteBeta(0.5d, 3.0d, 1.0d), EPSILON);
        assertEquals(0.6142724d, inverseIncompleteBeta(0.5d, 3.0d, 2.0d), EPSILON);
        assertEquals(0.3641161d, inverseIncompleteBeta(0.5d, 3.0d, 5.0d), EPSILON);
        assertEquals(0.2166864d, inverseIncompleteBeta(0.5d, 3.0d, 10.0d), EPSILON);
        assertEquals(0.1197041d, inverseIncompleteBeta(0.5d, 3.0d, 20.0d), EPSILON);
        assertEquals(0.9771600d, inverseIncompleteBeta(0.5d, 30.0d, 1.0d), EPSILON);
        assertEquals(0.9464479d, inverseIncompleteBeta(0.5d, 30.0d, 2.0d), EPSILON);
        assertEquals(0.8639674d, inverseIncompleteBeta(0.5d, 30.0d, 5.0d), EPSILON);
        assertEquals(0.7541989d, inverseIncompleteBeta(0.5d, 30.0d, 10.0d), EPSILON);
        assertEquals(0.6013437d, inverseIncompleteBeta(0.5d, 30.0d, 20.0d), EPSILON);
        assertEquals(0.09259526d, inverseIncompleteBeta(0.1d, 2.0d, 5.0d), EPSILON);
        assertEquals(0.18180347d, inverseIncompleteBeta(0.3d, 2.0d, 5.0d), EPSILON);
        assertEquals(0.26444998d, inverseIncompleteBeta(0.5d, 2.0d, 5.0d), EPSILON);
        assertEquals(0.36035769d, inverseIncompleteBeta(0.7d, 2.0d, 5.0d), EPSILON);
        assertEquals(0.51031631d, inverseIncompleteBeta(0.9d, 2.0d, 5.0d), EPSILON);
    }

    @Test public void largeIncompleteBeta() {
        assertEquals(0.2639728d, inverseIncompleteBeta(0.1d, 200.0d, 500.0d), EPSILON);
        assertEquals(0.2766122d, inverseIncompleteBeta(0.3d, 200.0d, 500.0d), EPSILON);
        assertEquals(0.2855101d, inverseIncompleteBeta(0.5d, 200.0d, 500.0d), EPSILON);
        assertEquals(0.2945205d, inverseIncompleteBeta(0.7d, 200.0d, 500.0d), EPSILON);
        assertEquals(0.3077185d, inverseIncompleteBeta(0.9d, 200.0d, 500.0d), EPSILON);
    }

}
