package simdeg.util;

import java.util.logging.Logger;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Estimator for Bernoulli Trial only. It is based on Successive values
 * measures.
 */
public class BTS extends Beta {

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
        this(estimate, DEFAULT_ERROR);
    }

    public BTS(double estimate, double error) {
        this(estimate, error, DEFAULT_LEVEL);
    }

    public BTS(double estimate, double error, double level) {
        super(estimate, error);
        this.level = level;
    }

    /**
     * Internal initialization for faster cloning.
     */
    protected BTS(double alpha, double beta, double lower, double upper, double level) {
        super(alpha, beta, lower, upper);
        this.level = level;
    }

    public BTS clone() {
        return new BTS(getAlpha(), getBeta(), getLowerEndpoint(), getUpperEndpoint(), level);
    }

    public void setSample(double value) {
        super.setSample(value);
        if (value == 1.0d) {
            successiveZero = 0;
            successiveOne++;
            if (pow(getEstimate(), successiveOne) < 1.0d - level) {
                logger.fine("Reinitialization probably because of too much successive ones (" + successiveOne + ")");
                reset();
            }
        } else {
            successiveOne = 0;
            successiveZero++;
            if (pow(1.0d - getEstimate(), successiveZero) < 1.0d - level) {
                logger.fine("Reinitialization probably because of too much successive zeros (" + successiveZero + ")");
                reset();
            }
        }
    }

    protected boolean set(double estimate, double variance) {
        successiveOne = 0;
        successiveZero = 0;
        return super.set(estimate, variance);
    }

    public BTS reset() {
        successiveOne = 0;
        successiveZero = 0;
        super.reset();
        return this;
    }

}
