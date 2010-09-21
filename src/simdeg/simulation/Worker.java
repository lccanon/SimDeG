package simdeg.simulation;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.HashableObject;
import simdeg.util.RandomManager;

/**
 * Workers are agents that treat more or less successfully the jobs they are
 * assigned to. They are characterized by their probability of failure and their
 * collusion behaviors.
 */
class Worker extends HashableObject implements simdeg.reputation.Worker {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Worker.class
			.getName());

	/** Speed of the worker in floating operations per second (immutable) */
	private double fops;

	/** Probability not to return a failed result (immutable) */
	private double reliability = 1.0d;

	/** Group of collusion to which belong the worker (null if none) (immutable) */
	private CollusionGroup collusionGroup;

	/** Job that the current worker is processing (null if none) */
	private Job currentJob;

	/** Remaining floating operations to be performed for the current job */
	private double remainingFops;

	/** Event that indicates the end of the computation of the current job */
	private ProcessCompletionEvent processCompletion;

	/** Event that indicates the timeout for the current job */
	private ProcessTimeoutEvent processTimeout;

	/**
	 * Precedent availability of unavailability event (null means it has never
	 * be available).
	 */
	private Event previousAvailabilityEvent;

	/**
	 * Specifies the speed of the worker (called only once).
	 */
	protected void setFOPS(double fops) {
		this.fops = fops;
	}

	protected double getReliability() {
		return reliability;
	}

	/**
	 * Specifies the reliability of the worker (called only once).
	 */
	protected void setReliability(double reliability) {
		this.reliability = reliability;
	}

	protected CollusionGroup getCollusionGroup() {
		return collusionGroup;
	}

	/**
	 * Specifies the group of collusion of the worker (called only once).
	 */
	protected void setCollusionGroup(CollusionGroup collusionGroup) {
		this.collusionGroup = collusionGroup;
	}

	/**
	 * Assigns a job to the current worker.
	 */
	protected void assignJob(Job job) {
		currentJob = job;
		if (job != null)
			remainingFops = job.getFOPS();
		processCompletion = null;
		processTimeout = null;
	}

	/**
	 * Returns the current processed Job
	 */
	protected Job getCurrentJob() {
		return currentJob;
	}

	/**
	 * Updates the computation that has been done as of the given date.
	 */
	protected void updateRemainingTime(double currentDate) {
		remainingFops = fops * (processCompletion.getDate() - currentDate);
	}

	/**
	 * Returns the remaining time for the current job.
	 */
	protected double getRemainingTime() {
		return remainingFops / fops;
	}

	/**
	 * Sets the completion time separately of the job assignment because the
	 * completion changes if the worker becomes unavailable.
	 */
	protected void setNextProcessCompletionEvent(
			ProcessCompletionEvent processCompletion) {
		this.processCompletion = processCompletion;
	}

	/**
	 * This event must be removed when the worker becomes unavailable.
	 */
	protected ProcessCompletionEvent getNextProcessCompletionEvent() {
		return processCompletion;
	}

	protected void setNextProcessTimeoutEvent(ProcessTimeoutEvent processTimeout) {
		this.processTimeout = processTimeout;
	}

	/**
	 * This event must be removed when the worker finish its assigned jobs.
	 */
	protected ProcessTimeoutEvent getNextProcessTimeoutEvent() {
		return processTimeout;
	}

	/**
	 * Sets the last available of unavailable event.
	 */
	protected void setPreviousAvailabilityEvent(Event event) {
		this.previousAvailabilityEvent = event;
	}

	protected Event getPreviousAvailabilityEvent() {
		return previousAvailabilityEvent;
	}

	/**
	 * Gets the result for the given job (takes into account the reliability and
	 * the groups of collusion).
	 */
	protected Result getResult(Job job) {
		if (RandomManager.getRandom("reliability").nextDouble() > reliability) {
			logger.fine("Worker " + this + " fails for job " + job);
			return Result.getFailedResult();
		}

		if (collusionGroup != null && collusionGroup.getResult(job) != null)
			return collusionGroup.getResult(job);

		return Result.getCorrectResult();
	}

	public String toString() {
		return "(" + hashCode() + ", " + fops + ", " + reliability + ")";
	}

	public static void main(String[] args) {
		/* Workers creation */
		Worker[] workers = new Worker[8];
		for (int i = 0; i < workers.length; i++)
			workers[i] = new Worker();
		/* CollusionGroup creation */
		CollusionGroup collusionGroup1 = new CollusionGroup(0.45d);
		collusionGroup1.add(workers[0]);
		collusionGroup1.add(workers[1]);
		CollusionGroup collusionGroup2 = new CollusionGroup(0.15d);
		collusionGroup2.add(workers[2]);
		collusionGroup2.add(workers[3]);
		CollusionGroup collusionGroup3 = new CollusionGroup(0.24d);
		collusionGroup3.add(workers[4]);
		collusionGroup3.add(workers[5]);
		CollusionGroup collusionGroup4 = new CollusionGroup(0.2d);
		collusionGroup4.add(workers[6]);
		collusionGroup4.add(workers[7]);
		/* InterCollusionGroup creation */
		InterCollusionGroup interCollusionGroup1 = new InterCollusionGroup(0.1d);
		interCollusionGroup1.add(collusionGroup1);
		interCollusionGroup1.add(collusionGroup2);
		InterCollusionGroup interCollusionGroup2 = new InterCollusionGroup(0.2d);
		interCollusionGroup2.add(collusionGroup3);
		interCollusionGroup2.add(collusionGroup4);
		InterCollusionGroup interCollusionGroup3 = new InterCollusionGroup(0.4d);
		interCollusionGroup3.add(collusionGroup2);
		interCollusionGroup3.add(collusionGroup3);
		/* Decision tree creation */
		Set<InterCollusionGroup> interCollusionGroups = new HashSet<InterCollusionGroup>();
		interCollusionGroups.add(interCollusionGroup1);
		interCollusionGroups.add(interCollusionGroup2);
		interCollusionGroups.add(interCollusionGroup3);
		new InterCollusionDecisionTree(interCollusionGroups);

		/* Do some Monte Carlo simulations */
		int countNoneInterColluding = 0;
		int countAllIntraColluding = 0;
		final double MC = 1E6;
		for (int i = 0; i < MC; i++) {
			Job job = new Job(0.0d);
			if (interCollusionGroup1.getResult(job) == null
					&& interCollusionGroup2.getResult(job) == null
					&& interCollusionGroup3.getResult(job) == null)
				countNoneInterColluding++;
			if (collusionGroup1.getResult(job) == Result
					.getColludedResult(collusionGroup1)
					&& collusionGroup2.getResult(job) == Result
							.getColludedResult(collusionGroup2)
					&& collusionGroup3.getResult(job) == Result
							.getColludedResult(collusionGroup3)
					&& collusionGroup4.getResult(job) == Result
							.getColludedResult(collusionGroup4))
				countAllIntraColluding++;
		}
		/* It should be around 32% and 0.72% */
		System.out.println((countNoneInterColluding / MC) + " "
				+ (countAllIntraColluding / MC));
	}

}