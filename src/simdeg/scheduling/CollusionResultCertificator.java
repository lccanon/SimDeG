package simdeg.scheduling;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Tries to select the best result based on the estimations of the probabilities
 * of collusion for each worker. A group of workers, called a result group,
 * corresponds to each distinct returned result. The selected result is the one
 * whose probability to be correct is the highest.
 */
public class CollusionResultCertificator extends ResultCertificator {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(CollusionResultCertificator.class.getName());

	private static final double ZERO_PROBA = 0.001d;
	
	private static final int MAX_UNION = 5;

	private static final double CORRECTNESS_PROBA = 0.95d;

	private static final double MAX_STANDARD_DEVIATION = 0.1d;

	@Override
	<R extends Result> R certifyResult(VotingPool<R> votingPool) {
		assert (!votingPool.isEmpty()) : "No job given to the result certificator";
		assert (votingPool.isComplete()) : "Job still processing";

		/* Compute correctness probabilities */
		final Map<R, RV> correctProba = correctProbability(votingPool);
		/* Find the best result */
		final R best = selectBestResult(correctProba);

		/*
		 * If there is one, either the duplication is reached or the correctness
		 * probability estimation is high.
		 */
		if (best != null) {
			final RV rv = correctProba.get(best);
			if (Math.sqrt(rv.getVariance()) < MAX_STANDARD_DEVIATION
					&& rv.getMean() - Math.sqrt(rv.getVariance()) > CORRECTNESS_PROBA) {
				logger.fine("Result " + best + " is certified (among "
						+ correctProba.size() + " results and "
						+ votingPool.size() + " workers) with an estimated "
						+ "correctness probability of " + rv);
				return best;
			}
		}

		/* In any other case */
		logger.fine("No result is certified (among " + correctProba.size()
				+ " results and " + votingPool.size() + " workers)");
		return null;
	}

	private <R extends Result> R selectBestResult(Map<R, RV> correctProba) {
		R max = null;
		RV maxRV = null;
		for (R result : correctProba.keySet()) {
			final RV rv = correctProba.get(result);
			if (max == null || maxRV.getMean() < rv.getMean()) {
				max = result;
				maxRV = rv;
			}
		}
		return max;
	}

	/**
	 * Computes the probabilities that each result is correct given the returned
	 * results.
	 */
	protected <R extends Result> Map<R, RV> correctProbability(
			VotingPool<R> votingPool) {
		/*
		 * Get workers into a convenient structure by separating them by their
		 * results.
		 */
		final Map<R, Set<Worker>> map = votingPool.getJobsByResult();

		/* Select the result that is part of the observed non-colluding group */
		final Set<Worker> largest = reputationSystem.getLargestGroup();
		for (R result : map.keySet()) {
			if (map.get(result).size() == 1)
				continue;
			final Collection<? extends Set<Worker>> groups = reputationSystem
					.getGroups(map.get(result));
			for (Set<Worker> group : groups)
				if (group == largest) {
					logger.finer("One of the result is computed by"
							+ " a worker in the observed non-colluding group");
					final Map<R, RV> correctProba = new HashMap<R, RV>();
					correctProba.put(result, new RV(reputationSystem
							.getCollusionLikelihood(group).add(-1.0d).multiply(
									-1.0d)));
					return correctProba;
				}
		}

		/*
		 * Select the results that were received at least twice.
		 */
		final Set<R> results = new HashSet<R>();
		final Set<Set<Worker>> resultGroups = new HashSet<Set<Worker>>();
		for (R result : map.keySet())
			if (map.get(result).size() >= 2) {
				results.add(result);
				resultGroups.add(map.get(result));
			}

		/*
		 * Compute the probabilities that the workers of some result groups are
		 * all colluding (but not colluding with any other colluder).
		 */
		final RV allColluding = new DistinctCollusionsEvent(resultGroups,
				resultGroups).inter();
		final Map<R, RV> allOtherColluding = new HashMap<R, RV>();
		for (R result : results) {
			final Set<Set<Worker>> allOthers = new HashSet<Set<Worker>>(
					resultGroups);
			allOthers.remove(map.get(result));
			allOtherColluding.put(result, new DistinctCollusionsEvent(
					allOthers, resultGroups).inter());
		}

		/*
		 * Compute the probability that the current result groups are obtained.
		 */
		RV config = allColluding;
		for (R result : results)
			config = config.add(allOtherColluding.get(result)).subtract(
					allColluding);
		logger.finer("The probability that this voting pool occurs is "
				+ config);

		/*
		 * Aggregate the intermediate computation for the final results.
		 */
		final Map<R, RV> correctProba = new HashMap<R, RV>();
		for (R result : results) {
			final RV proba = allOtherColluding.get(result).subtract(
					allColluding);
			logger.finer("The probability that result " + result
					+ " is correct and that the voting" + " pool occurs is "
					+ proba);
			correctProba.put(result, proba.divide(config.getMean()));
		}

		return correctProba;
	}

