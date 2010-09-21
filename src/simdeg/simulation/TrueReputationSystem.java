package simdeg.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.util.Beta;
import simdeg.util.RV;

/**
 * Reputation system that characterizes precisely the platform reliability. It
 * can be used for measuring the performance of a reputation system through
 * comparison or for testing the validity of a result certification mechanism.
 */
class TrueReputationSystem implements ReputationSystem<Worker> {

	private double colludersFraction = 0.0d;

	public void addAllWorkers(Set<? extends Worker> workers) {
		for (Worker worker : workers)
			if (worker.getCollusionGroup() != null)
				colludersFraction++;
		colludersFraction /= workers.size();
	}

	public void removeAllWorkers(Set<? extends Worker> workers) {
	}

	public void setWorkerResult(Worker worker, Job job, Result result) {
	}

	public void setCertifiedResult(Job job, Result result) {
	}

	public RV getReliability(Worker worker) {
		return new Beta(worker.getReliability());
	}

	@Override
	public RV getCollusionLikelihood(Set<Worker> workers) {
		Set<CollusionGroup> groups = new HashSet<CollusionGroup>();
		for (Worker worker : workers)
			groups.add(worker.getCollusionGroup());
		if (groups.isEmpty())
			return new Beta(0.0d);
		if (groups.size() == 1)
			return new Beta(groups.iterator().next().getCollusionProbability());
		double interCollusion = 0.0d;
		for (InterCollusionGroup interGroup : groups.iterator().next()
				.getInterCollusionGroup())
			if (interGroup.containsAll(groups))
				interCollusion += interGroup.getInterCollusionProbability();
		return new Beta(interCollusion);
	}

	@Override
	public Map<Worker, RV> getCollusionLikelihood(Worker worker,
			Set<Worker> workers) {
		final CollusionGroup group = worker.getCollusionGroup();
		Map<Worker, RV> result = new HashMap<Worker, RV>();
		for (Worker worker2 : workers) {
			if (group == null || worker2.getCollusionGroup() == null)
				result.put(worker2, new Beta(0.0d));
			else if (worker2.getCollusionGroup() == group)
				result.put(worker2, new Beta(group.getCollusionProbability()));
			else {
				double interCollusion = 0.0d;
				for (InterCollusionGroup interGroup : group
						.getInterCollusionGroup())
					if (interGroup.contains(worker2.getCollusionGroup()))
						interCollusion += interGroup
								.getInterCollusionProbability();
				result.put(worker2, new Beta(interCollusion));
			}
		}
		return result;
	}

	@Override
	public RV getColludersFraction() {
		return new Beta(colludersFraction);
	}

	@Override
	public Set<? extends Set<Worker>> getGroups(Collection<Worker> workers) {
		final Set<CollusionGroup> result = new HashSet<CollusionGroup>();
		for (Worker worker : workers)
			result.add(worker.getCollusionGroup());
		return result;
	}

	@Override
	public Set<Worker> getLargestGroup() {
		// TODO Auto-generated method stub
		return null;
	}

}