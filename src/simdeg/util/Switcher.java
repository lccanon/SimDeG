package simdeg.util;

import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Tool class used for switching behaviors. Only used here.
 */
public class Switcher<E> {

    private static final double M = 997;

    private static final double A = 805153;

    private E[] elements;

    private double[] starts, ends;

    /*
     * Specific constructor for 2 elements only.
     */
    public Switcher(E[] elements, double start, double end) {
        this(elements, new double[] {start}, new double[] {end});
    }

    /**
     * General constructor where the first elements has to be returned before
     * the first transition between starts[0] and ends[0].
     */
    public Switcher(E[] elements, double[] starts, double[] ends) {
        /* Test for admissibility of parameters */
        if (elements == null || starts == null || ends == null)
            throw new NullPointerException("Array not initialized");
        if (starts.length != ends.length || starts.length != elements.length - 1)
            throw new IllegalArgumentException("Incompatible arrays");
        for (int i=0; i<starts.length; i++)
            if (starts[i] > ends[i] || (i+1 < starts.length && ends[i] > starts[i+1]))
                throw new IllegalArgumentException("Ranges are not contiguous");

        this.elements = elements;
        this.starts = starts;
        this.ends = ends;
    }

    public E get(double time) {
        /* Test for admissibility of parameters */
        if (elements.length == 1)
            return elements[0];
        if (time < 0.0d)
            throw new OutOfRangeException(time, 0.0d, Double.MAX_VALUE);

        /* Optimization */
        if (time <= starts[0])
            return elements[0];
        if (time >= ends[ends.length-1])
            return elements[elements.length-1];

        /* If it is during a non-transition phase */
        for (int i=0; i<starts.length-1; i++)
            if (time >= ends[i] && time <= starts[i+1])
                return elements[i+1];

        /* General algorithm */
        for (int i=0; i<starts.length; i++)
            if (time > starts[i] && time < ends[i]) { 
                final double prop = (time - starts[i])
                    / (ends[i] - starts[i]);
                final double proba = (Math.round(prop * M * A) % M ) / M;
                if (proba > prop)
                    return elements[i];
                return elements[i+1];
            }
        assert(false) : "Abnormal situation in switcher getter";
        return null;
    }

    public E[] getAll() {
        return elements;
    }

    private static int count = 0;
    private final int hash = count++;
    public final int hashCode() {
        int result = 0;
        for (E e : elements)
            if (e != null)
                result += e.hashCode();
        if (result != 0)
            return result;
        return hash;
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("0.##E0",
                new DecimalFormatSymbols(Locale.ENGLISH));
        StringBuilder string = new StringBuilder("Switcher: ");
        for (int i=0; i<elements.length-1; i++) {
            string.append(elements[i] + " -> " + elements[i+1] + " during ["
                    + df.format(starts[i]) + "," + df.format(ends[i]) + "]");
            if (i+1 != elements.length-1)
                string.append("; ");
        }
        return string.toString();
    }

}
