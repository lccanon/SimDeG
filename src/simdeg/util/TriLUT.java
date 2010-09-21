package simdeg.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class creating Look-Up Table objects containing previously computed
 * results. For ternary functions. Does not work with short, byte and atomic
 * types. Also possible to achieve the same result with two unary LUT since,
 * for now, there is no interpolation.
 */
public final class TriLUT<E1 extends Number, E2 extends Number, E3 extends Number, R> extends LUT {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(TriLUT.class.getName());

    /** Method to which we want to minimize calls */
    private final Method method;

    /** First range */
    private final BigDecimal[] range1;

    /** Second range */
    private final BigDecimal[] range2;

    /** Third range */
    private final BigDecimal[] range3;

    /** Computed values */
    private final R[][][] values;

    /**
     * Normal constructor constructing the binary LUT from the information.
     */
    @SuppressWarnings("unchecked")
    public TriLUT(Method method, E1[] range1, E2[] range2, E3[] range3) {
        this.method = method;
        this.range1 = getBigDecimal(range1);
        this.range2 = getBigDecimal(range2);
        this.range3 = getBigDecimal(range3);
        final int sample1 = getSample(this.range1);
        final int sample2 = getSample(this.range2);
        final int sample3 = getSample(this.range3);
        values = (R[][][])new Object[sample1][sample2][sample3];
        for (int i=0; i<sample1; i++)
            for (int j=0; j<sample2; j++)
                for (int k=0; k<sample3; k++)
                    values[i][j][k] = computeValue(getValueIndex(i, range1, this.range1),
                            getValueIndex(j, range2, this.range2), getValueIndex(k, range3, this.range3));
    }

    /**
     * Computes value that are out of the range.
     */
    @SuppressWarnings("unchecked")
    private final R computeValue(E1 x, E2 y, E3 z) {
        try {
            return (R)method.invoke(null, x, y, z);
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
    public final R getValue(E1 x, E2 y, E3 z) {
        final int xIndex = getIndex(x, range1);
        final int yIndex = getIndex(y, range2);
        final int zIndex = getIndex(z, range3);
        if (xIndex < 0 || xIndex >= values.length || yIndex < 0
                || yIndex >= values[0].length || zIndex < 0
                || zIndex >= values[0][0].length) {
            logger.fine("Value out of the range stored in the LUT");
            return computeValue(x, y, z);
        }
        if (logger.isLoggable(Level.FINER))
            logger.finer("Returns " + values[xIndex][yIndex][zIndex] + " instead of "
                    + computeValue(x, y, z));
        /* XXX do interpolation if R is a Number */
        return values[xIndex][yIndex][zIndex];
    }

}
