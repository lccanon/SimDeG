package simdeg.scheduling;

import java.util.EventListener;

import simdeg.reputation.Job;
import simdeg.reputation.Result;

/**
 * Specifies the methods that have to be called by the scheduler whenever a
 * significant event occurs.
 */
public interface SchedulerListener extends EventListener {

	/**
	 * Specifies that there is no more unassigned job in the queue of jobs (risk
	 * of starvation).
	 */
	public void endOfJobQueue();

	/**
	 * Specifies that the given result is certified for the given job.
	 */
	public <J extends Job, R extends Result> void setCertifiedResult(
			VotingPool<R> votingPool, R result);

}