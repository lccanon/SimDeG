package simdeg.util;

import static simdeg.util.InverseMath.inverseIncompleteBeta;
import static simdeg.util.InverseMath.inverseNormal;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.IllegalArgumentException;
import java.lang.reflect.Method;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Estimator for Bernoulli Trial only. It is based on Bayesian estimation.
 */
public class Beta extends Estimator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Beta.class.getName());

    private static final int MAX_ITERATION = 5;

    /** Binary LUT for minimizing calls to complex functions */
    private static TriLUT<Double, Double, Double, Double> errorValues = null;

    /** Number of values stored in the LUT */
    private static final int MAX_LUT_INDEX = 100;

    /**
     * Generates a LUT for fastening complex computations.
     */
    static {
        try {
            Method errorMethod = Beta.class
                .getMethod("error", Double.TYPE, Double.TYPE, Double.TYPE);
            errorValues = new TriLUT<Double, Double, Double, Double>(errorMethod,
                    new Double[] {1.0d, MAX_LUT_INDEX + 1.0d, 1.0d},
                    new Double[] {1.0d, MAX_LUT_INDEX + 1.0d, 1.0d},
                    new Double[] {DEFAULT_LEVEL, DEFAULT_LEVEL, 1.0d});
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE,
                    "The method error was not found", e);
            System.exit(1);
        }
    }

    /** Principal paramater representing quantity of one values */
    private double alpha = 1.0d;

    /** Principal paramater representing quantity of zero values */
    private double beta = 1.0d;

    /**
     * Initializes without any information.
     */
    public Beta() {
        reset();
    }

    /**
     * Initializes with precise information.
     */
    public Beta(double estimate) {
        this(estimate, DEFAULT_ERROR);
    }

    public Beta(double estimate, double error) {
        /* Test for admissibility of parameters */
        if (estimate < 0.0d || estimate > 1.0d)
            throw new OutOfRangeException(estimate, 0.0d, 1.0d);
        if (error < 0.0d || error > 1.0d)
            throw new OutOfRangeException(error, 0.0d, 1.0d);
        this.lower = 0.0d;
        this.upper = 1.0d;

        /* Optimization */
        if (error == 1.0d) {
            reset();
            return;
        }
        if (error == DEFAULT_ERROR && estimate == 0.0d) {
            alpha = 1;
            beta = 199;
            return;
        } else if (error == DEFAULT_ERROR && estimate == 1.0d) {
            alpha = 199;
            beta = 1;
            return;
        }

        
        /* Approximate iteratively the number of obsevations */
        //XXX More precise and faster method
        alpha = beta = 1.0d;
        double bestAlpha = 1.0d, bestBeta = 1.0d;
        double bestDiff = 1.0d;
        final double increment = 0.1d / error;
        for (int nbUnsuccess = 0; nbUnsuccess < MAX_ITERATION; ) {
            /* Test if it is better */
            if (Math.abs(getError() - error) < bestDiff) {
                bestAlpha = alpha;
                bestBeta = beta;
                bestDiff = Math.abs(getError() - error);
                nbUnsuccess = 0;
            } else
                nbUnsuccess++;
            /* Try to get closer for next iteration */
            final double weightedEstimate = (alpha + beta + increment) * estimate;
            if (Math.abs(alpha - weightedEstimate) > Math.abs(alpha + increment - weightedEstimate))
                alpha += increment;
            else
                beta += increment;
        }

        /* Take the best solutions */
        alpha = bestAlpha;
        beta = bestBeta;
    }

    /**
     * Internal initialization for faster cloning.
     */
    protected Beta(double alpha, double beta, double lower, double upper) {
        /* Test for admissibility of parameters */
        if (alpha < 1.0d)
            throw new OutOfRangeException(alpha, 1.0d, Double.MAX_VALUE);
        if (beta < 1.0d)
            throw new OutOfRangeException(beta, 1.0d, Double.MAX_VALUE);
        if (lower > upper)
            throw new IllegalArgumentException("Range is not admissible: " + lower + ", " + upper);
        this.alpha = alpha;
        this.beta = beta;
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Accessor for children of this class.
     */
    protected double getAlpha() {
        return this.alpha;
    }

    /**
     * Accessor for children of this class.
     */
    protected double getBeta() {
        return this.beta;
    }

    public Beta clone() {
        return new Beta(alpha, beta, lower, upper);
    }

    /**
     * Notifies a new values (either 1 or 0).
     */
    public void setSample(double value) {
        if (value != 0.0d && value != 1.0d)
            throw new IllegalArgumentException
                ("Estimator designed for Bernoulli trial only");
        if (value == 1.0d)
            alpha++;
        else
            beta++;
    }

    /**
     * Gives the current estimate.
     */
    public double getEstimate() {
        assert(this.alpha >= 1.0d && this.beta >= 1.0d) : "Alpha or beta are uncorrect";
        return alpha / (alpha + beta) * (upper - lower) + lower;
    }

    protected double getVariance() {
        assert(this.alpha >= 1.0d && this.beta >= 1.0d) : "Alpha or beta are uncorrect";
        return alpha * beta / ((alpha + beta) * (alpha + beta)
                * (alpha + beta + 1.0d))
            * (this.upper - this.lower) * (this.upper - this.lower);
    }

    public double getError() {
        return getError(DEFAULT_LEVEL);
    }

    /**
     * Gives the current error with a predefined condidence level.
     */
    public double getError(double level) {
        /* Optimization */
        if (alpha > MAX_LUT_INDEX + 1.0d || beta > MAX_LUT_INDEX + 1.0d)
            return getEstimate() - inverseNormal((1.0d - level) / 2.0d,
                    getEstimate(), getVariance());
        
        return errorValues.getValue(alpha, beta, level);
    }

    public static final double error(double alpha, double beta, double level) {
        final double estimate = alpha / (alpha + beta);
        final double aLinear = (1.0d - level) * estimate;
        final double bLinear = level + aLinear;
        final double aCentral = (1.0d - level) / 2.0d;
        final double bCentral = level + aCentral;
        final double linear = Math.max(inverseIncompleteBeta(bLinear, alpha, beta) - estimate,
                estimate - inverseIncompleteBeta(aLinear, alpha, beta));
        final double central = Math.max(inverseIncompleteBeta(bCentral, alpha, beta) - estimate,
                estimate - inverseIncompleteBeta(aCentral, alpha, beta));
        return Math.min(linear, central);
    }

    protected boolean set(double estimate, double variance) {
        /* Optimization */
        if (estimate == getEstimate() && variance == getVariance())
            return true;

        /* Normalize estimate and variance */
        final double normalizedEstimate = (estimate - this.lower)
            / (this.upper - this.lower);
        final double normalizedVariance = variance
            / ((this.upper - this.lower) * (this.upper - this.lower));

        /* Test for admissibility of parameter */
        if (normalizedVariance <= 0.0d || normalizedVariance > 1.0d / 12.0d)
            return false;

        /* Special cases where the estimate is out of bounds or the variance is too high */
        final double product = normalizedEstimate * (1.0d - normalizedEstimate);
        if (normalizedEstimate <= 0.0d || normalizedEstimate >= 1.0d
                || normalizedVariance > product * normalizedEstimate / (1.0d + normalizedEstimate)
                || normalizedVariance > product * (1.0d - normalizedEstimate) / (2.0d - normalizedEstimate)) {
            logger.fine("Normalized mean is out of bounds");
            final double polynom = 1.0d - normalizedVariance * normalizedVariance - 11.0d * normalizedVariance;
            final double atanArg = 3.0d / (normalizedVariance + 18.0d) * Math.sqrt(3.0d * polynom / normalizedVariance);
            final double sinArg = Math.atan(atanArg) / 3.0d + Math.PI / 6.0d;
            final double shape = 2.0d / 3.0d * Math.sqrt((normalizedVariance + 3.0d) / normalizedVariance) * Math.sin(sinArg) - 4.0d / 3.0d;
            this.alpha = normalizedEstimate < 0.5d ? 1.0d : shape;
            this.beta = normalizedEstimate > 0.5d ? 1.0d : shape;
        } else {
            /* Normal computations */
            final double intermediate = normalizedEstimate
                * (1.0d - normalizedEstimate) / normalizedVariance - 1.0d;
            this.alpha = normalizedEstimate * intermediate;
            this.beta = (1.0d - normalizedEstimate) * intermediate;
        }

        assert(this.alpha >= 1.0d && this.beta >= 1.0d)
            : "Alpha or beta are uncorrect: " + alpha + ", " + beta;
        return true;
    }

    public Beta reset() {
        this.alpha = 1.0d;
        this.beta = 1.0d;
        this.lower = 0.0d;
        this.upper = 1.0d;
        return this;
    }


}
