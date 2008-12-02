package simdeg.reputation;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import simdeg.util.BTS;
import simdeg.util.Estimator;

/**
 * Optimistic strategy considering no collusion and no failure
 */
public class OptimisticGridCharacteristics implements ReputationSystem {

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
	public void setWorkerResult(Worker worker, Job job, Result result) {
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
     * certfified result associated to it.
     */
    public void setCertifiedResult(Job job, Result result) {
    }
    
	
    /**
     * Returns a perfect reliability for each worker.
     */
    public Estimator getReliability(Worker worker) {
        return new BTS(1.0d);
    }

    /**
     * Returns a zero likelihood that any given group of workers give the
     * same wrong result.
     */
    public Estimator getCollusionLikelihood(Set<? extends Worker> workers) {
        return new BTS(0.0d);
    }

    /**
     * Returns the estimated likelihoods that a worker will return the same
     * wrong result than each other.
     */
    public <W extends Worker> Map<W,Estimator> getCollusionLikelihood(W worker,
            Set<W> workers) {
        Map<W,Estimator> result = new HashMap<W,Estimator>();
        for (W otherWorker : workers)
            result.put(otherWorker, new BTS(0.0d));
        return result;
    }

    /**
     * Returns an optimistic fraction of colluders (workers returning together
     * the same wrong result).
     */
    public Estimator getColludersFraction() {
        return new BTS(0.0d);
    }

}
