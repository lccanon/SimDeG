package simdeg.reputation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import simdeg.util.RV;

/**
 * Stores and handles all observations relative to the Desktop grids on which we
 * are running. Specifies additional methods given information about collusion
 * behavior among workers.
 */
public interface ReputationSystem extends BasicReputationSystem {

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same wrong result.
	 */
	public abstract RV getCollusionLikelihood(Set<? extends Worker> workers);

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public abstract <W extends Worker> Map<W, RV> getCollusionLikelihood(
			W worker, Set<W> workers);

	/**
	 * Returns the estimated fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public abstract RV getColludersFraction();

}
