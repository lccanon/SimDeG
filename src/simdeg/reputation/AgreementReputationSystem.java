package simdeg.reputation;

import static simdeg.util.Collections.addElement;
import static simdeg.util.RV.add;
import static simdeg.util.RV.multiply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.Beta;
import simdeg.util.BetaEstimator;
import simdeg.util.RV;

/**
 * Strategy considering failures and collusion with convergence.
 */
public class AgreementReputationSystem<W extends Worker> extends
		SkeletonReputationSystem<W> {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(AgreementReputationSystem.class.getName());

	private final AgreementMatrix<W> agreement = new AgreementMatrix<W>(
			new BetaEstimator());

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends W> workers) {
		super.addAllWorkers(workers);
		agreement.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends W> workers) {
		super.removeAllWorkers(workers);
		agreement.removeAll(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers.
	 */
	protected void setAgreement(Job job, W worker, Set<W> workers) {
		assert (!workers.isEmpty()) : "Not enough workers in group";
		for (W otherWorker : workers) {
			final Set<W> setWorker = agreement.getSet(worker);
			final Set<W> setOtherWorker = agreement.getSet(otherWorker);
			if (updateInteraction(job, worker, otherWorker, setWorker,
					setOtherWorker)) {
				agreement.increaseAgreement(worker, otherWorker);
				adaptInteractionStructure(job, setWorker, setOtherWorker,
						agreement.getSet(worker), agreement.getSet(otherWorker));
			}
		}
	}

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected void setDisagreement(Job job, W worker, Set<W> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
		for (W otherWorker : workers) {
			final Set<W> setWorker = agreement.getSet(worker);
			final Set<W> setOtherWorker = agreement.getSet(otherWorker);
			if (updateInteraction(job, worker, otherWorker, setWorker,
					setOtherWorker)) {
				agreement.decreaseAgreement(worker, otherWorker);
				adaptInteractionStructure(job, setWorker, setOtherWorker,
						agreement.getSet(worker), agreement.getSet(otherWorker));
			}
		}
	}

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct results. Workers does not contain the winning set. Useless in
	 * this class.
	 */
	protected void setDistinctSets(Job job, Set<W> winningWorkers,
			Set<Set<W>> workers) {
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result. Since getAgreements return a truncated matrix, it is normal
	 * that proba[0][i - 1] is used in this way.
	 */
	public RV getCollusionLikelihood(Set<W> workers) {
		final RV[][] proba = agreement.getAgreements(workers);
		// TODO put the following into a function 
//		String probabilities = "[";
//		for (RV[] prob : proba)
//			probabilities += Arrays.toString(prob) + ", ";
//		probabilities += "]";
//		String size = "";
//		final List<Set<W>> sets = new ArrayList<Set<W>>(agreement.getSets(workers));
//		for (Set<W> set : sets)
//			size += set.size() + ", ";
//		logger.finest("Agreement probabilities are " + probabilities + " and size " + size);
		final RV rv = new Beta(1.0d);
		for (int i = 0; i < proba[0].length; i++)
			rv.min(multiply(proba[0][i], -1.0d).add(1.0d));
		for (int i = 1; i < proba.length; i++)
			for (int j = i; j < proba[i].length; j++) {
				final RV other = add(proba[i][j], 1.0d).subtract(
						proba[0][i - 1]).subtract(proba[0][j]).multiply(0.5d)
						.truncateRange(0.0d, 1.0d);
				// TODO deals with correlation with a clean method
				rv.min(other, 1.0d).min(proba[i][j]);
			}

		assert (rv.getMean() >= 0.0d) : "Negative estimate: " + rv.getMean();

		logger.finer("Estimated collusion likelihood of " + workers.size()
				+ " workers in " + proba.length + " observed groups is " + rv);
		return rv;
	}

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public Map<W, RV> getCollusionLikelihood(W worker, Set<W> workers) {
		Map<W, RV> result = new HashMap<W, RV>();
		Set<W> set = addElement(worker, new HashSet<W>());
		for (W otherWorker : workers) {
			set.add(otherWorker);
			result.put(otherWorker, getCollusionLikelihood(set));
			set.remove(otherWorker);
		}
		logger.finest("Estimated collusion likelihoods of worker " + worker
				+ " is " + result);
		return result;
	}

	/**
	 * Returns the estimated fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public RV getColludersFraction() {
		final int countAgreer = agreement.countAgreerMajority();
		final double fraction = 1.0d - (double) countAgreer / workers.size();
		final RV result = new RV(0.0d, 1.0d) {
			double error = agreement.getGeneralError();

			public RV clone() {
				return this;
			}

			public double getMean() {
				return fraction;
			}

			public double getVariance() {
				return 0.0d;
			}

			public double getError() {
				return error;
			}
		};
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

	public Set<? extends Set<W>> getGroups(Collection<W> workers) {
		return agreement.getSets(workers);
	}
	
	public Set<W> getLargestGroup() {
		return agreement.getLargest();
	}

	public String toString() {
		return super.toString() + "Agreement-based reputation system:\n"
				+ agreement.toString();
	}

}
