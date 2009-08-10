package simdeg.scheduling;

import java.util.List;

import simdeg.reputation.Job;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;
import simdeg.util.Collections;
import simdeg.util.RandomManager;

/**
 * Basic implementation of the result selection process which select one result
 * randomly.
 */
public class RandomResultCertificator extends ResultCertificator {

	/**
	 * Returns the a random result.
	 */
	<J extends Job, R extends Result> R selectBestResult(
			List<Worker> workers, List<R> results) {
		assert (!results.isEmpty()) : "No job given to the result certificator";
		final List<R> result = Collections.getRandomSubGroup(1, results,
				RandomManager.getRandom("scheduling"));
		return result.get(0);
	}

	/**
	 * Returns an result in every cases which is the less worse.
	 */
	<J extends Job, R extends Result> R selectLessWorseResult(
			List<Worker> workers, List<R> results) {
		return selectBestResult(workers, results);
	}

}
