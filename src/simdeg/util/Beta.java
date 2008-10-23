package simdeg.util;

import java.util.logging.Logger;
import java.lang.IllegalArgumentException;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Estimator for Bernoulli Trial only. It is based on Bayesian estimation.
 */
public class Beta extends Estimator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Beta.class.getName());

    private int totalOne = 0;

    private int total = 0;

    public Beta() {
        this(0.0d);
    }

    public Beta(double estimate) {
        this(estimate, 0.01d);
    }

    public Beta(double estimate, double error) {
        setEstimate(estimate);
        setError(error);
    }

    public Beta clone() {
        return new Beta(getEstimate(), getError());
    }

    public Beta clone(double value) {
        return new Beta(value, getError());
    }

    public Beta clone(double value, double error) {
        return new Beta(value, error);
    }

    public void setSample(double value) {
        if (value != 0.0d && value != 1.0d)
            throw new IllegalArgumentException
                ("Estimator designed for Bernoulli trial only");
        if (value == 1.0d)
            totalOne++;
        total++;
    }

    public double getEstimate() {
        return (totalOne + 1.0d) / (total + 2.0d);
    }

    void setEstimate(double estimate) {
        total = Math.max(10000, total);
        if (estimate == 0.0d)
            totalOne = 0;
        else if (estimate == 1.0d)
            totalOne = total;
        else
            totalOne = (int) Math.round(estimate * (total + 2)) - 1;
    }

    // TODO fix it with qbeta func and a confidence level of 5%
    public double getError() {
        return Math.min(1.0d, 2.0d / sqrt(total));
    }

    void setError(double error) {
        if (error < 0.0d || error > 1.0d)
            throw new IllegalArgumentException();
        if (error == 1.0d) {
            total = 0;
            totalOne = 0;
        } else {
            final double estimate = (double)totalOne / total;
            total = (int) Math.round(4.0d / (error * error));
            totalOne = (int) Math.round(estimate * (total + 2)) - 1;
        }
    }

    public double getConsistency(Estimator estimator) {
        final double maxError = Math.max(getError(),
                estimator.getError());
        if (Math.abs(getEstimate() - estimator.getEstimate()) > maxError)
            return 0.0d;
        return 1.0d - maxError;
    }

}
