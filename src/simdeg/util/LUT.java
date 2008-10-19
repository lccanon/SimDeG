package simdeg.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class creating Look-Up Table objets containing previously computed
 * results. For binary function. Does not work with short, byte and atomic
 * types. Also possible to achieve the same result with two unary LUT since,
 * for now, there is no interpolation.
 */
public class LUT<E extends Number, R> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(LUT.class.getName());

    /** Method to which we want to minimize calls */
    private Method method = null;

    /** Range */
    private BigDecimal[] range = null;

    /** Computed values */
    private R[] values = null;

    /**
     * Turn around java API limitation.
     */
    static protected <E extends Number> BigDecimal getBigDecimal(E value) {
        if (value instanceof Double || value instanceof Float)
            return new BigDecimal((Double)value);
        if (value instanceof Long)
            return new BigDecimal((Long)value);
        if (value instanceof Integer)
            return new BigDecimal((Integer)value);
        if (value instanceof BigInteger)
            return new BigDecimal((BigInteger)value);
        if (value instanceof BigDecimal)
            return (BigDecimal)value;
        return null;
    }

    /**
     * Turn around java API limitation.
     */
    static protected <E extends Number> BigDecimal[] getBigDecimal(E[] values) {
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i=0; i<values.length; i++)
            result[i] = getBigDecimal(values[i]);
        return result;
    }

    /**
     * Turn around java API limitation.
     */
    static protected <E extends Number> int getSample(BigDecimal[] range) {
        return range[1].subtract(range[0]).divide(range[2],
                RoundingMode.HALF_EVEN).intValue() + 1;
    }

    /**
     * Turn around java API limitation.
     */
    @SuppressWarnings("unchecked")
    static protected <E extends Number> E getValueIndex(int i, E[] range,
            BigDecimal[] bdRange) {
        BigDecimal result = (new BigDecimal(i)).multiply(bdRange[2])
            .add(bdRange[0]);
        if (range[0] instanceof Double)
            return (E)(Double)result.doubleValue();
        if (range[0] instanceof Float)
            return (E)(Float)result.floatValue();
        if (range[0] instanceof Long)
            return (E)(Long)result.longValue();
        if (range[0] instanceof Integer)
            return (E)(Integer)result.intValue();
        if (range[0] instanceof BigInteger)
            return (E)result.toBigInteger();
        return (E)result;
    }

    /**
     * Default constructors for inheritance.
     */
    protected LUT() {
    }

    /**
     * Normal constructor constructing the binary LUT from the information.
     */
    @SuppressWarnings("unchecked")
    public LUT(Method method, E[] range) {
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
     * Turn around java API limitation.
     */
    protected static <E extends Number> int getIndex(E x, BigDecimal[] range) {
        BigDecimal bdX = getBigDecimal(x);
        return bdX.subtract(range[0]).divide(range[2],
                RoundingMode.HALF_EVEN).intValue();
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
