package simdeg.reputation;

import static simdeg.util.Collections.addElement;
import static simdeg.util.RV.add;
import static simdeg.util.RV.max;
import static simdeg.util.RV.min;
import static simdeg.util.RV.subtract;
import static simdeg.util.RV.multiply;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import simdeg.util.RV;
import simdeg.util.Beta;
import simdeg.util.BetaEstimator;

/**
 * Strategy considering failures and collusion with convergence.
 */
public class AgreementReputationSystem extends SkeletonReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(AgreementReputationSystem.class.getName());

	private final AgreementMatrix agreement
        = new AgreementMatrix(new BetaEstimator(1.0d));

	/**
	 * Gives participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		super.addAllWorkers(workers);
		agreement.addAll(workers);
	}

	/**
	 * Remove participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		super.removeAllWorkers(workers);
		agreement.removeAll(workers);
	}

	/**
	 * Specifies that a worker agrees with a set of workers.
	 */
	protected void setAgreement(Job job, Worker worker, Set<Worker> workers) {
		assert (!workers.isEmpty()) : "Not enough workers in group";
        for (Worker otherWorker : workers) {
            final Set<Worker> setWorker = agreement.getSet(worker);
            final Set<Worker> setOtherWorker = agreement.getSet(otherWorker);
            if (updateInteraction(job, worker, otherWorker,
                        setWorker, setOtherWorker)) {
                agreement.increaseAgreement(worker, otherWorker);
                adaptInteractionStructure(job, setWorker, setOtherWorker,
                        agreement.getSet(worker), agreement.getSet(otherWorker));
            }
        }
	}

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected void setDisagreement(Job job, Worker worker, Set<Worker> workers) {
		assert (workers.size() > 1) : "Not enough workers in group";
        for (Worker otherWorker : workers) {
            final Set<Worker> setWorker = agreement.getSet(worker);
            final Set<Worker> setOtherWorker = agreement.getSet(otherWorker);
            if (updateInteraction(job, worker, otherWorker,
                        setWorker, setOtherWorker)) {
                agreement.decreaseAgreement(worker, otherWorker);
                adaptInteractionStructure(job, setWorker, setOtherWorker,
                        agreement.getSet(worker), agreement.getSet(otherWorker));
            }
        }
    }

	/**
	 * Specifies the winning group among all these groups of workers giving
     * distinct results. Workers does not contain the winning set. Useless in
     * this class.
	 */
	protected void setDistinctSets(Job job, Set<Worker> winningWorkers,
			Set<Set<Worker>> workers) {
	}

	/**
	 * Returns the estimated likelihood that a given group of workers give the
	 * same result.
	 */
	public RV getCollusionLikelihood(Set<? extends Worker> workers) {
		final RV[][] proba = agreement.getAgreements(workers);
		RV rv = new Beta(1.0d);
		for (int i = 0; i < proba[0].length; i++)
			rv = min(rv, multiply(proba[0][i], -1.0d).add(1.0d));
        for (int i = 1; i < proba.length; i++)
            for (int j = i; j < proba[i].length; j++)
                rv = min(rv, add(proba[i][j], 1.0d).subtract(proba[0][i - 1])
                        .subtract(proba[0][j]).multiply(0.5d)
                        .truncateRange(0.0d, 1.0d)).min(proba[i][j]);

		assert (rv.getMean() >= 0.0d) : "Negative estimate: "
				+ rv.getMean();

		logger.finest("Estimated collusion likelihood of " + workers.size()
				+ " workers is " + rv);
		return rv;
	}

	/**
	 * Returns the estimated likelihoods that a worker will return the same
	 * wrong result than each other.
	 */
	public <W extends Worker> Map<W, RV> getCollusionLikelihood(W worker,
			Set<W> workers) {
		Map<W, RV> result = new HashMap<W, RV>();
		Set<W> set = addElement(worker, new HashSet<W>());
		for (W otherWorker : workers) {
			set.add(otherWorker);
			result.put(otherWorker, getCollusionLikelihood(set));
			set.remove(otherWorker);
		}
		logger.finest("Estimated collusion likelihoods of worker " + worker
				+ " is " + result);
		return result;
	}

	/**
	 * Returns the estimated fraction of colluders (workers returning together
	 * the same wrong result).
	 */
	public RV getColludersFraction() {
        final int countAgreer = agreement.countAgreerMajority();
		final double fraction = 1.0d - (double)countAgreer / workers.size();
        final RV result = new RV(0.0d, 1.0d) {
            double error = agreement.getGeneralError();
            public RV clone() { return this; }
            public double getMean() { return fraction; }
            protected double getVariance() { return 0.0d; }
            public double getError() { return error; }
        };
		logger.finer("Estimated fraction of colluders: " + result);
		return result;
	}

    public String toString() {
        return super.toString() + "Agreement-based reputation system:\n"
            + agreement.toString();
    }

}
