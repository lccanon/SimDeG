package simdeg.simulation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import simdeg.util.OutOfRangeException;

/**
 * Test that the decision tree for inter-collusion groups is correctly built.
 * The basic idea is that all functions work without raising exceptions. Few
 * assertions check that the probability are respected.
 */
public class TestInterCollusionGroup {

	private final static double EPSILON = 1E-6d;

	private static CollusionGroup buildCollusionGroup(double probability) {
		CollusionGroup collusionGroup = new CollusionGroup(probability);
		collusionGroup.add(new Worker());
		collusionGroup.add(new Worker());
		return collusionGroup;
	}

	@Test
	public void oneInterCollusionGroup() {
		InterCollusionGroup interCollusionGroup = new InterCollusionGroup(0.5d);
		interCollusionGroup.add(buildCollusionGroup(0.2d));
		interCollusionGroup.add(buildCollusionGroup(0.2d));

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup);
		InterCollusionDecisionTree decision = new InterCollusionDecisionTree(
				interCollusionGroups);

		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup),
				EPSILON);
	}

	@Test
	public void getInterCollusionDecision() {
		InterCollusionGroup interCollusionGroup = new InterCollusionGroup(0.5d);
		interCollusionGroup.add(buildCollusionGroup(0.2d));
		interCollusionGroup.add(buildCollusionGroup(0.2d));

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup);
		new InterCollusionDecisionTree(interCollusionGroups);

		interCollusionGroup.getResult(new Job(1.0d));
	}

	@Test(expected = OutOfRangeException.class)
	public void incorrectCollusionGroupException() {
		CollusionGroup collusionGroup1 = new CollusionGroup(0.2d);
		collusionGroup1.add(new Worker());
		CollusionGroup collusionGroup2 = new CollusionGroup(0.2d);
		collusionGroup2.add(new Worker());

		InterCollusionGroup interCollusionGroup = new InterCollusionGroup(0.5d);
		interCollusionGroup.add(collusionGroup1);
		interCollusionGroup.add(collusionGroup2);

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup);
		new InterCollusionDecisionTree(interCollusionGroups);
	}

	@Test(expected = OutOfRangeException.class)
	public void incorrectInterCollusionGroupException() {
		InterCollusionGroup interCollusionGroup = new InterCollusionGroup(0.5d);
		interCollusionGroup.add(buildCollusionGroup(0.2d));

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup);
		new InterCollusionDecisionTree(interCollusionGroups);
	}

	@Test(expected = NullPointerException.class)
	public void noDecisionTreeException() {
		InterCollusionGroup interCollusionGroup = new InterCollusionGroup(0.5d);
		interCollusionGroup.add(buildCollusionGroup(0.2d));
		interCollusionGroup.add(buildCollusionGroup(0.2d));

		interCollusionGroup.getResult(new Job(1.0d));
	}

	@Test
	public void twoIndependentInterCollusionGroups() {
		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.2d));
		interCollusionGroup1.add(buildCollusionGroup(0.2d));

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(0.5d);
		interCollusionGroup2.add(buildCollusionGroup(0.2d));
		interCollusionGroup2.add(buildCollusionGroup(0.2d));

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		InterCollusionDecisionTree decision = new InterCollusionDecisionTree(
				interCollusionGroups);

		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup1),
				EPSILON);
		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup2),
				EPSILON);
	}

	@Test
	public void twoOverlappingInterCollusionGroups() {
		CollusionGroup overlappingGroup = buildCollusionGroup(0.0d);

		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.2d));
		interCollusionGroup1.add(overlappingGroup);

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(0.5d);
		interCollusionGroup2.add(buildCollusionGroup(0.2d));
		interCollusionGroup2.add(overlappingGroup);

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		InterCollusionDecisionTree decision = new InterCollusionDecisionTree(
				interCollusionGroups);

		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup1),
				EPSILON);
		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup2),
				EPSILON);
	}

	@Test(expected = OutOfRangeException.class)
	public void twoOverlappingInterCollusionGroupsException() {
		CollusionGroup overlappingGroup = buildCollusionGroup(0.1d);

		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.2d));
		interCollusionGroup1.add(overlappingGroup);

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(0.5d);
		interCollusionGroup2.add(buildCollusionGroup(0.2d));
		interCollusionGroup2.add(overlappingGroup);

		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		new InterCollusionDecisionTree(interCollusionGroups);
	}

	@Test
	public void threeInterCollusionGroups() {
		CollusionGroup overlappingGroup1 = buildCollusionGroup(0.25d);
		CollusionGroup overlappingGroup2 = buildCollusionGroup(0.25d);

		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.5d));
		interCollusionGroup1.add(overlappingGroup1);

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(
				0.25d);
		interCollusionGroup2.add(overlappingGroup1);
		interCollusionGroup2.add(overlappingGroup2);

		InterCollusionGroup interCollusionGroup3 = new InterCollusionGroup(0.5d);
		interCollusionGroup3.add(overlappingGroup2);
		interCollusionGroup3.add(buildCollusionGroup(0.5d));

		Collection<InterCollusionGroup> interCollusionGroups = new ArrayList<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		interCollusionGroups.add(interCollusionGroup3);
		InterCollusionDecisionTree decision = new InterCollusionDecisionTree(
				interCollusionGroups);

		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup1),
				EPSILON);
		assertEquals(0.25d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup2),
				EPSILON);
		assertEquals(0.5d, decision
				.totalProbabilityInterCollusionGroup(interCollusionGroup3),
				EPSILON);
	}

	@Test(expected = OutOfRangeException.class)
	public void threeInterCollusionGroupsException() {
		CollusionGroup overlappingGroup1 = buildCollusionGroup(0.0d);
		CollusionGroup overlappingGroup2 = buildCollusionGroup(0.0d);

		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.0d));
		interCollusionGroup1.add(overlappingGroup1);

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(
				0.26d);
		interCollusionGroup2.add(overlappingGroup1);
		interCollusionGroup2.add(overlappingGroup2);

		InterCollusionGroup interCollusionGroup3 = new InterCollusionGroup(0.5d);
		interCollusionGroup3.add(overlappingGroup2);
		interCollusionGroup3.add(buildCollusionGroup(0.0d));

		Collection<InterCollusionGroup> interCollusionGroups = new ArrayList<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		interCollusionGroups.add(interCollusionGroup3);
		new InterCollusionDecisionTree(interCollusionGroups);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void constructionFailureException() {
		CollusionGroup overlappingGroup1 = buildCollusionGroup(0.25d);
		CollusionGroup overlappingGroup2 = buildCollusionGroup(0.25d);

		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.5d);
		interCollusionGroup1.add(buildCollusionGroup(0.5d));
		interCollusionGroup1.add(overlappingGroup1);

		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(
				0.25d);
		interCollusionGroup2.add(overlappingGroup1);
		interCollusionGroup2.add(overlappingGroup2);

		InterCollusionGroup interCollusionGroup3 = new InterCollusionGroup(0.5d);
		interCollusionGroup3.add(overlappingGroup2);
		interCollusionGroup3.add(buildCollusionGroup(0.5d));

		Collection<InterCollusionGroup> interCollusionGroups = new ArrayList<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup2);
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup3);
		new InterCollusionDecisionTree(interCollusionGroups);
	}

}