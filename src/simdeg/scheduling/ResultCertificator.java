package simdeg.scheduling;

import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Class allowing to select one result from a set of results.
 */
public abstract class ResultCertificator {

	ReputationSystem<Worker> reputationSystem = null;

	/**
	 * Specifies the object characterizing the information we have on the grid.
	 */
	void setReputationSystem(ReputationSystem<Worker> reputationSystem) {
		this.reputationSystem = reputationSystem;
	}

	/**
	 * Returns the best result according to some defined policy and grid
	 * characteristics.
	 */
	abstract <R extends Result> R certifyResult(VotingPool<R> votingPool);

}