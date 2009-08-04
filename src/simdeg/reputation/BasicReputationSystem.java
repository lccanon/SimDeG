package simdeg.reputation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import simdeg.util.RV;

/**
 * Stores and handles all observations relative to the Desktop grids on which we
 * are running. Is only concerned with added workers and should manage old
 * workers.
 */
public interface BasicReputationSystem {

	/**
	 * Gives participating workers.
	 */
	public abstract void addAllWorkers(Set<? extends Worker> workers);

	/**
	 * Remove participating workers.
	 */
	public abstract void removeAllWorkers(Set<? extends Worker> workers);

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
	public abstract RV getReliability(Worker worker);

}
