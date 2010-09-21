package simdeg.simulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.OutOfRangeException;
import simdeg.util.RandomManager;

class CollusionGroup extends HashSet<Worker> {

	private static final long serialVersionUID = 1L;

	/** Logger */
	private static final Logger logger = Logger.getLogger(CollusionGroup.class
			.getName());

	/** Probability that each worker of this group colludes if they do not fail */
	final private double collusionProbability;

	/**
	 * Probability that the current group is not implicated in an
	 * inter-collusion behavior.
	 */
	private double probabilityLeft = 1.0d;

	/**
	 * Groups of inter-collusion to which belongs the current group of
	 * collusion.
	 */
	final private Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();

	/** History of previous choice for any job */
	final private Map<Job, Boolean> collusion = new HashMap<Job, Boolean>();

	/**
	 * Simple constructor used when workers are added one by one.
	 */
	protected CollusionGroup(double collusionProbability) {
		this(new HashSet<Worker>(), collusionProbability);
	}

	protected CollusionGroup(Set<Worker> workers, double collusionProbability) {
		super(workers);
		this.collusionProbability = collusionProbability;

		/* Test for admissibility of parameter */
		if (collusionProbability < 0.0d || collusionProbability > 1.0d)
			throw new OutOfRangeException(collusionProbability, 0.0d, 1.0d);
	}

	protected double getCollusionProbability() {
		return collusionProbability;
	}
	
	protected Set<InterCollusionGroup> getInterCollusionGroup() {
		return interCollusionGroups;
	}

	/**
	 * Adds a worker to the current group (updating the worker it-self).
	 */
	public boolean add(Worker worker) {
		final boolean result = super.add(worker);
		worker.setCollusionGroup(this);
		return result;
	}

	/**
	 * Adds a new group of inter-collusion to which belongs the current group of
	 * collusion. The probability that the current group colludes must be
	 * adapted accordingly.
	 */
	protected void add(InterCollusionGroup interCollusionGroup) {
		interCollusionGroups.add(interCollusionGroup);
		probabilityLeft -= interCollusionGroup.getInterCollusionProbability();
		if (collusionProbability > probabilityLeft)
			throw new OutOfRangeException(collusionProbability, 0.0d,
					probabilityLeft);
	}

	/**
	 * Returns the result of this group of collusion for a given job (either
	 * based on previous choice or based on the group of inter-collusion or
	 * finally based on the current probability of collusion.
	 */
	protected Result getResult(Job job) {
		for (InterCollusionGroup interCollusionGroup : interCollusionGroups)
			if (interCollusionGroup.getResult(job) != null)
				return interCollusionGroup.getResult(job);

		if (!collusion.containsKey(job)) {
			collusion.put(job, RandomManager.getRandom("reliability")
					.nextDouble() < collusionProbability / probabilityLeft);
			if (collusion.get(job))
				logger.fine("Group of collusion " + this + " collude for job "
						+ job);
		}

		if (collusion.get(job))
			return Result.getColludedResult(this);
		else
			return null;
	}

	/**
	 * Cleans data structures when a job will never be used again.
	 */
	protected void clear(Job job) {
		collusion.remove(job);
	}

	public String toString() {
		return "{" + collusionProbability + ": " + super.toString() + "}";
	}

}