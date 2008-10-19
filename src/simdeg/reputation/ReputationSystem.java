package simdeg.reputation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import simdeg.util.Estimator;

/**
 * Stores and handles all observations relative to the Desktop grids on which we
 * are running. Is only concerned with added workers and should manage old
 * workers.
 */
public abstract class ReputationSystem {

	/** Set of workers we are manipulating */
	protected Set<Worker> workers = new HashSet<Worker>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		this.workers.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		this.workers.removeAll(workers);
	}

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public abstract void setWorkerResult(Worker worker, Job job, Result result);

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certfified result associated to it.
	 */
	public abstract void setCertifiedResult(Job job, Result result);

	/**
	 * Returns the estimated reliability of the worker.
	 */
	public abstract Estimator getReliability(Worker worker);

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same wrong result.
	 */
	public abstract Estimator getCollusionLikelihood(Set<Worker> workers);

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public abstract Map<Worker, Estimator> getCollusionLikelihood(
			Worker worker, Set<Worker> workers);

	/**
	 * Returns the estimated fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public abstract Estimator getColludersFraction();

}