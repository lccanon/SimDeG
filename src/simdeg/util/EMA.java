package simdeg.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.abs;
import java.lang.Double;

import flanagan.analysis.Stat;

public class EMA extends RV {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(EMA.class.getName());

    /**
     * Weight of the exponential short term moving average in order to have
     * a standard deviation equal to shortTermStdDev.
     */
    private final double shortTermWeight;

    private final double shortTermStdDev;

    /**
     * Weight of the exponential long term moving average in order to have
     * a standard deviation equal to longTermStdDev.
     */
    private final double longTermWeight;

    private final double longTermStdDev;

    private double shortTermEstimate;

    private double longTermEstimate;

    /** Default short term estimator standard deviation */
    private static final double SHORT_TERM_STD_DEV = 0.1d;

    /** Default long term estimator standard deviation */
    private static final double LONG_TERM_STD_DEV = 0.03d;

    private static final double LUT_PRECISION = 0.01d;

    private static UnaryLUT<Double,Double> estimatorsConsistencyValues;

    static {
        try {
            Method estimatorsConsistencyMethod
                = EMA.class.getMethod(
                        "getEstimatorsConsistency", Double.TYPE);
            estimatorsConsistencyValues = new UnaryLUT<Double,Double>(
                    estimatorsConsistencyMethod,
                    new Double[] {0.0d, 1.0d, LUT_PRECISION});
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE,
                    "The method getEstimatorsConsistency was not found", e);
            System.exit(1);
        }
    }

    public EMA() {
        this(SHORT_TERM_STD_DEV, LONG_TERM_STD_DEV);
    }

    public EMA(double estimate) {
        this(SHORT_TERM_STD_DEV, LONG_TERM_STD_DEV);
        set(0.0d, 1.0d, estimate, 0.0d);
    }

    public EMA(double shortTermStdDev, double longTermStdDev) {
        this(1.0d, 0.5d, shortTermStdDev, longTermStdDev);
    }

    public EMA(double shortTermEstimate, double longTermEstimate,
            double shortTermStdDev, double longTermStdDev) {
        super(0.0d, 1.0d);
        this.shortTermEstimate = shortTermEstimate;
        this.longTermEstimate = longTermEstimate;
        this.shortTermStdDev = shortTermStdDev;
        this.longTermStdDev = longTermStdDev;
        this.shortTermWeight = 8.0d * pow(shortTermStdDev, 2.0d)
                / (4.0d * pow(shortTermStdDev, 2.0d) + 1.0d);
        this.longTermWeight = 8.0d * pow(longTermStdDev, 2.0d)
                / (4.0d * pow(longTermStdDev, 2.0d) + 1.0d);
    }

    public final double getEstimatorsConsistency(double delta,
            double shortTermWeight, double longTermWeight) {
        final double result = 1.0d - Stat.erf(delta
                / (sqrt(2.0d) * (shortTermStdDev + longTermStdDev)));
        return result;
    }

    public final double getEstimatorsConsistency(double delta) {
        return getEstimatorsConsistency(delta, shortTermWeight,
                longTermWeight);
    }

    private double getEstimatorsConsistency(double e1, double e2) {
        return estimatorsConsistencyValues.getValue(abs(e1 - e2));
    }

    public EMA clone() {
        return new EMA(shortTermEstimate, longTermEstimate,
                shortTermStdDev, longTermStdDev);
    }

    public EMA clone(double value) {
        return new EMA(shortTermEstimate, value,
                shortTermStdDev, longTermStdDev);
    }

    public EMA clone(double value, double error) {
        EMA result = new EMA(shortTermEstimate, value,
                shortTermStdDev, longTermStdDev);
        return result.set(this.lower, this.upper, result.getMean(), error);
    }

    public void setSample(double value) {
        shortTermEstimate *= shortTermWeight;
        shortTermEstimate += value * shortTermWeight;
        longTermEstimate *= longTermEstimate;
        longTermEstimate += value * longTermEstimate;
    }

    public double getMean() {
        return longTermEstimate;
    }

    public double getError() {
        return 1.0d - getEstimatorsConsistency(abs(shortTermEstimate
                    - longTermEstimate));
        //return getEstimatorsConsistency(shortTermEstimate, longTermEstimate);
    }

    private double getError(double level) {
        return getError();
    }

    public double getVariance() {
        return 0.0d;
    }

    //XXX dirty and higly approximate
    protected EMA set(double lower, double upper,
            double estimate, double variance) {
        super.set(lower, upper, estimate, variance);
        longTermEstimate = estimate;
        shortTermEstimate = longTermEstimate;
        double error = Math.sqrt(variance) * 6.0d;
        while (getError() < error) {
            if (longTermEstimate < 0.5d)
                shortTermEstimate += 0.1d;
            else
                shortTermEstimate -= 0.1d;
        }
        return this;
    }

    protected EMA approximate(double lower, double upper,
            double estimate, double variance) {
        return set(lower, upper, estimate, variance);
    }

    /*
    public double getConsistency(Estimator estimator) {
        return Math.min(Math.min(1.0d - getError(),
                    1.0d - estimator.getError()),
                getEstimatorsConsistency(abs(getMean()
                        - estimator.getMean()),
                    longTermWeight, longTermWeight));
    }
    */

    public EMA clear() {
        return this;
    }

    public EMA merge(Estimator estimator) {
        return this;
    }

}
