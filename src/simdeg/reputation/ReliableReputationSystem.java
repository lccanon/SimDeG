package simdeg.reputation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.BTS;
import simdeg.util.Collections;
import simdeg.util.Estimator;

/**
 * Strategy considering only failures.
 */
public abstract class ReliableReputationSystem extends ReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(ReliableReputationSystem.class.getName());

	/** Workers sorted by result for each job (for collusion) */
	private Map<Job, Map<Result, Set<Worker>>> workersByResults = new HashMap<Job, Map<Result, Set<Worker>>>();

	/** Estimates of the reliability */
	private Map<Worker, Estimator> reliability = new HashMap<Worker, Estimator>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		for (Worker worker : workers) {
			reliability.put(worker, new BTS(1.0d));
		}
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		for (Worker worker : workers)
			reliability.remove(worker);
	}

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public void setWorkerResult(Worker worker, Job job, Result result) {
		/* Update data structures related to collusion and fault */
		if (!workersByResults.containsKey(job))
			workersByResults.put(job, new HashMap<Result, Set<Worker>>());
		Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			workersByResult.put(result, new HashSet<Worker>());
		workersByResult.get(result).add(worker);

		/* Update collusion when this is the second result for this job */
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

		/* Update successful worker(s) */
		if (workersByResult.get(result).size() == 2)
			setSuccess(workersByResult.get(result));
		else if (workersByResult.get(result).size() > 2)
			setSuccess(Collections.addElement(worker, new HashSet<Worker>()));
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certfified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
		Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			throw new IllegalArgumentException(
					"Job never met before by the reputation system");

		/*
		 * Remove every colluding group of answers and consider the rest as
		 * failures
		 */
		Set<Worker> failedWorkers = new HashSet<Worker>();
		for (Result otherResult : workersByResult.keySet())
			if (workersByResult.get(otherResult).size() == 1
					&& otherResult != result)
				failedWorkers.addAll(workersByResult.get(otherResult));
		setFailure(failedWorkers);

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
	 * Specifies that some workers were found to give singleton result.
	 */
	protected void setFailure(Set<Worker> workers) {
		for (Worker worker : workers)
			if (this.workers.contains(worker))
				reliability.get(worker).setSample(0.0d);
	}

	/**
	 * Specifies that some workers give same kind of result, hence success.
	 */
	protected void setSuccess(Set<Worker> workers) {
		for (Worker worker : workers)
			if (this.workers.contains(worker))
				reliability.get(worker).setSample(1.0d);
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

	/**
	 * Returns the estimated reliability of the worker.
	 */
	public Estimator getReliability(Worker worker) {
		if (!reliability.containsKey(worker))
			throw new IllegalArgumentException("Inexistant worker");
		logger.finer("Reliability of worker " + worker + " is "
				+ reliability.get(worker));
		return reliability.get(worker);
	}

}