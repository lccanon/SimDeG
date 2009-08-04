package simdeg.util;

import java.util.logging.Logger;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Estimator for Bernoulli Trial only. It is based on Successive values
 * measures.
 */
public class BTS extends BetaEstimator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(BTS.class.getName());

    private double level = DEFAULT_LEVEL;

    private int successiveZero = 0;

    private int successiveOne = 0;

    public BTS() {
        super();
    }

    public BTS(double estimate) {
        this(estimate, DEFAULT_LEVEL);
    }

    public BTS(double estimate, double level) {
        super(estimate);
        this.level = level;
    }

    /**
     * Internal initialization for faster cloning.
     */
    protected BTS(double alpha, double beta, double level) {
        super(alpha, beta);
        this.level = level;
    }

    public BTS clone() {
        return new BTS(getAlpha(), getBeta(), level);
    }

    public void setSample(double value) {
        super.setSample(value);
        if (value == 1.0d) {
            successiveZero = 0;
            successiveOne++;
            if (pow(getMean(), successiveOne) < 1.0d - level) {
                logger.fine("Reinitialization probably because of too much successive ones (" + successiveOne + ")");
                clear();
            }
        } else {
            successiveOne = 0;
            successiveZero++;
            if (pow(1.0d - getMean(), successiveZero) < 1.0d - level) {
                logger.fine("Reinitialization probably because of too much successive zeros (" + successiveZero + ")");
                clear();
            }
        }
    }

    protected BTS set(double lower, double upper, double estimate, double variance) {
        super.set(lower, upper, estimate, variance);
        successiveOne = 0;
        successiveZero = 0;
        return this;
    }

    public BTS clear() {
        super.clear();
        successiveOne = 0;
        successiveZero = 0;
        return this;
    }

}
