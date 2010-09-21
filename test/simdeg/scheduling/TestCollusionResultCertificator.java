package simdeg.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;
import simdeg.scheduling.CollusionResultCertificator.RV;
import simdeg.util.HashableObject;

public class TestCollusionResultCertificator {

	private final static double EPSILON = 1E-6d;

	private static Worker[] workers;
	private static int count = 0;

	private static CollusionResultCertificator certificator;

	private final static double VARIANCE = 1E-4d;
	private static boolean stochastic = false;

	@BeforeClass
	public static void buildReputationSystem() {
		workers = new Worker[9];
		for (int i = 0; i < workers.length; i++)
			workers[i] = new Worker() {
				private int hash = count++;

				public int hashCode() {
					return hash;
				}
			};
		final Set<Worker> coll1 = new HashSet<Worker>();
		for (int i = 0; i < 3; i++)
			coll1.add(workers[i]);
		final Set<Worker> coll2 = new HashSet<Worker>();
		for (int i = 0; i < 3; i++)
			coll2.add(workers[i + 3]);
		final Set<Worker> coll3 = new HashSet<Worker>();
		for (int i = 0; i < 3; i++)
			coll3.add(workers[i + 6]);
		final Set<Worker> interColl1 = new HashSet<Worker>();
		interColl1.addAll(coll2);
		interColl1.addAll(coll3);
		final Set<Worker> interColl2 = new HashSet<Worker>();
		interColl2.addAll(coll1);
		interColl2.addAll(coll3);
		final Set<Worker> interColl3 = new HashSet<Worker>();
		interColl3.addAll(coll1);
		interColl3.addAll(coll2);
		certificator = new CollusionResultCertificator();
		certificator.setReputationSystem(new ReputationSystem<Worker>() {
			public void setWorkerResult(Worker worker, Job job, Result result) {
			}

			public void setCertifiedResult(Job job, Result result) {
			}

			public void removeAllWorkers(Set<? extends Worker> workers) {
			}

			public RV getReliability(Worker worker) {
				return null;
			}

			public void addAllWorkers(Set<? extends Worker> workers) {
			}

			public Map<Worker, simdeg.util.RV> getCollusionLikelihood(
					Worker worker, Set<Worker> workers) {
				return null;
			}

			/**
			 * coll1 colludes itself with proba 0.1. coll2 colludes itself with
			 * proba 0.3. coll3 colludes itself with proba 0.2.
			 */
			public RV getCollusionLikelihood(Set<Worker> workers) {
				if (coll1.containsAll(workers))
					return certificator.new RV(0.05d, stochastic ? VARIANCE
							: 0.0d);
				if (coll2.containsAll(workers))
					return certificator.new RV(0.06d, stochastic ? VARIANCE
							: 0.0d);
				if (coll3.containsAll(workers))
					return certificator.new RV(0.07d, stochastic ? VARIANCE
							: 0.0d);
				if (interColl1.containsAll(workers))
					return certificator.new RV(0.02d, stochastic ? VARIANCE
							: 0.0d);
				if (interColl2.containsAll(workers))
					return certificator.new RV(0.03d, stochastic ? VARIANCE
							: 0.0d);
				if (interColl3.containsAll(workers))
					return certificator.new RV(0.01d, stochastic ? VARIANCE
							: 0.0d);
				return certificator.new RV(0.0d, stochastic ? VARIANCE : 0.0d);
			}

			public RV getColludersFraction() {
				return null;
			}

			public Set<? extends Set<Worker>> getGroups(
					Collection<Worker> workers) {
				final Set<Set<Worker>> result = new HashSet<Set<Worker>>();
				for (Worker worker : workers)
					if (coll1.contains(worker))
						result.add(coll1);
					else if (coll2.contains(worker))
						result.add(coll2);
					else if (coll3.contains(worker))
						result.add(coll3);
				return result;
			}

			public Set<Worker> getLargestGroup() {
				return null;
			}
		});
	}

	/**
	 * All the results are the same. Since, inter-collusion is not possible for
	 * all workers, the result is correct with probability one.
	 */
	@Test
	public void agreeingVotingPool() {
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result = new ResultTest();
		pool.put(workers[0], result);
		pool.put(workers[1], result);
		pool.put(workers[2], result);
		pool.put(workers[3], result);
		pool.put(workers[4], result);
		pool.put(workers[6], result);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(1.0d, correctProba.get(result).getMean(), EPSILON);
	}

