package simdeg.scheduling;

import java.lang.Double;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Job;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;
import simdeg.util.Estimator;

/**
 * Collusion aware implementation of the result selection process which select
 * the best result.
 */
public class CollusionAwareResultCertificator extends ResultCertificator {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(CollusionAwareResultCertificator.class.getName());

    /** Maximal error allowed when manipulating an estimator */
	private static final double MAX_ERROR = 1.0d / 3.0d;

    /** Maximal measured collusion of a group before certifying a result */
	private static final double COLLUSION_THRESHOLD = 0.01d;

	/**
	 * Returns the best result according to the minimal likelihood of collusion
	 * between workers.
	 */
	<J extends Job<R>, R extends Result> R selectBestResult(
			List<Worker> workers, List<R> results)
			throws ResultCertificationException {
		assert (!results.isEmpty()) : "No job given to the result certificator";
		/* Need at least two workers (for majority) and some info */
		if (results.size() == 1)
			throw new ResultCertificationException();

		/* Find the best result according to its collusion likelihood */
		Map<R, Set<Worker>> map = ResultCertificator.getJobsByResult(workers,
				results);
		double bestRank = 0.0d;
		R bestResult = null;
		/* Store the likelihoods to avoid recomputation */
		Map<Set<Worker>, Double> colludingLikelihood = new HashMap<Set<Worker>, Double>();
		for (Set<Worker> group : map.values()) {
			Estimator estimator = reputationSystem
					.getCollusionLikelihood(group);
			if (estimator.getError() > MAX_ERROR)
				throw new ResultCertificationException();
			colludingLikelihood.put(group, estimator.getEstimate());
		}
		for (Map.Entry<R, Set<Worker>> entry : map.entrySet()) {
			/* We don't consider singleton group */
			if (entry.getValue().size() == 1)
				continue;
			double rank = 1.0d;
			/*
			 * Likelihood that this group is not colluding while the others are
			 */
			for (Map.Entry<R, Set<Worker>> otherEntry : map.entrySet()) {
				/* We don't consider singleton group */
				if (otherEntry.getValue().size() == 1)
					continue;
				if (otherEntry == entry)
					rank *= (1.0d - colludingLikelihood.get(otherEntry
							.getValue()));
				else
					rank *= colludingLikelihood.get(otherEntry.getValue());
			}
			if (rank > bestRank) {
				bestResult = entry.getKey();
				bestRank = rank;
			}
			logger.finer("Rank for result " + entry.getKey() + " is " + rank);
		}

		if (bestResult == null
				|| colludingLikelihood.get(map.get(bestResult)) > COLLUSION_THRESHOLD)
			throw new ResultCertificationException();

		logger.fine("The best rank is " + bestRank + " for a group of "
				+ map.get(bestResult).size() + " workers on a total of "
				+ workers.size() + " executed jobs and corresponds to result "
				+ bestResult);
		return bestResult;
	}

	/**
	 * Returns an result in every cases which is the less worse (based on strict
	 * majority).
	 */
	<J extends Job<R>, R extends Result> R selectLessWorseResult(
			List<Worker> workers, List<R> results) {
		try {
			return selectBestResult(workers, results);
		} catch (ResultCertificationException e) {
		}
		ResultCertificator resultCertificator = new AbsoluteMajorityResultCertificator();
		return resultCertificator.selectLessWorseResult(workers, results);
	}

}
