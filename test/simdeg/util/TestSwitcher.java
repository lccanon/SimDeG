package simdeg.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestSwitcher {

    private final static double EPSILON = 1E-6d;

    private Switcher<Double> instance() {
        Double[] elements = new Double[] {100.0d, 200.0d, 300.0d, 400.0d};
        double[] starts = new double[] {1.0d, 2.0d, 10.0d};
        double[] ends = new double[] {1.5d, 4.0d, 20.0d};
        return new Switcher<Double>(elements, starts, ends);
    }

    @Test public void switcher() {
        instance();
    }

    @Test public void get() {
        Switcher<Double> switcher = instance();
        assertEquals(switcher.get(0.0d), 100.0d, EPSILON);
        assertEquals(switcher.get(1.8d), 200.0d, EPSILON);
        assertEquals(switcher.get(5.0d), 300.0d, EPSILON);
        assertEquals(switcher.get(10.0d), 300.0d, EPSILON);
    }

    @Test(expected=NullPointerException.class)
    public void switcherException() {
        new Switcher<Double>(null, null, null);
    }

    @Test(expected=OutOfRangeException.class)
    public void getException() {
        Switcher<Double> switcher = instance();
        switcher.get(-1.0d);
    }

}