	/**
	 * Only one of the groups of collusion colludes (or the other two
	 * inter-colludes).
	 */
	@Test
	public void singleCollusionVotingPool() {
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result1 = new ResultTest();
		final Result result2 = new ResultTest();
		pool.put(workers[0], result1);
		pool.put(workers[1], result1);
		pool.put(workers[2], result1);
		pool.put(workers[3], result2);
		pool.put(workers[4], result2);
		pool.put(workers[5], result2);
		pool.put(workers[6], result2);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(0.0198d / 0.0298d, correctProba.get(result1).getMean(),
				EPSILON);
		assertEquals(0.0098d / 0.0298d, correctProba.get(result2).getMean(),
				EPSILON);
	}

	/**
	 * Each group of collusion colludes without any inter-collusion. With the
	 * same settings, but with an additional collusion group with which each
	 * group of collusion inter-colludes, with end up the case when the
	 * estimated probabilities are higher than the true ones.
	 */
	@Test
	public void multipleCollusionVotingPool() {
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result1 = new ResultTest();
		final Result result2 = new ResultTest();
		final Result result3 = new ResultTest();
		pool.put(workers[0], result1);
		pool.put(workers[1], result1);
		pool.put(workers[2], result1);
		pool.put(workers[3], result2);
		pool.put(workers[4], result2);
		pool.put(workers[6], result3);
		pool.put(workers[7], result3);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(0.000594d / 0.001088d,
				correctProba.get(result1).getMean(), EPSILON);
		assertEquals(0.000194d / 0.001088d,
				correctProba.get(result2).getMean(), EPSILON);
		assertEquals(0.000294d / 0.001088d,
				correctProba.get(result3).getMean(), EPSILON);
	}

	/**
	 * Tests that the approach provides a lower bound when the number of result
	 * groups is two. The probabilities are actually 0.4997/0.8997 and
	 * 0.3997/0.8997. An upper bound could be obtained with 0.5/0.9 and 0.4/0.9.
	 */
	@Test
	public void lowerBoundCollusionVotingPool() {
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result1 = new ResultTest();
		final Result result2 = new ResultTest();
		pool.put(workers[0], result1);
		pool.put(workers[1], result1);
		pool.put(workers[3], result2);
		pool.put(workers[4], result2);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(0.048d / 0.088d, correctProba.get(result1).getMean(),
				EPSILON);
		assertEquals(0.038d / 0.088d, correctProba.get(result2).getMean(),
				EPSILON);
	}

	/**
	 * Tests an impossible situation: some workers the first, but not all,
	 * colluding group colludes with some workers of the second.
	 */
	@Test
	public void incompatibleVotingPool() {
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result1 = new ResultTest();
		final Result result2 = new ResultTest();
		pool.put(workers[0], result1);
		pool.put(workers[3], result1);
		pool.put(workers[2], result2);
		pool.put(workers[4], result2);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(0.0d, correctProba.get(result1).getMean(), EPSILON);
		assertEquals(0.0d, correctProba.get(result2).getMean(), EPSILON);
	}

	/**
	 * Stochastic case with only one of the groups of collusion colludes (or the
	 * other two inter-colludes).
	 */
	@Test
	public void stochasticSingleCollusionVotingPool() {
		stochastic = true;
		final VotingPool<Result> pool = new VotingPool<Result>(null);
		final Result result1 = new ResultTest();
		final Result result2 = new ResultTest();
		pool.put(workers[0], result1);
		pool.put(workers[1], result1);
		pool.put(workers[2], result1);
		pool.put(workers[3], result2);
		pool.put(workers[4], result2);
		pool.put(workers[5], result2);
		pool.put(workers[6], result2);
		final Map<Result, RV> correctProba = certificator
				.correctProbability(pool);
		assertEquals(0.0198d / 0.0298d, correctProba.get(result1).getMean(),
				EPSILON);
		assertEquals(0.0098d / 0.0298d, correctProba.get(result2).getMean(),
				EPSILON);
		assertTrue(
				"The loss in precision must not exceed 100 times the initial variance ("
						+ VARIANCE + "): "
						+ correctProba.get(result1).getVariance(), correctProba
						.get(result1).getVariance() < 100 * VARIANCE);
		stochastic = false;
	}

	private class ResultTest extends HashableObject implements Result {
	}

}