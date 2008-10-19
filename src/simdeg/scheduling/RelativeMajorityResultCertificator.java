package simdeg.scheduling;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Job;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Relative majority-based implementation of the result selection process which
 * select the result having relative majority (at least two).
 */
public class RelativeMajorityResultCertificator extends ResultCertificator {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(RelativeMajorityResultCertificator.class.getName());

	/**
	 * Returns the best result according to the majority.
	 */
	<J extends Job<R>, R extends Result> R selectBestResult(
			List<Worker> workers, List<R> results)
			throws ResultCertificationException {
		assert (!results.isEmpty()) : "No job given to the result certificator";
		if (results.size() == 1)
			throw new ResultCertificationException();
		Map<R, Set<Worker>> map = ResultCertificator.getJobsByResult(workers,
				results);
		R majorityResult = null;
		for (R result : results)
			if (majorityResult == null
					|| map.get(result).size() > map.get(majorityResult).size())
				majorityResult = result;
		if (map.get(majorityResult).size() == 1)
			throw new ResultCertificationException();
		logger.fine("Result " + majorityResult
				+ " is selected with a relative majority of "
				+ map.get(majorityResult).size() + " workers");
		return majorityResult;
	}

	/**
	 * Returns an result in every cases which is the less worse.
	 */
	<J extends Job<R>, R extends Result> R selectLessWorseResult(
			List<Worker> workers, List<R> results) {
		try {
			return selectBestResult(workers, results);
		} catch (ResultCertificationException e) {
		}
		ResultCertificator resultCertificator = new RandomResultCertificator();
		return resultCertificator.selectLessWorseResult(workers, results);
	}

}