package simdeg.scheduling;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Quorum-based implementation of the result selection process which select the
 * result that reaches a quorum.
 */
public class BOINCResultCertificator extends ResultCertificator {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(BOINCResultCertificator.class.getName());

	private int quorum;
	private int maxDuplication;
	
	public BOINCResultCertificator() {
		this(3, 10);
	}

	public BOINCResultCertificator(int quorum, int maxDuplication) {
		this.quorum = quorum;
		this.maxDuplication = maxDuplication;
	}
	
	/**
	 * Returns the best result according to the quorum policy.
	 */
	<R extends Result> R certifyResult(VotingPool<R> votingPool) {
		assert (!votingPool.isEmpty()) : "No job given to the result certificator";
		assert (votingPool.isComplete()) : "Job still processing";

		/*
		 * Get workers into a convenient structure by separating them by their
		 * results
		 */
		Map<R, Set<Worker>> map = votingPool.getJobsByResult();

		/* Find the job that have the majority */
		R majorityResult = null;
		for (R result : map.keySet())
			if (majorityResult == null
					|| map.get(result).size() > map.get(majorityResult).size())
				majorityResult = result;

		/* Test if the quorum or the maximum duplication parameter is achieved */
		if (map.get(majorityResult).size() < quorum
				&& votingPool.size() < maxDuplication) {
			logger.fine("No result has reached the quorum (the current "
					+ "best is " + majorityResult + " with a majority of "
					+ map.get(majorityResult).size() + " workers)");
			return null;
		}
		logger.fine("Result " + majorityResult
				+ " is certified with a majority of "
				+ map.get(majorityResult).size() + " workers (>" + quorum
				+ ") over " + votingPool.size() + " results");
		return majorityResult;
	}

	public String toString() {
		return "BOINCResultCertificator(" + quorum + ", " + maxDuplication
				+ ")";
	}

}