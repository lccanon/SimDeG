package simdeg.simulation;

import simdeg.util.HashableObject;

/**
 * A job is defined by a task with a given cost to be computed on a given worker
 * and given a specific result. A job is created by the simulator and is
 * submitted to the scheduler which duplicates it correctly and associate to
 * each a worker. Then, one of the result is selected.
 */
class Job extends HashableObject implements simdeg.reputation.Job {

	/**
	 * Number of floating operations to be performed in order to complete this
	 * job.
	 */
	private final double fops;

	protected Job(double fops) {
		this.fops = fops;
	}

	protected double getFOPS() {
		return fops;
	}

	public String toString() {
		return "(" + hashCode() + ", " + fops + ")";
	}

}