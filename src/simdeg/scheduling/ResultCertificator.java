package simdeg.scheduling;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Class allowing to select one result from a set of answers.
 */
public abstract class ResultCertificator {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(ResultCertificator.class.getName());

	ReputationSystem reputationSystem = null;

	/**
	 * Specifies the object characterizing the information we have on the grid.
	 */
	void setReputationSystem(ReputationSystem reputationSystem) {
		this.reputationSystem = reputationSystem;
	}

	/**
	 * Returns the best result according to some defined policy and grid
	 * characteristics.
	 */
	abstract <J extends Job<R>, R extends Result> R selectBestResult(
			List<Worker> workers, List<R> results)
			throws ResultCertificationException, JobPostponedException;

	/**
	 * Returns an result in every cases which is the less worse.
	 */
	abstract <J extends Job<R>, R extends Result> R selectLessWorseResult(
			List<Worker> workers, List<R> results);

	/**
	 * Utility function to sort the jobs by their respective result.
	 */
	protected static <J extends Job<R>, R extends Result> Map<R, Set<Worker>> getJobsByResult(
			List<Worker> workers, List<R> results) {
		assert (workers.size() == results.size()) : "Both lists correspond to similar computations and should have identical size";
		Map<R, Set<Worker>> map = new HashMap<R, Set<Worker>>();
		for (R result : results)
			map.put(result, new HashSet<Worker>());
		for (int i = 0; i < workers.size(); i++)
			map.get(results.get(i)).add(workers.get(i));
		logger.fine("We have " + map.size() + " distinct answers in groups "
				+ map.values());
		return map;
	}
}