package simdeg.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class creating Look-Up Table objets containing previously computed
 * results. For unary functions. Does not work with short, byte and atomic
 * types. Also possible to achieve the same result with two unary LUT since,
 * for now, there is no interpolation.
 */
public final class UnaryLUT<E extends Number, R> extends LUT {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(UnaryLUT.class.getName());

    /** Method to which we want to minimize calls */
    private final Method method;

    /** Range */
    private final BigDecimal[] range;

    /** Computed values */
    private final R[] values;

    /**
     * Normal constructor constructing the binary LUT from the information.
     */
    @SuppressWarnings("unchecked")
    public UnaryLUT(Method method, E[] range) {
        this.method = method;
        this.range = getBigDecimal(range);
        final int sample = getSample(this.range);
        values = (R[])new Object[sample];
        for (int i=0; i<sample; i++)
            values[i] = computeValue(getValueIndex(i, range, this.range));
    }

    /**
     * Computes value that are out of the range.
     */
    @SuppressWarnings("unchecked")
    private R computeValue(E x) {
        try {
            return (R)method.invoke(null, x);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "The method " + method.getName()
                    + " has raised a problem", e);
            System.exit(1);
        }
        return null;
    }

    /**
     * Standard method for retrieving a given value.
     */
    public R getValue(E x) {
        final int xIndex = getIndex(x, range);
        if (xIndex < 0 || xIndex >= values.length) {
            logger.fine("Value out of the range stored in the LUT");
            return computeValue(x);
        }
        if (logger.isLoggable(Level.FINER))
            logger.finer("Returns " + values[xIndex] + " instead of "
                    + computeValue(x));
        /* XXX do interpolation if R is a Number */
        return values[xIndex];
    }

}
