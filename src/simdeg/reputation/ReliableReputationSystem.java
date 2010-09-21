package simdeg.reputation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.BetaEstimator;
import simdeg.util.Estimator;
import simdeg.util.RV;

/**
 * Strategy considering only failures.
 */
public class ReliableReputationSystem<W extends Worker> implements BasicReputationSystem<W> {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(ReliableReputationSystem.class.getName());

	/** Workers sorted by result for each job (for collusion) */
    protected Map<Job, Map<Result, Set<W>>> workersByResults
        = new HashMap<Job, Map<Result, Set<W>>>();

    /** Estimates of the reliability */
    private Map<W, Estimator> reliability = new HashMap<W, Estimator>();
    
    /** Set of workers we are manipulating */
    protected Set<W> workers = new HashSet<W>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends W> workers) {
        this.workers.addAll(workers);
		for (W worker : workers)
			reliability.put(worker, new BetaEstimator());
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends W> workers) {
        this.workers.removeAll(workers);
		for (W worker : workers)
			reliability.remove(worker);
	}

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
    public void setWorkerResult(W worker, Job job, Result result) {
		/* Update data structures related to collusion and fault */
		if (!workersByResults.containsKey(job))
			workersByResults.put(job, new HashMap<Result, Set<W>>());
        final Map<Result, Set<W>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			workersByResult.put(result, new HashSet<W>());
		workersByResult.get(result).add(worker);

		/* Update successful worker(s) */
		if (workersByResult.get(result).size() == 2) {
            for (W successfulWorker : workersByResult.get(result))
                if (this.workers.contains(successfulWorker))
                    reliability.get(successfulWorker).setSample(1.0d);
        } else if (workersByResult.get(result).size() > 2)
            if (this.workers.contains(worker))
                reliability.get(worker).setSample(1.0d);
	}

	/**
	 * Gives to the reputation system a pair containing a job and the
	 * certified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
		Map<Result, Set<W>> workersByResult = workersByResults.get(job);

		/*
		 * Remove every colluding group of results and consider the rest as
		 * failures
		 */
		for (Result otherResult : workersByResult.keySet())
			if (workersByResult.get(otherResult).size() == 1
					&& otherResult != result)
                for (W worker : workersByResult.get(otherResult))
                    if (this.workers.contains(worker))
                        reliability.get(worker).setSample(0.0d);

        /* Clean structure */
        workersByResults.remove(job);
	}

	/**
	 * Returns the estimated reliability of the worker.
	 */
	public RV getReliability(W worker) {
		if (!reliability.containsKey(worker))
			throw new NoSuchElementException("Inexistant worker");
		logger.finer("Reliability of worker " + worker + " is "
				+ reliability.get(worker));
		return reliability.get(worker);
	}

    public String toString() {
        return "Reliability-based reputation system:\n"
            + Arrays.toString(workers.toArray()) + " = "
            + Arrays.toString(reliability.values().toArray()) + '\n';
    }

}
