package simdeg.util;

import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.Collection;

public abstract class Estimator {

    public abstract Estimator clone();

    public abstract Estimator clone(double value);

    public abstract Estimator clone(double value, double error);

    public abstract void setSample(double value);

    public abstract double getEstimate();

    abstract void setEstimate(double estimate);

    public abstract double getError();

    abstract void setError(double error);

    public abstract double getConsistency(Estimator estimator);

    public static Estimator inverse(Estimator estimator) {
        return estimator.clone().inverse();
    }

    public Estimator inverse() {
        final double error = getError();
        setEstimate(-getEstimate());
        setError(error);
        return this;
    }

    public static Estimator max(Estimator v1, Estimator v2) {
        System.out.println(v1 + " " + v2);
        if (v1.getEstimate() > v2.getEstimate())
            return v1.clone();
        else
            return v2.clone();
    }

    public static Estimator min(Estimator v1, Estimator v2) {
        return max(inverse(v1), inverse(v2)).inverse();
    }

    public static Estimator add(Estimator v1, Estimator v2) {
        return v1.clone().add(v2);
    }

    //XXX try with weights
    public Estimator add(Estimator estimator) {
        setEstimate(estimator.getEstimate() + getEstimate());
        setError((estimator.getError() + getError()) / 2.0d);
        return this;
    }

    public static Estimator subtract(Estimator v1, Estimator v2) {
        return v1.clone().subtract(v2);
    }

    public Estimator subtract(Estimator estimator) {
        inverse();
        add(estimator);
        return inverse();
    }

    public static Estimator multiply(Estimator v1, double d) {
        return v1.clone().multiply(d);
    }

    public Estimator multiply(double d) {
        setEstimate(d * getEstimate());
        return this;
    }

    public static Estimator mean(Collection<? extends Estimator> estimators) {
        double numinator = 0.0d;
        double denominator = 0.0d;
        for (Estimator estimator : estimators) {
            final double error = estimator.getError();
            numinator += (1.0d - error) * estimator.getEstimate();
            denominator += (1.0d - error);
        }
        final Estimator result = estimators.iterator().next().clone();
        result.setEstimate(numinator/denominator);
        result.setError(1.0d - denominator/estimators.size());
        return result;
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("0.##",
                new DecimalFormatSymbols(Locale.ENGLISH));
        return "{" + df.format(getEstimate()) + ","
            + df.format(getError()) + "}";
    }

}
