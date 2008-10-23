package simdeg.util;

import java.util.logging.Logger;
import java.lang.IllegalArgumentException;
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

    private static final double DEFAULT_LEVEL = 0.95d;

    private final double level;

    private int successiveZero = 0;

    private int successiveOne = 0;

    public BTS() {
        this(0.0d);
    }

    public BTS(double estimate) {
        this(estimate, 0.01d);
    }

    public BTS(double estimate, double error) {
        this(estimate, error, DEFAULT_LEVEL);
    }

    public BTS(double estimate, double error, double level) {
        super(estimate, error);
        this.level = level;
    }

    public BTS clone() {
        return new BTS(getEstimate(), getError(), level);
    }

    public BTS clone(double value) {
        return new BTS(value, getError(), level);
    }

    public BTS clone(double value, double error) {
        return new BTS(value, error, level);
    }

    private void reinit(String msg) {
        logger.fine("Reinitialization probably because of " + msg);
        setError(1.0d);
    }

    public void setSample(double value) {
        super.setSample(value);
        if (value == 1.0d) {
            successiveZero = 0;
            successiveOne++;
            if (pow(getEstimate(), successiveOne) < 1.0d - level)
                reinit("too much successive ones (" + successiveOne + ")");
        } else {
            successiveOne = 0;
            successiveZero++;
            if (pow(1.0d - getEstimate(), successiveZero) < 1.0d - level)
                reinit("too much successive zeros (" + successiveZero + ")");
        }
    }

    void setEstimate(double estimate) {
        super.setEstimate(estimate);
        successiveOne = 0;
        successiveZero = 0;
    }

}
