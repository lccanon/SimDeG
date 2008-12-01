package simdeg.util;

import java.lang.IllegalArgumentException;

/**
 * Exception thrown when the selection of an Result have to be postponed.
 */
public class OutOfRangeException extends IllegalArgumentException {

    private static final long serialVersionUID = 0L;

    private final Object value, min, max;

    public OutOfRangeException(Object value, Object min, Object max) {
        super("Value " + value + " out of range " +
                "[" + min + ", " + max + "]");
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public Object getValue() {
        return value;
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }

}
