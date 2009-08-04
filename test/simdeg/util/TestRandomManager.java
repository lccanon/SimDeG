package simdeg.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestRandomManager {

    private final static double EPSILON = 1E-6d;

    @Test public void getRandom() {
        RandomManager.getRandom("AAA").nextDouble();
    }

    @Test public void getPsRandom() {
        RandomManager.getPsRandom("BBB").nextBeta(1d, 2d);
    }

    @Test public void setSeed() {
        double a0 = RandomManager.getRandom("Test").nextDouble();
        double b0 = RandomManager.getPsRandom("Test").nextBeta(1d, 2d);
        RandomManager.setSeed("Test", 134L);
        double a1 = RandomManager.getRandom("Test").nextDouble();
        double b1 = RandomManager.getPsRandom("Test").nextBeta(1d, 2d);
        assertTrue(Math.abs(a0 - a1) > EPSILON);
        assertTrue(Math.abs(b0 - b1) > EPSILON);
        RandomManager.setSeed("Test", 134L);
        double a2 = RandomManager.getRandom("Test").nextDouble();
        double b2 = RandomManager.getPsRandom("Test").nextBeta(1d, 2d);
        assertEquals(a1, a2, EPSILON);
        assertEquals(b1, b2, EPSILON);
    }

}
