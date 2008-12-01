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
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;

import simdeg.util.BTS;
import simdeg.util.Estimator;

/**
 * Strategy considering failures and collusion with convergence.
 */
public final class AgreementReputationSystem extends ReliableReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(AgreementReputationSystem.class.getName());

	private final AgreementMatrix agreement = new AgreementMatrix(new BTS(1.0d));

	/**
	 * Keep track of each update made on the AgreementMatrix.
	 * For each job, the set of pairs of internal set to the AgreementMatrix is
	 * stored. This allows to avoid long sequence of similar event which would
	 * pertube the estimators.
	 */
	private final Map<Job, Set<Set<Set<Worker>>>> updatedSets = new HashMap<Job, Set<Set<Set<Worker>>>>();

	/**
	 * Gives participating workers.
	 */
	public final void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		agreement.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public final void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		agreement.removeAll(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers.
	 */
	protected final void setAgreement(Job job, Worker worker, Set<Worker> workers) {
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
			} catch (NoSuchElementException e) {
                logger.log(Level.SEVERE, "Some workers seems unknown to the agreement matrix", e);
                System.exit(1);
			}
		}
	}

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected final void setDisagreement(Job job, Worker worker, Set<Worker> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashSet<Set<Set<Worker>>>());
		try {
            final Set<Worker> setWorker = agreement.getSet(worker);
            /* Stores the set of each other workers, because their constitutions
             * could variate at each call to decreaseAgreement */
            final Map<Worker, Set<Worker>> setsOtherWorker = new HashMap<Worker, Set<Worker>>();
            for (Worker otherWorker : workers)
                setsOtherWorker.put(otherWorker, agreement.getSet(otherWorker));
            /* Update each element in the agreement matrix only once per computed job,
             * except if disagreements happen in the same set */
			for (Worker otherWorker : workers) {
                final Set<Worker> setOtherWorker = setsOtherWorker.get(otherWorker);
				Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
				pair.add(setWorker);
				pair.add(setOtherWorker);
				if (!updatedSets.get(job).contains(pair) || setWorker == setOtherWorker) {
					agreement.decreaseAgreement(worker, otherWorker);
					updatedSets.get(job).add(pair);
				}
			}
		} catch (NoSuchElementException e) {
            logger.log(Level.SEVERE, "Some workers seems unknown to the agreement matrix", e);
            System.exit(1);
		}
	}

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct answers. Workers does not contain the winning set.
	 */
	protected final void setDistinctSets(Job job, Set<Worker> winningWorkers,
			Set<Set<Worker>> workers) {
        assert (!winningWorkers.isEmpty()) : "No winning workers";
		updatedSets.remove(job);
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result.
	 */
	public final Estimator getCollusionLikelihood(Set<? extends Worker> workers) {
		final Estimator[][] proba = agreement.getAgreements(workers);
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
	public final <W extends Worker> Map<W, Estimator> getCollusionLikelihood(W worker,
			Set<W> workers) {
		Map<W, Estimator> result = new HashMap<W, Estimator>();
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
	public final Estimator getColludersFraction() {
		final Set<Worker> biggest = agreement.getBiggest();
		final double fraction = 1.0d - (double) biggest.size() / workers.size();
		final Estimator result = new BTS(fraction, agreement.getBiggestError());
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

    public final String toString() {
        return "agreement-based reputation system";
    }

}
