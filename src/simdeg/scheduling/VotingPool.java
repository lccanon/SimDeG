package simdeg.scheduling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Job;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Correspondence between the workers and their results (null if still
 * processing).
 */
public class VotingPool<R extends Result> extends HashMap<Worker, R> {

	private static final long serialVersionUID = 1L;

	/** Logger */
	private static final Logger logger = Logger.getLogger(VotingPool.class
			.getName());

	/** Job that is concerned by this voting pool */
	private final Job job;

	protected VotingPool(Job job) {
		this.job = job;
	}

	public Job getJob() {
		return job;
	}

	protected boolean isComplete() {
		for (R result : values())
			if (result == null)
				return false;
		return true;
	}

	/**
	 * Utility function to sort the jobs by their respective result.
	 */
	protected Map<R, Set<Worker>> getJobsByResult() {
		assert (isComplete()) : "Some results have not yet arrived";

		/* Initialization of the structure */
		Map<R, Set<Worker>> map = new HashMap<R, Set<Worker>>();
		for (R result : values())
			map.put(result, new HashSet<Worker>());

		/* Regroup the workers */
		for (Worker worker : keySet())
			map.get(get(worker)).add(worker);
		logger.fine("We have " + map.size() + " distinct results in groups "
				+ map.values());
		return map;
	}

}