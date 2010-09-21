package simdeg.reputation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Skeleton easing development of reputation systems.
 */
public abstract class SkeletonReputationSystem<W extends Worker> extends
		ReliableReputationSystem<W> implements ReputationSystem<W> {

	/** Logger */
	private static final Logger logger = Logger
			.getLogger(SkeletonReputationSystem.class.getName());

	/**
	 * Keep track of each update made on the internal matrix (nice structure).
	 * For each job, and each internal set, the set of internal sets with whose
	 * it has already interacted is stored. This allows to avoid long sequence
	 * of similar events which would perturb the estimators. Additionally, the
	 * logic is to observe behavior between group, not individual.
	 */
	private final Map<Job, Map<Set<W>, Set<Set<W>>>> updatedSets = new HashMap<Job, Map<Set<W>, Set<Set<W>>>>();

	/**
	 * Informs to the reputation system a triple of worker, job, and result.
	 */
	public void setWorkerResult(W worker, Job job, Result result) {
		super.setWorkerResult(worker, job, result);

		/* Update structure for each new job */
		if (!updatedSets.containsKey(job))
			updatedSets.put(job, new HashMap<Set<W>, Set<Set<W>>>());

		/* Update collusion when this is the second result for this job */
		final Map<Result, Set<W>> workersByResult = workersByResults
				.get(job);
		if (workersByResult.get(result).size() == 2) {
			/* Find first worker of the set if it was alone */
			W firstWorker = null;
			for (W otherWorker : workersByResult.get(result))
				if (otherWorker != worker)
					firstWorker = otherWorker;

			/* Update its disagreements */
			for (Map.Entry<Result, Set<W>> entry : workersByResult
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
			Set<W> workers = new HashSet<W>(workersByResult
					.get(result));
			workers.remove(worker);
			setAgreement(job, worker, workers);
			logger.finer("Worker " + worker + " agrees with " + workers);

			/* Update disagreements */
			for (Map.Entry<Result, Set<W>> entry : workersByResult
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
	 * certified result associated to it.
	 */
	public void setCertifiedResult(Job job, Result result) {
		Map<Result, Set<W>> workersByResult = workersByResults.get(job);

		/* Inform in the grid characteristics which group gives separate results */
		Set<Set<W>> sets = new HashSet<Set<W>>();
		Set<W> winningSet = new HashSet<W>();
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
	 * Update each element in the matrix only once per computed job and per set.
	 */
	protected boolean updateInteraction(Job job, W worker,
			W otherWorker, Set<W> setWorker,
			Set<W> setOtherWorker) {
		if (!updatedSets.containsKey(job))
			return false;

		final Map<Set<W>, Set<Set<W>>> updatedSet = updatedSets
				.get(job);
		/* Fill the structure for easing the code that follows */
		if (!updatedSet.containsKey(setWorker))
			updatedSet.put(setWorker, new HashSet<Set<W>>());
		if (!updatedSet.containsKey(setOtherWorker))
			updatedSet.put(setOtherWorker, new HashSet<Set<W>>());
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
	protected void adaptInteractionStructure(Job job, Set<W> setWorker,
			Set<W> setOtherWorker, Set<W> newSetWorker,
			Set<W> newSetOtherWorker) {
		if (!updatedSets.containsKey(job))
			return;

		final Map<Set<W>, Set<Set<W>>> updatedSet = updatedSets
				.get(job);
		if (newSetWorker == newSetOtherWorker && newSetWorker != setWorker) {
			/* In case of merge */
			updatedSet.put(newSetWorker, new HashSet<Set<W>>());
			/* Regroup previous observations */
			final Set<Set<W>> alreadyObserved = new HashSet<Set<W>>(
					updatedSet.get(setWorker));
			/* Add observations symmetrically */
			alreadyObserved.addAll(updatedSet.get(setOtherWorker));
			updatedSet.get(newSetWorker).addAll(alreadyObserved);
			for (Set<W> set : alreadyObserved)
				updatedSet.get(set).add(newSetWorker);
			/* Handle the diagonal case */
			if (updatedSet.get(setWorker).contains(setWorker)
					&& updatedSet.get(setOtherWorker).contains(setOtherWorker))
				updatedSet.get(newSetWorker).add(newSetWorker);
		} else if (setWorker == setOtherWorker && newSetWorker != setWorker
				&& updatedSet.containsKey(setWorker)) {
			/* In case of split */
			Set<W> newBiggestSet = (newSetOtherWorker.size() > newSetWorker
					.size()) ? newSetOtherWorker : newSetWorker;
			updatedSet.put(newBiggestSet, new HashSet<Set<W>>());
			/* Add observations symmetrically */
			updatedSet.get(newBiggestSet).addAll(updatedSet.get(setWorker));
			for (Set<W> set : new HashSet<Set<W>>(updatedSet
					.get(setWorker)))
				updatedSet.get(set).add(newBiggestSet);
			/* Handle the diagonal case */
			if (updatedSet.get(setWorker).contains(setWorker))
				updatedSet.get(newBiggestSet).add(newBiggestSet);
		}
	}

	/**
	 * Specifies that a worker agrees with a set of workers. The specified
	 * worker can also be inside the set.
	 */
	protected abstract void setAgreement(Job job, W worker,
			Set<W> workers);

	/**
	 * Specifies that a worker disagrees with a set of workers.
	 */
	protected abstract void setDisagreement(Job job, W worker,
			Set<W> workers);

	/**
	 * Specifies the winning group among all these groups of workers giving
	 * distinct results. Workers does not contain the winning set.
	 */
	protected abstract void setDistinctSets(Job job,
			Set<W> winningWorkers, Set<Set<W>> workers);

}