	/**
	 * Represents the event that several independent collusion occurs.
	 */
	private class DistinctCollusionsEvent extends
			HashSet<DistinctCollusionEvent> {

		private static final long serialVersionUID = 1L;

		private DistinctCollusionsEvent(
				Collection<DistinctCollusionEvent> events) {
			addAll(events);
		}

		/**
		 * Builds the event given that the set of groups that collude
		 * independently of the groups in the second set.
		 */
		private DistinctCollusionsEvent(
				Collection<? extends Collection<Worker>> Q,
				Collection<? extends Collection<Worker>> P) {
			for (Collection<Worker> q : Q) {
				/* Put all relevant workers in the same set */
				final Set<Worker> workers = new HashSet<Worker>();
				for (Collection<Worker> p : P) {
					if (p == q)
						continue;
					workers.addAll(p);
				}
				/* Get the corresponding estimated groups of workers */
				final Collection<? extends Collection<Worker>> groups = reputationSystem
						.getGroups(workers);
				assert (groups.size() <= workers.size());
				logger.finest("Add a collusion event involving at least "
						+ groups.size() + " observed groups");
				/* Build each union event */
				final Set<CollusionEvent> events = new HashSet<CollusionEvent>();
				for (Collection<Worker> p : groups) {
					final CollusionEvent collusionEvent = new CollusionEvent(q);
					collusionEvent.add(p.iterator().next());
					events.add(collusionEvent);
				}
				/* Build the resulting event that a distinct collusion occurs */
				final DistinctCollusionEvent distinctCollusionEvent = new DistinctCollusionEvent(
						new CollusionEvent(q), events);
				assert (distinctCollusionEvent.size() == groups.size());
				add(distinctCollusionEvent);
			}
			assert (size() == Q.size());
		}

		/**
		 * Computes the probability of the intersection of the distinct
		 * collusion events.
		 */
		private RV inter() {
			if (isEmpty())
				return new RV(1.0d, 0.0d);
			final DistinctCollusionEvent head = iterator().next();
			final RV event = new RV(reputationSystem
					.getCollusionLikelihood(head.current));
			final RV union = head.union();
			final RV firstRV = event.subtract(union);
			final DistinctCollusionsEvent tail = new DistinctCollusionsEvent(
					this);
			tail.remove(head);
			return firstRV.mult(tail.inter());
		}

	}

	/**
	 * Represents the probability that a specific collusion occurs independently
	 * of the other result groups.
	 */
	private class DistinctCollusionEvent extends CollusionsUnionEvent {

		private static final long serialVersionUID = 1L;

		/**
		 * The current collusion event that occurs.
		 */
		private final CollusionEvent current;

		private DistinctCollusionEvent(CollusionEvent event,
				Collection<CollusionEvent> events) {
			this.current = event;
			addAll(events);
		}

		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof DistinctCollusionEvent))
				return false;
			final DistinctCollusionEvent event = (DistinctCollusionEvent) obj;
			return event.current == current;
		}

	}

	/**
	 * Represents the probability that a union of several collusions occurs.
	 */
	private class CollusionsUnionEvent extends HashSet<CollusionEvent> {

		private static final long serialVersionUID = 1L;

		private CollusionsUnionEvent() {
		}

		private CollusionsUnionEvent(Collection<CollusionEvent> events) {
			addAll(events);
		}

		/**
		 * Computes the probability that the union of the related collusion
		 * events happens.
		 */
		protected RV union() {
			if (isEmpty())
				return new RV(0.0d, 0.0d);
			logger.finest("Union of " + size() + " events");
			final CollusionEvent head = iterator().next();
			final RV firstRV = new RV(reputationSystem
					.getCollusionLikelihood(head));
			final CollusionsUnionEvent tail = new CollusionsUnionEvent(this);
			tail.remove(head);
			if (firstRV.getMean() + Math.sqrt(firstRV.getVariance()) < ZERO_PROBA)
				return tail.union();
			if (tail.size() > MAX_UNION)
				return firstRV.add(tail.union());
			return firstRV.add(tail.union()).subtract(tail.inter(head));
		}

		/**
		 * Computes the intersection of a union and a given event.
		 */
		private RV inter(CollusionEvent e) {
			final CollusionsUnionEvent result = new CollusionsUnionEvent();
			for (CollusionEvent event : this) {
				final CollusionEvent elem = new CollusionEvent(event);
				elem.addAll(e);
				result.add(elem);
			}
			return result.union();
		}

	}

	/**
	 * Represents the event that specifies that some workers collude all
	 * together.
	 */
	private class CollusionEvent extends HashSet<Worker> {

		private static final long serialVersionUID = 1L;

		private CollusionEvent(Collection<Worker> workers) {
			addAll(workers);
		}

	}

	protected class RV extends simdeg.util.RV {

		private final double mean;

		private final double var;

		protected RV(double mean, double var) {
			super(0.0d, 1.0d);
			this.mean = mean;
			this.var = var;
		}

		private RV(simdeg.util.RV rv) {
			this(rv.getMean(), rv.getVariance());
		}

		private RV add(RV rv) {
			return new RV(mean + rv.mean, var + rv.var);
		}

		private RV subtract(RV rv) {
			return new RV(mean - rv.mean, var + rv.var);
		}

		private RV mult(RV rv) {
			return new RV(mean * rv.mean, mean * mean * rv.var + rv.mean
					* rv.mean * var + var * rv.var);
		}

		private RV divide(double d) {
			if (d == 0.0d)
				return new RV(0.0d, var);
			return new RV(mean / d, var / d / d);
		}

		@Override
		public simdeg.util.RV clone() {
			return new RV(mean, var);
		}

		@Override
		public double getError() {
			return 0.0d;
		}

		@Override
		public double getMean() {
			return mean;
		}

		@Override
		public double getVariance() {
			return var;
		}

		public String toString() {
			return "<" + mean + "," + Math.sqrt(var) + ">";
		}

	}

}