package simdeg.reputation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import simdeg.util.Beta;
import simdeg.util.RV;

/**
 * Optimistic strategy considering no collusion and no failure
 */
public class OptimisticGridCharacteristics<W extends Worker> implements
		ReputationSystem<W> {

	/** Set of workers we are manipulating */
	protected Set<W> workers = new HashSet<W>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends W> workers) {
		this.workers.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends W> workers) {
		this.workers.removeAll(workers);
	}

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public void setWorkerResult(W worker, Job job, Result result) {
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
	}

	/**
	 * Returns a perfect reliability for each worker.
	 */
	public RV getReliability(W worker) {
		return new Beta(1.0d);
	}

	/**
	 * Returns a zero likelihood that any given group of workers give the same
	 * wrong result.
	 */
	public RV getCollusionLikelihood(Set<W> workers) {
		return new Beta(0.0d);
	}

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public Map<W, RV> getCollusionLikelihood(W worker, Set<W> workers) {
		Map<W, RV> result = new HashMap<W, RV>();
		for (W otherWorker : workers)
			result.put(otherWorker, new Beta(0.0d));
		return result;
	}

	/**
	 * Returns an optimistic fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public RV getColludersFraction() {
		return new Beta(0.0d);
	}

	public Set<? extends Set<W>> getGroups(Collection<W> workers) {
		final Set<Set<W>> result = new HashSet<Set<W>>();
		result.add(new HashSet<W>(workers));
		return result;
	}
	
	public Set<W> getLargestGroup() {
		return new HashSet<W>();
	}

}
