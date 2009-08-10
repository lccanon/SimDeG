package simdeg.util;

/**
 * Similar to a RV in that it tries to gives an estimation of a succession of
 * observation.
 */
public abstract class Estimator extends RV {

    /**
     * Default confidence level concerning the frequency and sensitivity of
     * the reinitialization.
     */
    //protected static final double DEFAULT_REINIT_LEVEL = 0.95d;
    public static double DEFAULT_REINIT_LEVEL = 0.95d;

    protected Estimator(double lower, double upper) {
        super(lower, upper);
    }

    /**
     * Generic duplication and creation.
     */
    public abstract Estimator clone();

    /**
     * Updates new values.
     */
    public abstract void setSample(double value);

    /**
     * Number of observed values. Due to arithmetic operation, this values can
     * be real.
     */
    public abstract double getSampleCount();

    /**
     * The number of measures to do in order to have an error lower that the
     * argument.
     */
    public abstract double sampleCountLimit(double error);

    /**
     * Clear the measures of this estimator (keep the bounds, but put the
     * certainty to max).
     */
    public abstract Estimator clear();

    /**
     * When merging two estimator's observations without propagating errors.
     */
    public abstract Estimator merge(Estimator estimator);

    public static Estimator merge(Estimator e1, Estimator e2) {
        return e1.clone().merge(e2);
    }

}
