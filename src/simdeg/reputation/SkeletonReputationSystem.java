package simdeg.reputation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import simdeg.util.Collections;
import simdeg.util.DynamicMatrix;
import simdeg.util.RV;

/**
 * Skeleton easing development of reputation systems.
 */
public abstract class SkeletonReputationSystem extends ReliableReputationSystem implements ReputationSystem {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(SkeletonReputationSystem.class.getName());

	/**
	 * Keep track of each update made on the internal matrix (nice structure).
     * For each job, and each internal set, the set of internal sets with whose
     * it has already interacted is stored. This allows to avoid long sequence
     * of similar events which would pertube the estimators. Additionally, the
     * logic is to observe behavior between group, not individual.
	 */
    private final Map<Job, Map<Set<Worker>, Set<Set<Worker>>>> updatedSets
        = new HashMap<Job, Map<Set<Worker>, Set<Set<Worker>>>>();

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public void setWorkerResult(Worker worker, Job job, Result result) {
        super.setWorkerResult(worker, job, result);

        /* Update structure for each new job */
        if (!updatedSets.containsKey(job))
            updatedSets.put(job, new HashMap<Set<Worker>, Set<Set<Worker>>>());

		/* Update collusion when this is the second result for this job */
        final Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);
		if (workersByResult.get(result).size() == 2) {
			/* Find first worker of the set if it was alone */
			Worker firstWorker = null;
			for (Worker otherWorker : workersByResult.get(result))
				if (otherWorker != worker)
					firstWorker = otherWorker;

			/* Update its disagreements */
			for (Map.Entry<Result, Set<Worker>> entry : workersByResult
					.entrySet())
				if (entry.getValue().size() > 1
						&& !result.equals(entry.getKey())) {
					setDisagreement(job, firstWorker, entry.getValue());
					logger.finer("Worker " + firstWorker + " disagrees with "
							+ entry.getValue());
				}
		}

		/* Update collusion when there is already more than 2 jobs */
		if (workersByResult.get(result).size() >= 2) {
			/* Update agreements */
			Set<Worker> workers = new HashSet<Worker>(workersByResult
					.get(result));
			workers.remove(worker);
			setAgreement(job, worker, workers);
			logger.finer("Worker " + worker + " agrees with " + workers);

			/* Update disagreements */
			for (Map.Entry<Result, Set<Worker>> entry : workersByResult
					.entrySet())
				if (entry.getValue().size() > 1
						&& !result.equals(entry.getKey())) {
					setDisagreement(job, worker, entry.getValue());
					logger.finer("Worker " + worker + " disagrees with "
							+ entry.getValue());
				}
		}
	}

	/**
	 * Informs to the reputation system a pair containing a job and the
	 * certfified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
		Map<Result, Set<Worker>> workersByResult = workersByResults.get(job);

		/* Inform in the grid characteristics which group gives separate results */
		Set<Set<Worker>> sets = new HashSet<Set<Worker>>();
		Set<Worker> winningSet = new HashSet<Worker>();
		for (Result otherResult : workersByResult.keySet()) {
			if (workersByResult.get(otherResult).size() != 1
					&& !result.equals(otherResult))
				sets.add(workersByResult.get(otherResult));
			if (result.equals(otherResult))
				winningSet = workersByResult.get(otherResult);
		}
		setDistinctSets(job, winningSet, sets);

		updatedSets.remove(job);

        super.setCertifiedResult(job, result);
	}

    /**
     * Update each element in the matrix only once per computed job and per
     * set.
     */
    protected boolean updateInteraction(Job job, Worker worker, Worker
            otherWorker, Set<Worker> setWorker, Set<Worker> setOtherWorker) {
        if (!updatedSets.containsKey(job))
            return false;

        final Map<Set<Worker>, Set<Set<Worker>>> updatedSet = updatedSets.get(job);
        /* Fill the structure for easing the code that follows */
        if (!updatedSet.containsKey(setWorker))
            updatedSet.put(setWorker, new HashSet<Set<Worker>>());
        if (!updatedSet.containsKey(setOtherWorker))
            updatedSet.put(setOtherWorker, new HashSet<Set<Worker>>());
        /* Performs the update if it is relevant (not already observed) */
        if (!updatedSet.get(setWorker).contains(setOtherWorker)) {
            /* Update the interactions already observed */
            updatedSet.get(setWorker).add(setOtherWorker);
            updatedSet.get(setOtherWorker).add(setWorker);
            return true;
        }
        return false;
    }

    /**
     * Handle the changes in the internal sets (cases when merge or split
     * happen).
     */
    protected void adaptInteractionStructure(Job job,
            Set<Worker> setWorker, Set<Worker> setOtherWorker,
            Set<Worker> newSetWorker, Set<Worker> newSetOtherWorker) {
        if (!updatedSets.containsKey(job))
            return;

        final Map<Set<Worker>, Set<Set<Worker>>> updatedSet = updatedSets.get(job);
        if (newSetWorker == newSetOtherWorker && newSetWorker != setWorker) {
            /* In case of merge */
            updatedSet.put(newSetWorker, new HashSet<Set<Worker>>());
            /* Regroup previous observations */
            final Set<Set<Worker>> alreadyObserved
                = new HashSet<Set<Worker>>(updatedSet.get(setWorker));
            /* Add observations symetrically */
            alreadyObserved.addAll(updatedSet.get(setOtherWorker));
            updatedSet.get(newSetWorker).addAll(alreadyObserved);
            for (Set<Worker> set : alreadyObserved)
                updatedSet.get(set).add(newSetWorker);
            /* Handle the diagonal case */
            if (updatedSet.get(setWorker).contains(setWorker)
                    && updatedSet.get(setOtherWorker).contains(setOtherWorker))
                updatedSet.get(newSetWorker).add(newSetWorker);
        } else if (setWorker == setOtherWorker && newSetWorker != setWorker
                && updatedSet.containsKey(setWorker)) {
            /* In case of split */
            Set<Worker> newBiggestSet = (newSetOtherWorker.size()
                    > newSetWorker.size()) ? newSetOtherWorker : newSetWorker;
            updatedSet.put(newBiggestSet, new HashSet<Set<Worker>>());
            /* Add observations symetrically */
            updatedSet.get(newBiggestSet).addAll(updatedSet.get(setWorker));
            for (Set<Worker> set
                    : new HashSet<Set<Worker>>(updatedSet.get(setWorker)))
                updatedSet.get(set).add(newBiggestSet);
            /* Hangle the diagonal case */
            if (updatedSet.get(setWorker).contains(setWorker))
                updatedSet.get(newBiggestSet).add(newBiggestSet);
        }
    }

	/**
	 * Specifies that a worker agrees with a set of workers. The specified
	 * worker can also be inside the set.
	 */
	protected abstract void setAgreement(Job job, Worker worker,
			Set<Worker> workers);

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected abstract void setDisagreement(Job job, Worker worker,
			Set<Worker> workers);

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct results. Workers does not contain the winning set.
	 */
	protected abstract void setDistinctSets(Job job,
			Set<Worker> winningWorkers, Set<Set<Worker>> workers);

}
