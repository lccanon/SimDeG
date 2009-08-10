package simdeg.util;

import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import flanagan.analysis.Stat;

public abstract class RV {

    /**
     * Default confidence level concerning the computation of the committed
     * error through getError(). This level is an upper bound.
     */
    protected static final double DEFAULT_ERROR_LEVEL = 0.95d;

    /**
     * Maximal error committed by default when an estimator is initialized to a
     * specific value (with the default level).
     */
    public static final double DEFAULT_ERROR = 1E-3d;

    protected double lower = Double.NEGATIVE_INFINITY;

    protected double upper = Double.POSITIVE_INFINITY;

    protected RV(double lower, double upper) {
        /* Test for admissibility of parameters */
        if (lower > upper)
            throw new IllegalArgumentException("Range is not admissible: "
                    + lower + ", " + upper);
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Gives the estimation of the measured value given the previous samples.
     */
    public abstract double getMean();

    /**
     * Gives the variance of the rv, based on the number of measures.
     */
    protected abstract double getVariance();

    /**
     * Gives the estimation of the error on the measured value with the default
     * level.
     */
    public abstract double getError();

    protected double getLowerEndpoint() {
        return this.lower;
    }

    protected double getUpperEndpoint() {
        return this.upper;
    }

    /**
     * Internal operation for setting to an approximation of the given estimate
     * and variance.
     */
    protected RV set(double lower, double upper,
            double estimate, double variance) {
        if (lower > upper) {
            this.lower = (lower + upper) / 2.;
            this.upper = this.lower;
        } else {
            this.lower = lower;
            this.upper = upper;
        }
        return this;
    }

    public RV truncateRange(double lower, double upper) {
        return set(lower, upper, getMean(), getVariance());
    }

    /**
     * Generic duplication and creation.
     */
    public abstract RV clone();

    /* Arithmetic operations */

    public RV add(double d) {
        return set(lower + d, upper + d, getMean() + d, getVariance());
    }

    public static RV add(RV rv1, double d) {
        return rv1.clone().add(d);
    }

    public RV multiply(double d) {
        final double estimate = getMean();
        final double variance = getVariance();
        final double lower = (d < 0.0d) ? getUpperEndpoint() : getLowerEndpoint();
        final double upper = (d > 0.0d) ? getUpperEndpoint() : getLowerEndpoint();
        return set(d * lower, d * upper, d * estimate, d * d * variance);
    }

    public static RV multiply(RV rv1, double d) {
        return rv1.clone().multiply(d);
    }

    private RV opposite() {
        return this.multiply(-1.0d);
    }

    private static RV opposite(RV rv) {
        return rv.clone().opposite();
    }

    public RV add(RV rv) {
        set(getLowerEndpoint() + rv.getLowerEndpoint(),
                getUpperEndpoint() + rv.getUpperEndpoint(),
                getMean() + rv.getMean(),
                getVariance() + rv.getVariance());
        return this;
    }

    public static RV add(RV rv1, RV rv2) {
        return rv1.clone().add(rv2);
    }

    public RV subtract(RV rv) {
        if (rv == this)
            return rv.add(opposite(rv));
        return opposite().add(rv).opposite();
    }

    public static RV subtract(RV rv1, RV rv2) {
        return rv1.clone().subtract(rv2);
    }

    public RV min(RV rv) {
        if (rv == this)
            return opposite().max(rv).opposite();
        return opposite().max(opposite(rv)).opposite();
    }

    public static RV min(RV rv1, RV rv2) {
        return rv1.clone().min(rv2);
    }

    /**
     * Generate a distribution of the same type that set the maximum
     * through Clark's moment approach.
     */
    public RV max(RV rv) {
        final double mean1 = getMean();
        final double mean2 = rv.getMean();
        final double var1 = getVariance();
        final double var2 = rv.getVariance();
        final double a = Math.sqrt(var1 + var2);
        final double alpha = (mean1 - mean2) / a;
        final double estimate = mean1 * Phi(alpha) + mean2 * Phi(-alpha)
            + a * varphi(alpha);
        final double variance = (mean1 * mean1 + var1) * Phi(alpha)
            + (mean2 * mean2 + var2) * Phi(-alpha)
            + (mean1 + mean2) * a * varphi(alpha) - estimate * estimate;
        final double lower = Math.max(getLowerEndpoint(), rv.getLowerEndpoint());
        final double upper = Math.max(getUpperEndpoint(), rv.getUpperEndpoint());
        return set(lower, upper, estimate, variance);
    }

    public static RV max(RV rv1, RV rv2) {
        return rv1.clone().max(rv2);
    }


    /* Function for the maximum approximation */

    /**
     * Definition of the function varphi.
     */
    private static final double varphi(double x) {
        if (x == Double.NEGATIVE_INFINITY || x == Double.POSITIVE_INFINITY)
            return 0.0d;
        final double c = 1.0d / (Math.sqrt(2.0d * Math.PI));
        return c * Math.exp(- x*x / 2.0d);
    }

    /**
     * Definition of the function Phi.
     */
    private static final double Phi(double x) {
        if (x == Double.NEGATIVE_INFINITY)
            return 0.0d;
        if (x == Double.POSITIVE_INFINITY)
            return 1.0d;
        final double c = 1.0d / Math.sqrt(2.0d);
        return (1.0d + Stat.erf(c * x)) / 2.0d;
    }


    /* Utilitary function */

    public String toString() {
        DecimalFormat df = new DecimalFormat("0.##",
                new DecimalFormatSymbols(Locale.ENGLISH));
        return "{" + df.format(getMean()) + ","
            + df.format(getError()) + "}";
    }

}
