package simdeg.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Class creating Look-Up Table objects containing previously computed
 * results.
 */
abstract class LUT {

    /**
     * Turn around java API limitation.
     */
    protected static <E extends Number> BigDecimal getBigDecimal(E value) {
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
    protected static <E extends Number> BigDecimal[] getBigDecimal(E[] values) {
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i=0; i<values.length; i++)
            result[i] = getBigDecimal(values[i]);
        return result;
    }

    /**
     * Turn around java API limitation.
     */
    protected static <E extends Number> int getSample(BigDecimal[] range) {
        return range[1].subtract(range[0]).divide(range[2],
                RoundingMode.HALF_EVEN).intValue() + 1;
    }

    /**
     * Turn around java API limitation.
     */
    @SuppressWarnings("unchecked")
    protected static <E extends Number> E getValueIndex(int i, E[] range,
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
     * Turn around java API limitation.
     */
    protected static <E extends Number> int getIndex(E x, BigDecimal[] range) {
        final BigDecimal bdX = getBigDecimal(x);
        return bdX.subtract(range[0]).divide(range[2],
                RoundingMode.HALF_EVEN).intValue();
    }

}
