package simdeg.scheduling;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * BOINC based scheduling mecanism.
 */
public class BOINCScheduler<J extends Job, R extends Result> extends
		Scheduler<J, R> {

	private final int minDuplication;

	public BOINCScheduler() {
		this(4, 3, 10);
	}

	public BOINCScheduler(int minDuplication, int quorum, int maxDuplication) {
		super(new BOINCResultCertificator(quorum, maxDuplication), null);
		this.minDuplication = minDuplication;
	}

	public BOINCScheduler(ResultCertificator resultCertificator,
			ReputationSystem<Worker> reputationSystem) {
		super(resultCertificator, reputationSystem);
		this.minDuplication = 4;
	}

	/**
	 * Requests a job for a given worker from the sets of active and processing
	 * jobs. This method justifies the use of generic.
	 */
	protected J pullJobFromSets(Worker worker) {
		/* If there are jobs that need more duplication to achieve quorum */
		for (J activeJob : activeJobs)
			if (!votingPools.get(activeJob).containsKey(worker))
				return activeJob;
		/* If there are jobs that need to be duplicated until the minimum amount */
		for (J processingJob : processingJobs) {
			final VotingPool<R> votingPool = votingPools.get(processingJob);
			if (votingPool.size() < minDuplication
					&& !votingPool.containsKey(worker))
				return processingJob;
		}
		/* No active or processing job needs to be duplicated */
		return null;
	}

	public String toString() {
		return "BOINCScheduler(" + minDuplication + ")";
	}

}