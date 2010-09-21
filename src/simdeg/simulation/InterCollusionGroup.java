package simdeg.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.OutOfRangeException;
import simdeg.util.RandomManager;

/**
 * A group of inter-collusion contains several groups of collusion (at least
 * two) and defines their probability of inter-collusion.
 */
class InterCollusionGroup extends HashSet<CollusionGroup> {

	private static final long serialVersionUID = 1L;

	/**
	 * Global variable that is used to make decision about which inter-colluding
	 * group collude.
	 */
	private InterCollusionDecisionTree collusionDecision = null;

	/**
	 * Probability of inter-collusion.
	 */
	private double interCollusionProbability = 0.0d;

	/**
	 * Store the decision for a given job, which allows to avoid bias.
	 */
	private Map<Job, Boolean> interCollusion = new HashMap<Job, Boolean>();

	protected InterCollusionGroup(double interCollusionProbability) {
		this(new HashSet<CollusionGroup>(), interCollusionProbability);
	}

	/**
	 * Builds a group of inter-collusion based on a set of group of collusion
	 * and a probability of inter-collusion. Adds the result to the decision
	 * tree.
	 */
	private InterCollusionGroup(Set<CollusionGroup> collusionGroups,
			double interCollusionProbability) {
		super(collusionGroups);
		this.interCollusionProbability = interCollusionProbability;

		/* Test for admissibility of parameter */
		if (interCollusionProbability < 0.0d
				|| interCollusionProbability > 1.0d)
			throw new OutOfRangeException(interCollusionProbability, 0.0d, 1.0d);
	}

	public boolean add(CollusionGroup collusionGroup) {
		final boolean result = super.add(collusionGroup);
		collusionGroup.add(this);
		return result;
	}

	protected double getInterCollusionProbability() {
		return interCollusionProbability;
	}

	protected void putInterCollusionDecisionTree(
			InterCollusionDecisionTree interCollusionDecisionTree) {
		this.collusionDecision = interCollusionDecisionTree;
	}

	/**
	 * Allows the decision tree to update if there will be inter-collusion for
	 * the given job.
	 */
	protected void putInterCollusion(Job job, boolean decision) {
		this.interCollusion.put(job, decision);
	}

	/**
	 * Tests if the current inter-colluding group inter-collude for the given
	 * job, in which case the contained groups of collusion may not collude
	 * them-selves.
	 */
	protected Result getResult(Job job) {
		if (collusionDecision == null)
			throw new NullPointerException(
					"Need to have access to a decision tree");

		if (!interCollusion.containsKey(job))
			collusionDecision.getInterCollusionDecision(job);

		if (interCollusion.get(job))
			return Result.getInterColludedResult(this);
		else
			return null;
	}

	protected void clear(Job job) {
		interCollusion.remove(job);
	}

	public String toString() {
		return "{" + interCollusionProbability + ": " + super.toString() + "}";
	}

}

/**
 * Structure for selecting the inter-colluding groups that will collude for a
 * given job. The tree size increases twofold each time an inter-colluding group
 * is added. This allows to avoid recomputations of the overlaps between each
 * pair of inter-colluding groups.
 */
class InterCollusionDecisionTree {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(InterCollusionDecisionTree.class.getName());

	/**
	 * Inter-collusion group that is represented at this node.
	 */
	private InterCollusionGroup interCollusionGroup = null;

	/**
	 * Probability that the inter-collusion happens and that the interCollusion
	 * tree is explored.
	 */
	private double interCollusionProbability = 0.0d;

	/**
	 * Global probability ot end up to this node in the tree.
	 */
	private double decisionProbability = 0.0d;

	/**
	 * Subtree that is explored in case the inter-collusion for the current
	 * inter-collusion group happens.
	 */
	private InterCollusionDecisionTree interCollusion = null;

	/**
	 * Subtree that is explored otherwise (no inter-collusion).
	 */
	private InterCollusionDecisionTree noCollusion = null;

	protected InterCollusionDecisionTree(
			Collection<InterCollusionGroup> interCollusionGroups) {
		this(interCollusionGroups.isEmpty() ? null : interCollusionGroups
				.iterator().next());
		if (interCollusionGroups.isEmpty())
			return;
		for (InterCollusionGroup interCollusionGroup : interCollusionGroups) {
			if (interCollusionGroup != this.interCollusionGroup)
				add(interCollusionGroup);
			interCollusionGroup.putInterCollusionDecisionTree(this);
		}
		if (!checkIndependence(interCollusionGroups))
			throw new UnsupportedOperationException("Some events are dependent");
	}

	/**
	 * Constructor called outside of the class for the initialization of the
	 * tree.
	 */
	private InterCollusionDecisionTree(InterCollusionGroup interCollusionGroup) {
		this(interCollusionGroup, interCollusionGroup == null ? 0.0d
				: interCollusionGroup.getInterCollusionProbability(), 1.0d);
	}

	/**
	 * Complete constructor called when incrementing the tree.
	 */
	private InterCollusionDecisionTree(InterCollusionGroup interCollusionGroup,
			double interCollusionProbability, double decisionProbability) {
		if (interCollusionGroup == null)
			return;
		this.interCollusionGroup = interCollusionGroup;
		if (decisionProbability != 0.0d)
			this.interCollusionProbability = interCollusionProbability
					/ decisionProbability;
		this.decisionProbability = decisionProbability;

		/* Test for admissibility of parameters */
		if (interCollusionGroup.size() < 2)
			throw new OutOfRangeException(interCollusionGroup.size(), 2,
					Integer.MAX_VALUE);
		for (CollusionGroup collusionGroup : interCollusionGroup)
			if (collusionGroup.size() < 2)
				throw new OutOfRangeException(collusionGroup.size(), 2,
						Integer.MAX_VALUE);
		if (this.interCollusionProbability < 0.0d
				|| this.interCollusionProbability > 1.0d)
			throw new OutOfRangeException(this.interCollusionProbability, 0.0d,
					1.0d);
		if (this.decisionProbability < 0.0d || this.decisionProbability > 1.0d)
			throw new OutOfRangeException(this.decisionProbability, 0.0d, 1.0d);
	}

