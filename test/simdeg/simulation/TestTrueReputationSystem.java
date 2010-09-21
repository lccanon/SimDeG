package simdeg.simulation;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.util.RV;

/**
 * Builds a complex collusion configuration and check that some specific
 * scenarios do appear. It also test if the true reputation system works.
 */
public class TestTrueReputationSystem {

	private final static double EPSILON = 1E-6d;

	private static Worker[] workers;

	private static CollusionGroup collusionGroup1;
	private static CollusionGroup collusionGroup2;
	private static CollusionGroup collusionGroup3;
	private static CollusionGroup collusionGroup4;

	private static InterCollusionGroup interCollusionGroup1;
	private static InterCollusionGroup interCollusionGroup2;
	private static InterCollusionGroup interCollusionGroup3;

	private static TrueReputationSystem trueReputationSystem;

	@BeforeClass
	public static void buildCollusionConfiguration() {
		/* Workers creation */
		workers = new Worker[10];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Worker();
			workers[0].setReliability(0.9d);
		}

		/* CollusionGroup creation */
		collusionGroup1 = new CollusionGroup(0.45d);
		collusionGroup1.add(workers[0]);
		collusionGroup1.add(workers[1]);
		collusionGroup2 = new CollusionGroup(0.15d);
		collusionGroup2.add(workers[2]);
		collusionGroup2.add(workers[3]);
		collusionGroup3 = new CollusionGroup(0.24d);
		collusionGroup3.add(workers[4]);
		collusionGroup3.add(workers[5]);
		collusionGroup4 = new CollusionGroup(0.2d);
		collusionGroup4.add(workers[6]);
		collusionGroup4.add(workers[7]);

		/* InterCollusionGroup creation */
		interCollusionGroup1 = new InterCollusionGroup(0.1d);
		interCollusionGroup1.add(collusionGroup1);
		interCollusionGroup1.add(collusionGroup2);
		interCollusionGroup2 = new InterCollusionGroup(0.2d);
		interCollusionGroup2.add(collusionGroup3);
		interCollusionGroup2.add(collusionGroup4);
		interCollusionGroup3 = new InterCollusionGroup(0.4d);
		interCollusionGroup3.add(collusionGroup2);
		interCollusionGroup3.add(collusionGroup3);

		/* Decision tree creation */
		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		interCollusionGroups.add(interCollusionGroup3);
		new InterCollusionDecisionTree(interCollusionGroups);

		trueReputationSystem = new TrueReputationSystem();
		final Set<Worker> set = new HashSet<Worker>();
		for (Worker worker : workers)
			set.add(worker);
		trueReputationSystem.addAllWorkers(set);
	}

	@Test(timeout = 1000)
	public void collusion() {
		/* No group of inter-collusion colludes */
		while (true) {
			final Job job = new Job(0.0d);
			if (interCollusionGroup1.getResult(job) == null
					&& interCollusionGroup2.getResult(job) == null
					&& interCollusionGroup3.getResult(job) == null)
				break;
		}
		/* All groups of collusion collude */
		while (true) {
			final Job job = new Job(0.0d);
			if (collusionGroup1.getResult(job) == Result
					.getColludedResult(collusionGroup1)
					&& collusionGroup2.getResult(job) == Result
							.getColludedResult(collusionGroup2)
					&& collusionGroup3.getResult(job) == Result
							.getColludedResult(collusionGroup3)
					&& collusionGroup4.getResult(job) == Result
							.getColludedResult(collusionGroup4))
				break;
		}
		/* No group of collusion colludes */
		while (true) {
			final Job job = new Job(0.0d);
			if (collusionGroup1.getResult(job) == null
					&& collusionGroup2.getResult(job) == null
					&& collusionGroup3.getResult(job) == null
					&& collusionGroup4.getResult(job) == null)
				break;
		}
		/* The first group of collusion colludes but the first worker fails */
		while (true) {
			final Job job = new Job(0.0d);
			if (collusionGroup1.getResult(job) == Result
					.getColludedResult(collusionGroup1)
					&& workers[0].getResult(job) != Result
							.getColludedResult(collusionGroup1))
				break;
		}
	}

	@Test
	public void getReliability() {
		assertEquals(0.9d, trueReputationSystem.getReliability(workers[0])
				.getMean(), EPSILON);
	}

	/**
	 * Tests if a worker colludes with the correct probability according to the
	 * reputation system.
	 */
	@Test
	public void getCollusionLikelihoodSame() {
		final Set<Worker> set = new HashSet<Worker>();
		set.add(workers[0]);
		set.add(workers[0]);
		assertEquals(collusionGroup1.getCollusionProbability(),
				trueReputationSystem.getCollusionLikelihood(set).getMean(),
				EPSILON);
	}

	/**
	 * Tests if the workers from the same group of collusion collude with the
	 * correct probability according to the reputation system.
	 */
	@Test
	public void getCollusionLikelihoodIntra() {
		final Set<Worker> set = new HashSet<Worker>();
		set.add(workers[0]);
		set.add(workers[1]);
		assertEquals(collusionGroup1.getCollusionProbability(),
				trueReputationSystem.getCollusionLikelihood(set).getMean(),
				EPSILON);
	}

	/**
	 * Tests if the workers from the same group of inter-collusion collude with
	 * the correct probability according to the reputation system.
	 */
	@Test
	public void getCollusionLikelihoodInter() {
		final Set<Worker> set = new HashSet<Worker>();
		set.add(workers[2]);
		set.add(workers[4]);
		assertEquals(interCollusionGroup3.getInterCollusionProbability(),
				trueReputationSystem.getCollusionLikelihood(set).getMean(),
				EPSILON);
	}

	/**
	 * Tests if workers with distinct properties collude with a given worker
	 * with the correct probability according to the reputation system.
	 */
	@Test
	public void getCollusionLikelihood() {
		final Set<Worker> set = new HashSet<Worker>();
		set.add(workers[2]);
		set.add(workers[3]);
		set.add(workers[4]);
		set.add(workers[6]);
		set.add(workers[8]);
		final Map<Worker, RV> collusion = trueReputationSystem
				.getCollusionLikelihood(workers[2], set);
		assertEquals(collusionGroup2.getCollusionProbability(), collusion.get(
				workers[2]).getMean(), EPSILON);
		assertEquals(collusionGroup2.getCollusionProbability(), collusion.get(
				workers[3]).getMean(), EPSILON);
		assertEquals(interCollusionGroup3.getInterCollusionProbability(),
				collusion.get(workers[4]).getMean(), EPSILON);
		assertEquals(0.0d, collusion.get(workers[6]).getMean(), EPSILON);
		assertEquals(0.0d, collusion.get(workers[8]).getMean(), EPSILON);
	}

	@Test
	public void getColludersFraction() {
		assertEquals(8.0d / workers.length, trueReputationSystem
				.getColludersFraction().getMean(), EPSILON);
	}

}