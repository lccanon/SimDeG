package simdeg.reputation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import simdeg.util.RV;

/**
 * Stores and handles all observations relative to the Desktop grids on which we
 * are running. Specifies additional methods given information about collusion
 * behavior among workers.
 */
public interface ReputationSystem<W extends Worker> extends
		BasicReputationSystem<W> {

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same wrong result.
	 */
	public abstract RV getCollusionLikelihood(Set<W> workers);

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public abstract Map<W, RV> getCollusionLikelihood(W worker, Set<W> workers);

	/**
	 * Returns the estimated fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public abstract RV getColludersFraction();

	public abstract Set<? extends Set<W>> getGroups(Collection<W> workers);
	
	public abstract Set<W> getLargestGroup();

}