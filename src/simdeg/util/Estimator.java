package simdeg.util;

import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.Collection;

public abstract class Estimator {

    /** Default confidence level concerning the committed error */
    public final static double DEFAULT_LEVEL = 0.95d;

    /** Default error of precise estimator */
    public static final double DEFAULT_ERROR = 1E-2d;

    protected double lower = Double.NEGATIVE_INFINITY;

    protected double upper = Double.POSITIVE_INFINITY;

    /**
     * Generic duplication and creation.
     */
    public abstract Estimator clone();

    /**
     * Updates new values.
     */
    public abstract void setSample(double value);


    /* Public accessors */

    public abstract double getEstimate();

    protected abstract double getVariance();

    public abstract double getError();

    public abstract double getError(double level);

    public double getConsistency(Estimator estimator) {
        final double maxError = Math.max(getError(),
                estimator.getError());
        if (Math.abs(getEstimate() - estimator.getEstimate()) > maxError)
            return 0.0d;
        return 1.0d - maxError;
    }

    /**
     * Internal operation for setting to a given estimate and variance.
     */
    protected abstract boolean set(double estimate, double variance);


    /* General operations */

    protected double getLowerEndpoint() {
        return this.lower;
    }

    protected double getUpperEndpoint() {
        return this.upper;
    }

    /**
     * Sets the interval of current estimator.
     */
    public void setRange(double lower, double upper) {
        /* Test for admissibility of parameters */
        if (lower > upper)
            throw new IllegalArgumentException("Range is not admissible: [" + lower + ", " + upper + "]");
        final double estimate = getEstimate();
        final double variance = getVariance();
        this.lower = lower;
        this.upper = upper;
        /* Try to keep the same characteristics */
        if (!set(estimate, variance))
            reset();
    }

    /**
     * Reinitializes the parameters of this estimator.
     */
    public abstract Estimator reset();


    /* Arithmetic operations */

    public Estimator inverse() {
        return this.multiply(-1.0d);
    }

    public static Estimator inverse(Estimator estimator) {
        return estimator.clone().inverse();
    }

    public Estimator add(Estimator estimator) {
        final double estimate = getEstimate();
        final double variance = getVariance();
        setRange(getLowerEndpoint() + estimator.getLowerEndpoint(),
                getUpperEndpoint() + estimator.getUpperEndpoint());
        set(estimate + estimator.getEstimate(),
                variance + estimator.getVariance());
        return this;
    }

    public static Estimator add(Estimator e1, Estimator e2) {
        return e1.clone().add(e2);
    }

    public Estimator subtract(Estimator estimator) {
        inverse();
        add(estimator);
        return inverse();
    }

    public static Estimator subtract(Estimator e1, Estimator e2) {
        return e1.clone().subtract(e2);
    }

    public Estimator min(Estimator estimator) {
        return inverse().max(inverse(estimator)).inverse();
    }

    public static Estimator min(Estimator e1, Estimator e2) {
        return e1.clone().min(e2);
    }

    public Estimator max(Estimator estimator) {
        //XXX better maximum
        if (getEstimate() > estimator.getEstimate())
            return this;
        else
            return estimator;
    }

    public static Estimator max(Estimator e1, Estimator e2) {
        return e1.clone().max(e2);
    }

    public Estimator multiply(double d) {
        final double estimate = getEstimate();
        final double variance = getVariance();
        if (d < 0.0d)
            setRange(d * getUpperEndpoint(), d * getLowerEndpoint());
        else
            setRange(d * getLowerEndpoint(), d * getUpperEndpoint());
        set(d * estimate, d * d * variance);
        return this;
    }

    public static Estimator multiply(Estimator e1, double d) {
        return e1.clone().multiply(d);
    }


    /* Utilitary function */

    public String toString() {
        DecimalFormat df = new DecimalFormat("0.##",
                new DecimalFormatSymbols(Locale.ENGLISH));
        return "{" + df.format(getEstimate()) + ","
            + df.format(getError()) + "}";
    }

}
