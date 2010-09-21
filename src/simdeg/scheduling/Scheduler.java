package simdeg.scheduling;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.Job;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Main component of the scheduling package. It provides a generic interface for
 * assembling scheduling component to form a policy.
 */
abstract public class Scheduler<J extends Job, R extends Result> {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Scheduler.class
			.getName());

	/** Component for certifying one result for each job */
	private ResultCertificator resultCertificator;

	/**
	 * Component that characterize the reliability of the platform based on the
	 * observations.
	 */
	protected ReputationSystem<Worker> reputationSystem;

	/** Jobs available for being added to workers waiting queues */
	protected Queue<J> availableJobs = new ArrayDeque<J>();

	/** Jobs that have been processed but not currently */
	protected Set<J> activeJobs = new HashSet<J>();

	/** Jobs being processed by at least one worker */
	protected Set<J> processingJobs = new HashSet<J>();

	/** Each terminated jobs has one certified result and is not being processed */
	private Set<J> terminatedJobs = new HashSet<J>();

	/**
	 * Voting pools obtained so far (keys correspond to all jobs except those
	 * that are available).
	 */
	protected Map<J, VotingPool<R>> votingPools = new HashMap<J, VotingPool<R>>();

	private SchedulerListener listener;

	/**
	 * Constructs a Scheduler with the given components.
	 */
	public Scheduler(ResultCertificator resultCertificator,
			ReputationSystem<Worker> reputationSystem) {
		this.resultCertificator = resultCertificator;
		this.reputationSystem = reputationSystem;
		this.resultCertificator.setReputationSystem(reputationSystem);
		logger.info(this + " created with " + resultCertificator + " and "
				+ reputationSystem);
	}

	/**
	 * Adds participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		if (reputationSystem != null)
			reputationSystem.addAllWorkers(workers);
	}

	/**
	 * Removes participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		if (reputationSystem != null)
			reputationSystem.removeAllWorkers(workers);
	}

	/**
	 * Adds a single work-unit to be treated.
	 */
	public void addJob(J job) {
		this.availableJobs.add(job);
	}

	/**
	 * Adds a workload to be treated in an arbitrary order.
	 */
	public void addAllJobs(Set<J> jobs) {
		for (J job : jobs)
			addJob(job);
	}

	/**
	 * Gives access to the reputation system that is used to characterize the
	 * platform.
	 */
	public ReputationSystem<Worker> getReputationSystem() {
		return reputationSystem;
	}

	/**
	 * Tests whether a worker is working on any job or not.
	 */
	private boolean isAssigned(Worker worker) {
		for (VotingPool<R> votingPool : votingPools.values())
			if (votingPool.containsKey(worker)
					&& votingPool.get(worker) == null)
				return true;
		return false;
	}

	/**
	 * Gives the result of a worker for a given job.
	 */
	public J submitResultAndPullJob(Worker worker, J job, R result) {
		if (job != null) {
			if (!votingPools.containsKey(job)
					|| !votingPools.get(job).containsKey(worker))
				throw new UnsupportedOperationException(
						"This worker was never assigned to this job");
			if (votingPools.get(job).get(worker) != null)
				throw new UnsupportedOperationException(
						"This worker has already computed this job");

			final VotingPool<R> votingPool = votingPools.get(job);
			if (result == null) {
				/*
				 * The worker gives up the computation without asking for
				 * another job
				 */
				votingPool.remove(worker);
				if (votingPool.isComplete()) {
					processingJobs.remove(job);
					activeJobs.add(job);
				}
				return null;
			} else {
				logger.fine("Worker " + worker + " returns result " + result
						+ " for job " + job);
				votingPool.put(worker, result);
				if (reputationSystem != null)
					reputationSystem.setWorkerResult(worker, job, result);

				if (votingPool.isComplete()) {
					processingJobs.remove(job);
					/* Try to certify one of the results for the given job */
					final R certifiedResult = resultCertificator
							.certifyResult(votingPool);
					/* Update the sets of jobs accordingly */
					// TODO takes also into account the maximum number of
					// workers
					if (certifiedResult != null) {
						terminatedJobs.add(job);
						votingPools.remove(votingPool);
						if (reputationSystem != null)
							reputationSystem.setCertifiedResult(job,
									certifiedResult);
						if (listener != null)
							listener.setCertifiedResult(votingPool,
									certifiedResult);
					} else
						activeJobs.add(job);
				}
			}
		} else
			assert (!isAssigned(worker)) : "A worker may not compute several jobs at the same time";

		/* Call custom mechanism for pulling a new job */
		J pulledJob = pullJobFromSets(worker);
		if (pulledJob == null) {
			if (availableJobs.isEmpty())
				// TODO specifies that current processing and active jobs may be
				// run
				return null;
			else {
				/* Activate the next job */
				pulledJob = availableJobs.poll();
				if (availableJobs.isEmpty() && listener != null)
					listener.endOfJobQueue();
			}
		}
		logger.fine("Worker " + worker + " pull job " + pulledJob);

		/* Update job sets */
		activeJobs.remove(pulledJob);
		processingJobs.add(pulledJob);

		/* Update voting pools */
		if (!votingPools.containsKey(pulledJob))
			votingPools.put(pulledJob, new VotingPool<R>(pulledJob));
		votingPools.get(pulledJob).put(worker, null);

		return pulledJob;
	}

	/**
	 * Requests a job for a given worker from the sets of active and processing
	 * jobs. This method justifies the use of generic.
	 */
	abstract protected J pullJobFromSets(Worker worker);

	/**
	 * Specifies the listener that will handles the job submissions and the
	 * certified results for this scheduler
	 */
	public void putSchedulerListener(SchedulerListener schedulerListener) {
		listener = schedulerListener;
	}

}