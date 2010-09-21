package simdeg.util;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static simdeg.util.InverseMath.inverseStandardNormal;

/**
 * Estimator for Bernoulli Trial only. It is based on Bayesian estimation.
 */
public class BetaEstimator extends Estimator {

	/** Allows to limit the errors done */
	private static final double EPSILON = 1E-5;

	private Beta beta = new Beta();

	protected double getLowerEndpoint() {
		return beta.getLowerEndpoint();
	}

	protected double getUpperEndpoint() {
		return beta.getUpperEndpoint();
	}

	public double getMean() {
		return beta.getMean();
	}

	public double getError() {
		return beta.getError();
	}

	public double getVariance() {
		return beta.getVariance();
	}

	protected double getAlpha() {
		return beta.getAlpha();
	}

	protected double getBeta() {
		return beta.getBeta();
	}

	/**
	 * Initializes without any information.
	 */
	public BetaEstimator() {
		super(0.0d, 1.0d);
		clear();
	}

	/**
	 * Initializes with precise information.
	 */
	public BetaEstimator(double estimate) {
		super(0.0d, 1.0d);
		/* Test for admissibility of parameters */
		if (estimate < 0.0d || estimate > 1.0d)
			throw new OutOfRangeException(estimate, 0.0d, 1.0d);
		set(0.0d, 1.0d, estimate, 0.0d);
	}

	protected BetaEstimator(double alpha, double beta) {
		super(0.0d, 1.0d);
		if (alpha < 1.0d)
			throw new OutOfRangeException(alpha, 1.0d, Double.MAX_VALUE);
		if (beta < 1.0d)
			throw new OutOfRangeException(beta, 1.0d, Double.MAX_VALUE);
		this.beta = new Beta(0.0d, 1.0d, alpha, beta);
	}

	public BetaEstimator clone() {
		return new BetaEstimator(getAlpha(), getBeta());
	}

	private final double roundIfNearInt(double value) {
		final double rounded = round(value);
		if (abs(rounded - value) < EPSILON)
			return rounded;
		return value;
	}

	protected BetaEstimator set(double lower, double upper, double estimate,
			double variance) {
		super.set(lower, upper, estimate, variance);
		beta.setRange(lower, upper);

		/* Normalize estimate and variance */
		double normalizedEstimate = Math.max(0.0d, Math.min(1.0d,
				(estimate - lower) / (upper - lower)));
		double normalizedVariance = Math.max(Beta.DEFAULT_VARIANCE, variance
				/ ((upper - lower) * (upper - lower)));

		/* Optimization */
		if (normalizedVariance > 1.0d / 12.0d || this.upper == this.lower)
			return clear();

		final double product = normalizedEstimate * (1.0d - normalizedEstimate);
		if (normalizedVariance > product * normalizedEstimate
				/ (1.0d + normalizedEstimate)
				|| normalizedVariance > product * (1.0d - normalizedEstimate)
						/ (2.0d - normalizedEstimate))
			return setSpecific(normalizedEstimate, normalizedVariance);

		/* Optimization */
		if (estimate == getMean() && variance == getVariance())
			return this;

		beta.set(lower, upper, normalizedEstimate * (upper - lower) + lower,
				normalizedVariance * (upper - lower) * (upper - lower));

		/* For keeping errors low */
		if (round(getAlpha()) != 0.0d)
			beta.setAlpha(roundIfNearInt(getAlpha()));
		if (round(getBeta()) != 0.0d)
			beta.setBeta(roundIfNearInt(getBeta()));

		return this;
	}

	/**
	 * Adjust the mean in order to stick to the variance as much as possible (it
	 * allows the estimator to catch the precision for the estimate later),
	 * while having alpha and beta greater than 1 (necessity for an Estimator).
	 */
	private BetaEstimator setSpecific(double normalizedEstimate,
			double normalizedVariance) {
		final double alpha = 2.0d
				/ 3.0d
				* sqrt((normalizedVariance + 3.0d) / normalizedVariance)
				* sin((atan(3.0d
						/ (normalizedVariance + 18.0d)
						* sqrt(3.0d
								* (1.0d - normalizedVariance
										* normalizedVariance - 11.0d * normalizedVariance)
								/ normalizedVariance)))
						/ 3.0d + PI / 6.0d) - 4.0d / 3.0d;
		if (normalizedEstimate < 0.5d) {
			beta.setAlpha(1.0d);
			beta.setBeta(alpha);
		} else {
			beta.setAlpha(alpha);
			beta.setBeta(1.0d);
		}
		return this;
	}

	/**
	 * Override RV.opposite for performance reason.
	 */
	protected BetaEstimator opposite() {
		super.set(-this.upper, -this.lower, 0.0d, 0.0d);
		beta.setRange(this.lower, this.upper);
		final double alphaValue = getBeta();
		final double betaValue = getAlpha();
		beta.setAlpha(alphaValue);
		beta.setBeta(betaValue);
		return this;
	}

	/**
	 * Notifies a new value (either 1 or 0).
	 */
	public void setSample(double value) {
		if (value != 0.0d && value != 1.0d)
			throw new IllegalArgumentException(
					"Estimator designed for Bernoulli trial only");
		if (value == 1.0d)
			beta.setAlpha(getAlpha() + 1.0d);
		else
			beta.setBeta(getBeta() + 1.0d);
	}

	/**
	 * Number of observed values.
	 */
	public double getSampleCount() {
		return getAlpha() + getBeta() - 2.0d;
	}

	public double getSampleCount(double value) {
		if (value == 1.0d)
			return roundIfNearInt(getAlpha() - 1.0d);
		if (value == 0.0d)
			return roundIfNearInt(getBeta() - 1.0d);
		throw new IllegalArgumentException(
				"Estimator designed for Bernoulli trial only");
	}

	public double sampleCountLimit(double error) {
		final double sd = error
				/ inverseStandardNormal(DEFAULT_ERROR_LEVEL / 2.0d + 0.5d);
		final double alpha = (1.0d / (4.0d * sd * sd) - 1.0d) / 2.0d;
		return (alpha - 1.0d) * 2.0d;
	}

	public BetaEstimator clear() {
		beta.setAlpha(1.0d);
		beta.setBeta(1.0d);
		return this;
	}

	public BetaEstimator merge(Estimator estimator) {
		final BetaEstimator cast = (BetaEstimator) estimator;
		final double count = Math.max(getSampleCount(), cast.getSampleCount());
		beta.setAlpha(Math.max(getAlpha(), cast.getAlpha()));
		beta.setBeta(Math.max(getBeta(), cast.getBeta()));
		final double ratio = (count + 2.0d) / (getSampleCount() + 2.0d);
		beta.setAlpha(getAlpha() * ratio);
		beta.setBeta(getBeta() * ratio);
		return this;
	}

	public String toString() {
		return beta.toString();
	}

}