package simdeg.reputation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import simdeg.util.BTS;
import simdeg.util.Collections;
import simdeg.util.Estimator;

/**
 * Skeleton easing development of reputation systems.
 */
public abstract class SkeletonReputationSystem extends ReliableReputationSystem implements ReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(SkeletonReputationSystem.class.getName());

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public void setWorkerResult(Worker worker, Job job, Result result) {
        super.setWorkerResult(worker, job, result);

		/* Update collusion when this is the second result for this job */
        final Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (workersByResult.get(result).size() == 2) {
			/* Find first worker of the set if it was alone */
			Worker firstWorker = null;
			for (Worker otherWorker : workersByResult.get(result))
				if (otherWorker != worker)
					firstWorker = otherWorker;

			/* Update its disagreements */
			for (Map.Entry<Result, Set<Worker>> entry : workersByResult
					.entrySet())
				if (entry.getValue().size() > 1
						&& !result.equals(entry.getKey())) {
					setDisagreement(job, firstWorker, entry.getValue());
					logger.finer("Worker " + firstWorker + " disagrees with "
							+ entry.getValue());
				}
		}

		/* Update collusion when there is already more than 2 jobs */
		if (workersByResult.get(result).size() >= 2) {
			/* Update agreements */
			Set<Worker> workers = new HashSet<Worker>(workersByResult
					.get(result));
			workers.remove(worker);
			setAgreement(job, worker, workers);
			logger.finer("Worker " + worker + " agrees with " + workers);

			/* Update disagreements */
			for (Map.Entry<Result, Set<Worker>> entry : workersByResult
					.entrySet())
				if (entry.getValue().size() > 1
						&& !result.equals(entry.getKey())) {
					setDisagreement(job, worker, entry.getValue());
					logger.finer("Worker " + worker + " disagrees with "
							+ entry.getValue());
				}
		}
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certfified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
        super.setCertifiedResult(job, result);

		Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			throw new NoSuchElementException(
					"Job never met before by the reputation system");

		/* Inform in the grid characteristics which groups give separate answers */
		Set<Set<Worker>> sets = new HashSet<Set<Worker>>();
		Set<Worker> winningSet = new HashSet<Worker>();
		for (Result otherResult : workersByResult.keySet()) {
			if (workersByResult.get(otherResult).size() != 1
					&& !result.equals(otherResult))
				sets.add(workersByResult.get(otherResult));
			if (result.equals(otherResult))
				winningSet = workersByResult.get(otherResult);
		}
		setDistinctSets(job, winningSet, sets);
	}

	/**
	 * Specifies that a worker agrees with a set of workers. The specified
	 * worker can also be inside the set.
	 */
	protected abstract void setAgreement(Job job, Worker worker,
			Set<Worker> workers);

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected abstract void setDisagreement(Job job, Worker worker,
			Set<Worker> workers);

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct answers. Workers does not contain the winning set.
	 */
	protected abstract void setDistinctSets(Job job,
			Set<Worker> winningWorkers, Set<Set<Worker>> workers);

}
