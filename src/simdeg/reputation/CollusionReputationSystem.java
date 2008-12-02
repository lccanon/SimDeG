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
public final class CollusionReputationSystem extends SkeletonReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(CollusionReputationSystem.class.getName());

	private final CollusionMatrix collusion = new CollusionMatrix(new BTS());

	/**
	 * Keep track of each update made on the CollusionMatrix (nice structure).
	 * For each job, the set of pairs of internal set to the AgreementMatrix is
	 * stored. This allows to avoid long sequence of similar events which would
     * pertube the estimators.
	 */
	private final Map<Job, Set<Set<Set<Worker>>>> updatedSets = new HashMap<Job, Set<Set<Set<Worker>>>>();

	/**
	 * Gives participating workers.
	 */
	public final void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		collusion.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public final void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		collusion.removeAll(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers. Useless in this class.
	 */
	protected final void setAgreement(Job job, Worker worker, Set<Worker> workers) {
		assert (!workers.isEmpty()) : "Not enough workers in group";
	}

	/**
	 * Specifies that a worker disagrees with a set of workers. Non-observed collusion.
	 */
	protected final void setDisagreement(Job job, Worker worker, Set<Worker> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashSet<Set<Set<Worker>>>());
		try {
            final Set<Worker> setWorker = collusion.getSet(worker);
            /* Split the worker from every set with which it disagrees */
            for (Worker otherWorker : workers)
                if (setWorker == collusion.getSet(otherWorker)) {
                    final Set<Worker> biggest = collusion.getBiggest();
                    collusion.split(setWorker, worker);
                    /* Test wether to readapt or not */
                    if (biggest != collusion.getBiggest())
                        collusion.readapt();
                }
            /* Update each element in the collusion matrix only once per computed job */
            for (Worker otherWorker : workers) {
                final Set<Worker> setOtherWorker = collusion.getSet(otherWorker);
                Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
				pair.add(setWorker);
				pair.add(setOtherWorker);
				if (!updatedSets.get(job).contains(pair)
						|| setWorker == setOtherWorker) {
					collusion.decreaseCollusion(worker, otherWorker);
					updatedSets.get(job).add(pair);
				}
			}
		} catch (NoSuchElementException e) {
            logger.log(Level.SEVERE, "Some workers seems unknown to the collusion matrix", e);
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
        /* Stores the set of each other workers, because their constitutions
         * could variate at each call to decreaseCollusion or increaseCollusion.
         * It allows to limit the number of updates by keeping track of initial
         * sets. */
        final Map<Worker, Set<Worker>> setsOtherWorker = new HashMap<Worker, Set<Worker>>();
        for (Worker otherWorker : winningWorkers)
            setsOtherWorker.put(otherWorker, collusion.getSet(otherWorker));
        for (Set<Worker> workersSet : workers)
            for (Worker otherWorker : workersSet)
                setsOtherWorker.put(otherWorker, collusion.getSet(otherWorker));
        /* Observed collusion */
        for (Set<Worker> workersSet : workers)
            for (Worker worker : workersSet) {
                final Set<Worker> setWorker = collusion.getSet(worker);
                /* Update each element in the collusion matrix only once per computed job */
                for (Worker otherWorker : workersSet) {
                    final Set<Worker> setOtherWorker = setsOtherWorker.get(otherWorker);
                    Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
                    pair.add(setWorker);
                    pair.add(setOtherWorker);
                    if (!updatedSets.get(job).contains(pair)) {
                        collusion.increaseCollusion(worker, otherWorker);
                        updatedSets.get(job).add(pair);
                    }
                }
            }
        /* Non-colluders */
        for (Worker worker : winningWorkers) {
            final Set<Worker> setWorker = collusion.getSet(worker);
            /* Update each element in the collusion matrix only once per computed job */
            for (Worker otherWorker : winningWorkers) {
                final Set<Worker> setOtherWorker = setsOtherWorker.get(otherWorker);
                Set<Set<Worker>> pair = new HashSet<Set<Worker>>();
                pair.add(setWorker);
                pair.add(setOtherWorker);
                if (!updatedSets.get(job).contains(pair)) {
                    collusion.decreaseCollusion(worker, otherWorker);
                    updatedSets.get(job).add(pair);
                }
            }
        }
		updatedSets.remove(job);
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result.
	 */
	public final Estimator getCollusionLikelihood(Set<? extends Worker> workers) {
		final Estimator[][] proba = collusion.getCollusions(workers);
		Estimator estimator = new BTS(1.0d);
		for (int i = 0; i < proba.length; i++)
			for (int j = i; j < proba[i].length; j++)
				estimator = min(estimator, proba[i][j]);
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
		final Set<Worker> biggest = collusion.getBiggest();
		final double fraction = 1.0d - (double) biggest.size() / workers.size();
		final Estimator result = new BTS(fraction, collusion.getBiggestError());
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

    public final String toString() {
        return "collusion-based reputation system";
    }

}
