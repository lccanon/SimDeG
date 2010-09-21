package simdeg.scheduling;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Collusion aware scheduling mechanism.
 */
public class CollusionScheduler<J extends Job, R extends Result> extends
		Scheduler<J, R> {

	public CollusionScheduler(ResultCertificator resultCertificator,
			ReputationSystem<Worker> reputationSystem) {
		super(resultCertificator, reputationSystem);
	}

	/**
	 * Requests a job for a given worker from the sets of active jobs. This
	 * method justifies the use of generic.
	 */
	protected J pullJobFromSets(Worker worker) {
		/* If there are jobs that need more duplication to achieve quorum */
		for (J activeJob : activeJobs)
			if (!votingPools.get(activeJob).containsKey(worker))
				return activeJob;
		/* No active or processing job needs to be duplicated */
		return null;
	}

	public String toString() {
		return "CollusionScheduler";
	}

}