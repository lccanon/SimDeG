package simdeg.util;

import java.util.logging.Logger;
import java.lang.IllegalArgumentException;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Estimator for Bernoulli Trial only. It is based on Successive values
 * measures.
 */
public class BTS extends Estimator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(BTS.class.getName());

    private static final double DEFAULT_LEVEL = 0.95d;

    private final double level;

    private int totalOne = 0;

    private int total = 0;

    private int successiveZero = 0;

    private int successiveOne = 0;

    public BTS() {
        this(0.0d);
    }

    public BTS(double estimate) {
        this(estimate, 0.99d, DEFAULT_LEVEL);
    }

    public BTS(double estimate, double consistency) {
        this(estimate, consistency, DEFAULT_LEVEL);
    }

    public BTS(double estimate, double consistency, double level) {
        setEstimate(estimate);
        setConsistency(consistency);
        this.level = level;
    }

    public BTS clone() {
        return new BTS(getEstimate(), getConsistency(), level);
    }

    public BTS clone(double value) {
        return new BTS(value, getConsistency(), level);
    }

    public BTS clone(double value, double consistency) {
        return new BTS(value, consistency, level);
    }

    private void reinit(String msg) {
        logger.fine("Reinitialization probably because of " + msg);
        totalOne = 0;
        total = 0;
    }

    public void setSample(double value) {
        if (value != 0.0d && value != 1.0d)
            throw new IllegalArgumentException
                ("Estimator designed for Bernoulli trial only");
        if (totalOne > total)
            reinit("incorrect data structure");
        if (value == 1.0d) {
            successiveZero = 0;
            successiveOne++;
            if (pow((totalOne+1.0d)/(total+1.0d), successiveOne) < 1.0d - level)
                reinit("too much successive ones (" + successiveOne + ")");
            totalOne++;
        } else {
            successiveOne = 0;
            successiveZero++;
            if (pow(1.0d-totalOne/(total+1.0d), successiveZero) < 1.0d - level)
                reinit("too much successive zeros (" + successiveZero + ")");
        }
        total++;
    }

    public double getEstimate() {
        if (total == 0)
            return 0.5d;
        return (double)totalOne / total;
    }

    void setEstimate(double estimate) {
        total = Math.max(10000, total);
        totalOne = (int) Math.round(estimate * total);
    }

    public double getConsistency() {
        return Math.max(0.0d, 1.0d - 2.0d / sqrt(total));
    }

    void setConsistency(double consistency) {
        if (consistency < 0.0d || consistency >= 1.0d)
            throw new IllegalArgumentException();
        successiveZero = 0;
        successiveOne = 0;
        final double estimate = (double)totalOne / total;
        total = (int) Math.round((2.0d / (1.0d - consistency))
            * (2.0d / (1.0d - consistency)));
        totalOne = (int) Math.round(estimate * total);
    }

    public double getConsistencyWith(Estimator estimator) {
        final double minConsistency = Math.min(getConsistency(), 
                estimator.getConsistency());
        if (1.0d - minConsistency < Math.abs(getEstimate()
                    - estimator.getEstimate()))
            return 0.0d;
        return minConsistency;
    }

}
