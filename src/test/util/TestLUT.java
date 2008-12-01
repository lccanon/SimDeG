package simdeg.util;

import java.lang.reflect.Method;
import java.lang.NoSuchMethodException;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestLUT {

    private final static double EPSILON = 1E-2d;

    public static final double square(double x) {
        return x*x;
    }

    @Test public void getValue() throws NoSuchMethodException {
        Method squareMethod = TestLUT.class.getMethod("square", Double.TYPE);
        UnaryLUT<Double,Double> methodValues = new UnaryLUT<Double,Double>(
                    squareMethod, new Double[] {0.0d, 1.0d, EPSILON/10.0d});
        for (int i=-100; i<200; i++)
            assertEquals(methodValues.getValue(i/100.0d), square(i/100.0d),
                    EPSILON);
    }

}
