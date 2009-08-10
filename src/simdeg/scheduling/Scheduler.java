package simdeg.scheduling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class Scheduler<J extends Job, R extends Result> {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Scheduler.class
			.getName());

	/* Components of the scheduler */
	private ResourcesGrouper resourcesGrouper = null;

	private ResultCertificator resultCertificator = null;

	private ReputationSystem reputationSystem = null;

	/* Basic structures for queuing scheduled and non-scheduled jobs */

	/** Jobs available for being added to workers waiting queues */
	private Queue<J> availableJobs = new ArrayDeque<J>();

	/** Jobs queue for each worker */
	private Map<Worker, Queue<J>> jobsQueue = new HashMap<Worker, Queue<J>>();

	/** List of workers assigned to each job */
	private Map<J, Set<Worker>> workersJob = new HashMap<J, Set<Worker>>();

	/* Structure for storing incomplete result sets */

	/** List of workers assigned to each job (for progression status) */
	private Map<J, List<Worker>> resultingWorkers = new HashMap<J, List<Worker>>();

	/** List of results for each job (for progression status) */
	private Map<J, List<R>> resultingResults = new HashMap<J, List<R>>();

	/** Jobs that have been postponed */
	private Set<J> postponedJobs = new HashSet<J>();

	/** Certified results for each job */
	private Map<J, R> resultsFound = new HashMap<J, R>();

	/**
	 * Constructs a Scheduler with the given components.
	 */
	public Scheduler(ResourcesGrouper resourcesGrouper,
			ResultCertificator resultCertificator,
			ReputationSystem reputationSystem) {
		this.resourcesGrouper = resourcesGrouper;
		this.resultCertificator = resultCertificator;
		this.reputationSystem = reputationSystem;
		this.resourcesGrouper.setReputationSystem(reputationSystem);
		this.resultCertificator.setReputationSystem(reputationSystem);
		logger.info("Scheduler created with " + resourcesGrouper + ", "
				+ resultCertificator + ", and " + reputationSystem);
	}

	/**
	 * Adds participating workers.
	 */
	public void addAllWorkers(Set<? extends Worker> workers) {
		for (Worker worker : workers)
			jobsQueue.put(worker, new ArrayDeque<J>());
		this.resourcesGrouper.addAllWorkers(workers);
		this.reputationSystem.addAllWorkers(workers);
		logger.info("Adding " + workers.size() + " workers");
	}

	/**
	 * Removes participating workers.
	 */
	public void removeAllWorkers(Set<? extends Worker> workers) {
		for (Worker worker : workers)
			jobsQueue.remove(worker);
		this.resourcesGrouper.removeAllWorkers(workers);
		this.reputationSystem.removeAllWorkers(workers);
	}

	/**
	 * Adds a single workunit to be treated.
	 */
	public void addJob(J job) {
		this.availableJobs.add(job);
		workersJob.put(job, new HashSet<Worker>());
		resultingWorkers.put(job, new ArrayList<Worker>());
		resultingResults.put(job, new ArrayList<R>());
	}

	/**
	 * Adds a workload to be treated.
	 */
	public void addAllJobs(Set<J> jobs) {
		for (J job : jobs)
			addJob(job);
	}

	/**
	 * Puts the given job in the queues of every specified workers.
	 */
	private void putJobsInQueue(J job, Set<Worker> workers) {
		for (Worker worker : workers) {
			jobsQueue.get(worker).offer(job);
			workersJob.get(job).add(worker);
		}
	}

	/**
	 * Requests a job for a given worker. This method justifies the use of
	 * generics.
	 */
	public J requestJob(Worker worker) {
		if (jobsQueue.get(worker).isEmpty()) {
			if (availableJobs.isEmpty())
				return null;
			J job = availableJobs.poll();
			Set<Worker> currentWorkers = resourcesGrouper.getGroup(worker);
			putJobsInQueue(job, currentWorkers);
			logger.fine("Creation of a group for job " + job + ": "
					+ currentWorkers);
		}
		return jobsQueue.get(worker).poll();
	}

	/**
	 * Completes last update and cleans useless data concerning a job.
	 */
	private void completeJob(J job, R result) {
		/* Last updates */
		reputationSystem.setCertifiedResult(job, result);
		/* Cleaning */
		resultingWorkers.remove(job);
		resultingResults.remove(job);
		workersJob.remove(job);
	}

	/**
	 * Tries to get the best result for the given job.
	 */
	private void findJobResult(J job) {
		List<Worker> workersList = resultingWorkers.get(job);
		List<R> resultsList = resultingResults.get(job);
		try {
			R result = resultCertificator.selectBestResult(workersList,
					resultsList);
			completeJob(job, result);
			resultsFound.put(job, result);
		} catch (JobPostponedException e) {
			postponedJobs.add(job);
		} catch (ResultCertificationException e) {
			/* Try to get an extension of the current worker group */
			Set<Worker> currentWorkers = resourcesGrouper
					.getGroupExtension(new HashSet<Worker>(workersList));
			logger.fine("Creation of a group extension for job " + job + ": "
					+ currentWorkers);
			if (currentWorkers != null)
				putJobsInQueue(job, currentWorkers);
			else {
				/* Choose the less worse result */
				logger.fine("The less worse result will now be selected in "
						+ "a group of size " + workersList.size());
				R result = resultCertificator.selectLessWorseResult(
						workersList, resultsList);
				completeJob(job, result);
				resultsFound.put(job, result);
			}
		}
	}

	/**
	 * Gives the result of a worker for a given job.
	 */
	public void submitWorkerResult(Worker worker, J job, R result) {
		reputationSystem.setWorkerResult(worker, job, result);

		/* Retreive the list of previous computed result for the current job */
		List<Worker> workersList = resultingWorkers.get(job);
		workersList.add(worker);
		resultingResults.get(job).add(result);

		/* Try to get result for postponed jobs */
		for (J postponedJob : new HashSet<J>(postponedJobs)) {
			postponedJobs.remove(postponedJob);
			findJobResult(postponedJob);
		}

		/* Try to get the best result if all jobs have been computed */
		if (workersList.size() == workersJob.get(job).size()) {
			for (J initialJob : resultingWorkers.keySet())
				if (job.equals(initialJob))
					job = initialJob;
			findJobResult(job);
		}
	}

	/**
	 * Returns the selected result if enough informations are available. This
	 * method, along with the next, are the main reasons of the use of generics
	 * here.
	 */
	public Map<J, R> getCertifiedResults() {
		Map<J, R> result = new HashMap<J, R>(resultsFound);
		resultsFound.clear();
		return result;
	}

}
