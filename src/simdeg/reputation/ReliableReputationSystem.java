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
 * Strategy considering only failures.
 */
public class ReliableReputationSystem implements BasicReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(ReliableReputationSystem.class.getName());

	/** Workers sorted by result for each job (for collusion) */
    protected Map<Job, Map<Result, Set<Worker>>> workersByResults = new HashMap<Job, Map<Result, Set<Worker>>>();

	/** Estimates of the reliability */
	private Map<Worker, Estimator> reliability = new HashMap<Worker, Estimator>();
    
    /** Set of workers we are manipulating */
    protected Set<Worker> workers = new HashSet<Worker>();

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
        this.workers.addAll(workers);
		for (Worker worker : workers)
			reliability.put(worker, new BTS(1.0d));
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
        this.workers.removeAll(workers);
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
        final Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			workersByResult.put(result, new HashSet<Worker>());
		workersByResult.get(result).add(worker);

		/* Update successful worker(s) */
		if (workersByResult.get(result).size() == 2)
            for (Worker successfulWorker : workersByResult.get(result))
                if (this.workers.contains(successfulWorker))
                    reliability.get(successfulWorker).setSample(1.0d);
		else if (workersByResult.get(result).size() > 2)
            if (this.workers.contains(worker))
                reliability.get(worker).setSample(1.0d);
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certfified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
		Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (!workersByResult.containsKey(result))
			throw new NoSuchElementException(
					"Job never met before by the reputation system");

		/*
		 * Remove every colluding group of answers and consider the rest as
		 * failures
		 */
		for (Result otherResult : workersByResult.keySet())
			if (workersByResult.get(otherResult).size() == 1
					&& otherResult != result)
                for (Worker worker : workersByResult.get(otherResult))
                    if (this.workers.contains(worker))
                        reliability.get(worker).setSample(0.0d);
	}

	/**
	 * Returns the estimated reliability of the worker.
	 */
	public Estimator getReliability(Worker worker) {
		if (!reliability.containsKey(worker))
			throw new NoSuchElementException("Inexistant worker");
		logger.finer("Reliability of worker " + worker + " is "
				+ reliability.get(worker));
		return reliability.get(worker);
	}

}
