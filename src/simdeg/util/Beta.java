package simdeg.util;

import static simdeg.util.InverseMath.inverseIncompleteBeta;
import static simdeg.util.InverseMath.inverseNormal;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import static java.lang.Math.sqrt;

/**
 * Beta distribution.
 */
public class Beta extends RV {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Beta.class.getName());

	/** Default variance of precise estimator */
	protected static final double DEFAULT_VARIANCE = (DEFAULT_ERROR / 6.0d)
			* (DEFAULT_ERROR / 6.0d);

	/** Trinary LUT for minimizing calls to complex functions */
	private static TriLUT<Double, Double, Double, Double> errorValues = null;

	/** Number of values stored in the LUT */
	private static final int MAX_LUT_INDEX = 100;

	/**
	 * Generates a LUT for fastening complex computations.
	 */
	static {
		try {
			Method errorMethod = Beta.class.getMethod("error", Double.TYPE,
					Double.TYPE, Double.TYPE);
			errorValues = new TriLUT<Double, Double, Double, Double>(
					errorMethod, new Double[] { 1.0d, MAX_LUT_INDEX + 1.0d,
							1.0d }, new Double[] { 1.0d, MAX_LUT_INDEX + 1.0d,
							1.0d }, new Double[] { DEFAULT_ERROR_LEVEL,
							DEFAULT_ERROR_LEVEL, 1.0d });
		} catch (NoSuchMethodException e) {
			logger.log(Level.SEVERE, "The method error was not found", e);
			System.exit(1);
		}
	}

	/** Principal paramater representing quantity of one values */
	private double alpha = 1.0d;

	/** Principal paramater representing quantity of zero values */
	private double beta = 1.0d;

	/** If beta and alpha are equals to 0, then it is a simple Bernoulli RV */
	private double bernoulli = 0.5d;

	/**
	 * Initializes without any information.
	 */
	public Beta() {
		super(0.0d, 1.0d);
	}

	/**
	 * Initializes with precise information.
	 */
	public Beta(double estimate) {
		super(0.0d, 1.0d);
		/* Test for admissibility of parameters */
		if (estimate < 0.0d || estimate > 1.0d)
			throw new OutOfRangeException(estimate, 0.0d, 1.0d);
		set(0.0d, 1.0d, estimate, 0.0d);
	}

	/**
	 * Internal initialization for faster cloning.
	 */
	Beta(double lower, double upper, double alpha, double beta) {
		this(lower, upper, alpha, beta, alpha / (alpha + beta));
	}

	/**
	 * Internal initialization for faster cloning.
	 */
	Beta(double lower, double upper, double alpha, double beta,
			double bernouilli) {
		super(lower, upper);
		/* Test for admissibility of parameters */
		if (alpha < 0.0d)
			throw new OutOfRangeException(alpha, 0.0d, Double.MAX_VALUE);
		if (beta < 0.0d)
			throw new OutOfRangeException(beta, 0.0d, Double.MAX_VALUE);
		this.alpha = alpha;
		this.beta = beta;
		this.bernoulli = bernouilli;
	}

	protected double getAlpha() {
		return this.alpha;
	}

	protected double getBeta() {
		return this.beta;
	}

	protected void setAlpha(double alpha) {
		if (alpha < 0.0d)
			throw new OutOfRangeException(alpha, 0.0d, Double.MAX_VALUE);
		this.alpha = alpha;
	}

	protected void setBeta(double beta) {
		if (beta < 0.0d)
			throw new OutOfRangeException(beta, 0.0d, Double.MAX_VALUE);
		this.beta = beta;
	}

	public Beta clone() {
		return new Beta(lower, upper, alpha, beta, bernoulli);
	}

	/**
	 * Gives the current estimate.
	 */
	public double getMean() {
		if (alpha == 0.0d && beta == 0.0d)
			return bernoulli * (upper - lower) + lower;
		return alpha / (alpha + beta) * (upper - lower) + lower;
	}

	public double getVariance() {
		assert (this.alpha >= 0.0d && this.beta >= 0.0d) : "Alpha or beta are uncorrect";
		if (alpha == 0.0d && beta == 0.0d)
			return bernoulli * (1.0d - bernoulli) * (this.upper - this.lower)
					* (this.upper - this.lower);
		return alpha * beta
				/ ((alpha + beta) * (alpha + beta) * (alpha + beta + 1.0d))
				* (this.upper - this.lower) * (this.upper - this.lower);
	}

	public double getError() {
		return getError(DEFAULT_ERROR_LEVEL);
	}

	/**
	 * Gives the current error with a predefined confidence level.
	 */
	private double getError(double level) {
		/* Optimization */
		if ((alpha == 0.0d || beta == 0.0d) && alpha != beta)
			return 0.0d;

		/* Gaussian method (approximation for high value of alpha and beta) */
		if (alpha > MAX_LUT_INDEX + 1.0d || beta > MAX_LUT_INDEX + 1.0d
				|| alpha < 1.0d || beta < 1.0d)
			return getMean()
					- inverseNormal((1.0d - level) / 2.0d, getMean(),
							getVariance());

		/* More accurate method (longer) */
		return errorValues.getValue(alpha, beta, level)
				* (this.upper - this.lower);
	}

	public static final double error(double alpha, double beta, double level) {
		if (alpha == beta && alpha < 0.5d)
			return 1.0d;
		final double estimate = alpha / (alpha + beta);
		final double aLinear = (1.0d - level) * estimate;
		final double bLinear = level + aLinear;
		final double aCentral = (1.0d - level) / 2.0d;
		final double bCentral = level + aCentral;
		final double linear = Math.max(inverseIncompleteBeta(bLinear, alpha,
				beta)
				- estimate, estimate
				- inverseIncompleteBeta(aLinear, alpha, beta));
		final double central = Math.max(inverseIncompleteBeta(bCentral, alpha,
				beta)
				- estimate, estimate
				- inverseIncompleteBeta(aCentral, alpha, beta));
		return Math.min(linear, central);
	}

	/**
	 * Adjust the mean and stick to the variance if both values are
	 * incompatible.
	 */
	protected Beta set(double lower, double upper, double estimate,
			double variance) {
		setRange(lower, upper);

		/* Optimization */
		if (estimate == getMean() && variance == getVariance())
			return this;

		/* Normalize variance in the feasible range */
		final double normalizedVariance = Math
				.min(
						0.25d,
						Math
								.max(
										DEFAULT_VARIANCE,
										variance
												/ ((this.upper - this.lower) * (this.upper - this.lower))));

		/* Feasible bound for the mean */
		final double root = sqrt(1.0d - 4.0d * normalizedVariance);
		final double minLower = (1.0d - root) / 2.0d;
		final double maxUpper = (1.0d + root) / 2.0d;

		/* Normalize estimate in the feasible range */
		double normalizedEstimate = (estimate - lower) / (upper - lower);
		normalizedEstimate = Math.max(minLower, normalizedEstimate);
		normalizedEstimate = Math.min(maxUpper, normalizedEstimate);

		/* Parameters computation */
		final double intermediate = normalizedEstimate
				* (1.0d - normalizedEstimate) / normalizedVariance - 1.0d;
		this.alpha = Math.max(0.0d, normalizedEstimate * intermediate);
		this.beta = Math.max(0.0d, (1.0d - normalizedEstimate) * intermediate);
		this.bernoulli = normalizedEstimate;

		assert (this.alpha >= 0.0d && this.beta >= 0.0d) : "Alpha or beta are uncorrect: "
				+ alpha + ", " + beta;
		return this;
	}

	/**
	 * Override RV.opposite for performance reason.
	 */
	protected Beta opposite() {
		setRange(-this.upper, -this.lower);
		final double alpha = this.beta;
		final double beta = this.alpha;
		this.alpha = alpha;
		this.beta = beta;
		this.bernoulli = 1.0d - this.bernoulli;
		return this;
	}

	protected Beta setRange(double lower, double upper) {
		super.set(lower, upper, 0.0d, 0.0d);
		return this;
	}

	public String toString() {
		DecimalFormat df = new DecimalFormat("0.##", new DecimalFormatSymbols(
				Locale.ENGLISH));
		return super.toString() + "/(" + df.format(alpha) + ","
				+ df.format(beta) + "," + df.format(getLowerEndpoint()) + ","
				+ df.format(getUpperEndpoint()) + ")";
	}

}