	private void add(InterCollusionGroup interCollusionGroup) {
		add(interCollusionGroup, interCollusionGroup
				.getInterCollusionProbability());
	}

	/**
	 * Method used for adding leafs recursively.
	 */
	private void add(InterCollusionGroup interCollusionGroup,
			double interCollusionProbability) {
		assert (interCollusionGroup != this.interCollusionGroup) : "A group of inter-collusion must not be added twice";

		/*
		 * Compute the probability that the inter-collusion may happen for the
		 * group to be inserted depending of the child that is selected at the
		 * current node.
		 */
		double childInterCollusionProbability;
		double childNoCollusionProbability;
		if (!commonElement(this.interCollusionGroup, interCollusionGroup)) {
			childInterCollusionProbability = interCollusionProbability
					* this.interCollusionProbability;
			childNoCollusionProbability = interCollusionProbability
					* (1.0d - this.interCollusionProbability);
		} else {
			childInterCollusionProbability = 0.0d;
			childNoCollusionProbability = interCollusionProbability;
		}

		/* Test if the children have to be inserted here or recursively */
		if (interCollusion == null && noCollusion == null) {
			interCollusion = new InterCollusionDecisionTree(
					interCollusionGroup, childInterCollusionProbability,
					decisionProbability * this.interCollusionProbability);
			noCollusion = new InterCollusionDecisionTree(interCollusionGroup,
					childNoCollusionProbability, decisionProbability
							* (1.0d - this.interCollusionProbability));
		} else {
			assert (interCollusion != null && noCollusion != null) : "Decision tree not binary";
			interCollusion.add(interCollusionGroup,
					childInterCollusionProbability);
			noCollusion.add(interCollusionGroup, childNoCollusionProbability);
		}
	}

	/* Compute intersection of two group of inter-collusion */
	private boolean commonElement(Set<CollusionGroup> set1,
			Set<CollusionGroup> set2) {
		final Set<CollusionGroup> intersection = new HashSet<CollusionGroup>(
				set1);
		intersection.retainAll(set2);
		return !intersection.isEmpty();
	}

	/**
	 * Procedure that updates the map in each inter-collusion group.
	 */
	protected void getInterCollusionDecision(Job job) {
		if (RandomManager.getRandom("reliability").nextDouble() < interCollusionProbability) {
			this.interCollusionGroup.putInterCollusion(job, true);
			logger.fine("Group of inter-collusion " + interCollusionGroup
					+ " collude for job " + job);
			if (interCollusion != null)
				interCollusion.getInterCollusionDecision(job);
		} else {
			this.interCollusionGroup.putInterCollusion(job, false);
			if (noCollusion != null)
				noCollusion.getInterCollusionDecision(job);
		}
	}

	/**
	 * Computes the probability that a groups of inter-collusion colludes. Used
	 * for testing if the tree encodes correctly the probability for each group.
	 */
	protected double totalProbabilityInterCollusionGroup(
			InterCollusionGroup interCollusionGroup) {
		if (this.interCollusionGroup == interCollusionGroup)
			return decisionProbability * interCollusionProbability;
		if (interCollusion == null || noCollusion == null)
			throw new NullPointerException("Incomplete decision tree");
		return interCollusion
				.totalProbabilityInterCollusionGroup(interCollusionGroup)
				+ noCollusion
						.totalProbabilityInterCollusionGroup(interCollusionGroup);
	}

	/**
	 * Computes the probability that two groups of inter-collusion collude at
	 * the same time.
	 */
	private double totalProbabilityInterCollusionGroup(
			InterCollusionGroup interCollusionGroup1,
			InterCollusionGroup interCollusionGroup2) {
		if (interCollusion == null && noCollusion == null)
			if (this.interCollusionGroup == interCollusionGroup1
					|| this.interCollusionGroup == interCollusionGroup2)
				return decisionProbability * interCollusionProbability;
			else
				return decisionProbability;
		if (interCollusion == null || noCollusion == null)
			throw new NullPointerException("Incomplete decision tree");
		if (this.interCollusionGroup == interCollusionGroup1
				|| this.interCollusionGroup == interCollusionGroup2)
			return interCollusion.totalProbabilityInterCollusionGroup(
					interCollusionGroup1, interCollusionGroup2);
		else
			return interCollusion.totalProbabilityInterCollusionGroup(
					interCollusionGroup1, interCollusionGroup2)
					+ noCollusion.totalProbabilityInterCollusionGroup(
							interCollusionGroup1, interCollusionGroup2);
	}

	private boolean checkIndependence(
			Collection<InterCollusionGroup> interCollusionGroups) {
		for (InterCollusionGroup interCollusionGroup1 : interCollusionGroups)
			for (InterCollusionGroup interCollusionGroup2 : interCollusionGroups)
				if (!commonElement(interCollusionGroup1, interCollusionGroup2)) {
					final double intersectionProbability = totalProbabilityInterCollusionGroup(
							interCollusionGroup1, interCollusionGroup2);
					if (intersectionProbability != interCollusionGroup1
							.getInterCollusionProbability()
							* interCollusionGroup2
									.getInterCollusionProbability())
						return false;
				}
		return true;
	}

	public String toString() {
		return "[" + interCollusionProbability + " -> " + interCollusion + ", "
				+ (1.0d - interCollusionProbability) + " -> " + noCollusion
				+ "]";
	}

}