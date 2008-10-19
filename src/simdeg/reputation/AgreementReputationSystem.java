package simdeg.reputation;

import static simdeg.util.Collections.addElement;
import static simdeg.util.Estimator.add;
import static simdeg.util.Estimator.max;
import static simdeg.util.Estimator.min;
import static simdeg.util.Estimator.subtract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.BTS;
import simdeg.util.Estimator;

/**
 * Strategy considering failures and collusion with convergence.
 */
public class AgreementReputationSystem extends ReliableReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(AgreementReputationSystem.class.getName());

	private AgreementMatrix agreement = new AgreementMatrix(new BTS());

	/**
	 * Keep track of each update made on the AgreementMatrix (nice structure).
	 * For each job, the set of pairs of internal set to the AgreementMatrix is
	 * stored.
	 */
	private Map<Job, Set<Set<Set<Worker>>>> updatedSets = new HashMap<Job, Set<Set<Set<Worker>>>>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		agreement.addAllWorkers(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		agreement.removeAllWorkers(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers.
	 */
	protected void setAgreement(Job job, Worker worker, Set<Worker> workers) {
		assert (!workers.isEmpty()) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashSet<Set<Set<Worker>>>());
		for (Worker otherWorker : workers) {
			Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
			try {
				pair.add(agreement.getSet(worker));
				pair.add(agreement.getSet(otherWorker));
				if (!updatedSets.get(job).contains(pair)) {
					agreement.increaseAgreement(worker, otherWorker);
					updatedSets.get(job).add(pair);
				}
			} catch (IllegalArgumentException e) {
			}
		}
	}

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected void setDisagreement(Job job, Worker worker, Set<Worker> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashSet<Set<Set<Worker>>>());
		try {
			Set<Worker> setWorker = agreement.getSet(worker);
			for (Worker otherWorker : workers) {
				Set<Worker> setOtherWorker = agreement.getSet(otherWorker);
				Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
				pair.add(setWorker);
				pair.add(setOtherWorker);
				if (!updatedSets.get(job).contains(pair)
						|| setWorker == setOtherWorker) {
					agreement.decreaseAgreement(worker, otherWorker);
					updatedSets.get(job).add(pair);
				}
			}
		} catch (IllegalArgumentException e) {
		}
	}

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct answers. Workers does not contain the winning set.
	 */
	protected void setDistinctSets(Job job, Set<Worker> winningWorkers,
			Set<Set<Worker>> workers) {
		updatedSets.remove(job);
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result.
	 */
	public Estimator getCollusionLikelihood(Set<Worker> workers) {
		final Estimator[][] proba = agreement.getCollusion(workers);
		final Estimator one = new BTS(1.0d);
		Estimator estimator = new BTS(1.0d);
		for (int i = 0; i < proba.length; i++)
			estimator = min(estimator, subtract(one, proba[0][i]));
		for (int i = 1; i < proba.length; i++)
			for (int j = i; j < proba[i].length; j++)
				estimator = min(estimator, add(one, proba[i][j]).subtract(
						proba[0][i - 1]).subtract(proba[0][j]).multiply(0.5d));

		estimator = max(new BTS(0.0d), estimator);
		assert (estimator.getEstimate() >= 0.0d) : "Negative estimate: "
				+ estimator.getEstimate();

		logger.finest("Estimated collusion likelihood of " + workers.size()
				+ " workers is " + estimator);
		return estimator;
	}

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public Map<Worker, Estimator> getCollusionLikelihood(Worker worker,
			Set<Worker> workers) {
		Map<Worker, Estimator> result = new HashMap<Worker, Estimator>();
		Set<Worker> set = addElement(worker, new HashSet<Worker>());
		for (Worker otherWorker : workers) {
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
	public Estimator getColludersFraction() {
		final Set<Worker> biggest = agreement.getBiggest();
		final double fraction = 1.0d - (double) biggest.size() / workers.size();
		final Estimator result = new BTS(fraction, agreement
				.getBiggestCertainty());
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

}