package simdeg.reputation;

import static simdeg.util.Collections.addElement;
import static simdeg.util.RV.add;
import static simdeg.util.RV.max;
import static simdeg.util.RV.min;
import static simdeg.util.RV.subtract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import simdeg.util.RV;
import simdeg.util.Beta;
import simdeg.util.BetaEstimator;

/**
 * Strategy considering failures and collusion with convergence.
 */
public class CollusionReputationSystem extends SkeletonReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(CollusionReputationSystem.class.getName());

	private final CollusionMatrix collusion
        = new CollusionMatrix(new BetaEstimator());

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		collusion.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		collusion.removeAll(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers. Useless in this class.
	 */
	protected void setAgreement(Job job, Worker worker, Set<Worker> workers) {
		assert (!workers.isEmpty()) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashMap<Set<Worker>, Set<Set<Worker>>>());
	}

	/**
	 * Specifies that a worker disagrees with a set of workers. Non-observed collusion.
	 */
	protected void setDisagreement(Job job, Worker worker, Set<Worker> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashMap<Set<Worker>, Set<Set<Worker>>>());
        final Set<Worker> setWorker = collusion.getSet(worker);
        /* Split the workers from every set with which it disagrees */
        for (Worker otherWorker : workers) {
            final Set<Worker> setOtherWorker = collusion.getSet(otherWorker);
            if (setWorker == setOtherWorker) {
                final Set<Worker> previousBiggest = collusion.getBiggest();
                collusion.split(setWorker, worker);
                /* Readapt if needed */
                collusion.readapt(previousBiggest);
                /* Update the interaction structure */
                adaptInteractionStructure(job, setWorker, setOtherWorker,
                    collusion.getSet(worker), collusion.getSet(otherWorker));
            }
        }
        for (Worker otherWorker : workers) {
            final Set<Worker> setOtherWorker = collusion.getSet(otherWorker);
            if (updateInteraction(job, worker, otherWorker,
                        setWorker, setOtherWorker)) {
                collusion.decreaseCollusion(worker, otherWorker);
                adaptInteractionStructure(job, setWorker, setOtherWorker,
                        collusion.getSet(worker), collusion.getSet(otherWorker));
            }
        }
	}

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct results. Workers does not contain the winning set.
	 */
	protected void setDistinctSets(Job job, Set<Worker> winningWorkers,
			Set<Set<Worker>> workers) {
        assert (!winningWorkers.isEmpty()) : "No winning workers";
        /* Non-colluders */
        /* Begin with diagonal */
        for (Worker worker : winningWorkers) {
            final Set<Worker> setWorker = collusion.getSet(worker);
            if (updateInteraction(job, worker, worker, setWorker, setWorker))
                collusion.decreaseCollusion(worker, worker);
        }
        for (Worker worker : winningWorkers)
            for (Worker otherWorker : winningWorkers) {
                final Set<Worker> setWorker = collusion.getSet(worker);
                final Set<Worker> setOtherWorker = collusion.getSet(otherWorker);
                if (updateInteraction(job, worker, otherWorker,
                            setWorker, setOtherWorker)) {
                    collusion.decreaseCollusion(worker, otherWorker);
                    adaptInteractionStructure(job, setWorker, setOtherWorker,
                        collusion.getSet(worker), collusion.getSet(otherWorker));
                }
            }
        /* Observed collusion */
        for (Set<Worker> workersSet : workers) {
            /* Begin with diagonal */
            for (Worker worker : workersSet) {
                final Set<Worker> setWorker = collusion.getSet(worker);
                if (updateInteraction(job, worker, worker, setWorker, setWorker))
                    collusion.increaseCollusion(worker, worker);
            }
            for (Worker worker : workersSet)
                for (Worker otherWorker : workersSet) {
                    final Set<Worker> setWorker = collusion.getSet(worker);
                    final Set<Worker> setOtherWorker = collusion.getSet(otherWorker);
                    if (updateInteraction(job, worker, otherWorker,
                            setWorker, setOtherWorker)) {
                        collusion.increaseCollusion(worker, otherWorker);
                        adaptInteractionStructure(job, setWorker, setOtherWorker,
                                collusion.getSet(worker), collusion.getSet(otherWorker));
                    }
                }
        }
		updatedSets.remove(job);
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result.
	 */
	public RV getCollusionLikelihood(Set<? extends Worker> workers) {
		final RV[][] proba = collusion.getCollusions(workers);
		RV estimator = new Beta(1.0d);
		for (int i = 0; i < proba.length; i++)
			for (int j = i; j < proba[i].length; j++)
				estimator = min(estimator, proba[i][j]);
		assert (estimator.getMean() >= 0.0d) : "Negative estimate: "
				+ estimator.getMean();

		logger.finest("Estimated collusion likelihood of " + workers.size()
				+ " workers is " + estimator);
		return estimator;
	}

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public <W extends Worker> Map<W, RV> getCollusionLikelihood(W worker,
			Set<W> workers) {
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
        final int countNonColluder = collusion.countNonColluder();
		final double fraction = 1.0d - (double)countNonColluder / workers.size();
		final RV result = new RV(0.0d, 1.0d) {
            double error = collusion.getGeneralError();
            public RV clone() { return this; }
            public double getMean() { return fraction; }
            protected double getVariance() { return 0.0d; }
            public double getError() { return error; }
        };
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

    public String toString() {
        return "collusion-based reputation system";
    }

    protected void print() {
        collusion.print();
    }    

}
