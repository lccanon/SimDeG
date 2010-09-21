package simdeg.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestRandomManager {

    private final static double EPSILON = 1E-6d;

    @Test public void getRandom() {
        RandomManager.getRandom("AAA").nextDouble();
    }

    @Test public void setSeed() {
        double a0 = RandomManager.getRandom("Test").nextDouble();
        RandomManager.setSeed("Test", 134L);
        double a1 = RandomManager.getRandom("Test").nextDouble();
        assertTrue(Math.abs(a0 - a1) > EPSILON);
        RandomManager.setSeed("Test", 134L);
        double a2 = RandomManager.getRandom("Test").nextDouble();
        assertEquals(a1, a2, EPSILON);
    }

}
