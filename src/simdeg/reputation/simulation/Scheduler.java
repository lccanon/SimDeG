package simdeg.reputation.simulation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.reputation.ReputationSystem;
import simdeg.util.RandomManager;
import simdeg.util.Collections;

/**
 * Main component of the scheduling package. It provides a generic interface for
 * assembling scheduling component to form a policy.
 */
public class Scheduler {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Scheduler.class
			.getName());

	/** Component on which we focus */
	private ReputationSystem reputationSystem = null;

    /** Evaluator */
    private Evaluator evaluator = null;

	/* Basic structures for queuing scheduled and non-scheduled jobs */

	/** Jobs queue for each worker */
	private Map<Worker, Queue<Job>> jobsQueue = new HashMap<Worker, Queue<Job>>();

	/** Jobs currently computed by each worker */
    private Map<Worker, Job> currentJobs =  new HashMap<Worker, Job>();

	/** List of workers assigned to each job */
	private Map<Job, Set<Worker>> workersJob = new HashMap<Job, Set<Worker>>();

	/**
     * List of results for each job (for progression status). 
     * This structure stores incomplete result sets.
     */
	private Map<Job, List<Result>> resultingResults = new HashMap<Job, List<Result>>();

	/**
	 * Constructs a Scheduler with the given components.
	 */
	protected Scheduler(ReputationSystem reputationSystem, Evaluator evaluator) {
		this.reputationSystem = reputationSystem;
        this.evaluator = evaluator;
		logger.info("Scheduler created with " + reputationSystem);
	}

	/**
	 * Adds participating workers.
	 */
	protected void addAllWorkers(Set<? extends Worker> workers) {
		for (Worker worker : workers)
			jobsQueue.put(worker, new ArrayDeque<Job>());
		this.reputationSystem.addAllWorkers(workers);
		logger.info("Adding " + workers.size() + " workers");
	}

	/**
	 * Removes participating workers.
	 */
	protected void removeAllWorkers(Set<? extends Worker> workers) {
		for (Worker worker : workers)
			jobsQueue.remove(worker);
		this.reputationSystem.removeAllWorkers(workers);
	}

    /**
     * Specifies platform heterogeneity by associating a coefficient to each worker
     */
    private Map<Worker, Double>
        generateArrivalHeterogeneity(double resultArrivalHeterogeneity) {

        /* Distribute values */
        Map<Worker, Double> result = new HashMap<Worker, Double>();
        double totalWeight = 0.0d;
        for (Worker worker : jobsQueue.keySet()) {
            final double weight = RandomManager.getRandom("result").nextBeta(0.0d, jobsQueue.size(), 1.0d, resultArrivalHeterogeneity);
            result.put(worker, weight);
            totalWeight += weight;
        }

        /* Normalize */
        for (Worker worker : jobsQueue.keySet())
            result.put(worker, result.get(worker) / totalWeight);

        return result;
    }

    /**
     * Get current worker according to a weighted uniform method.
     */
    //TODO switch to a deterministic method
    private Worker getWorker(Map<Worker,Double> heterogeneity) {
        double total = RandomManager.getRandom("scheduling").nextDouble();
        for (Worker worker : jobsQueue.keySet()) {
            total -= heterogeneity.get(worker);
            if (total < 0.0d)
                return worker;
        }
        assert(false) : "One worker should have been selected";
        return null;
    }

    /**
     * Replaces the heuristic in charge of grouping resources.
     */
    private Set<Worker> getGroup(double resourcesGroupSizeMean,
            double resourcesGroupSizeStdDev) {
        final int size = (int)Math.round(RandomManager.getRandom("scheduling")
                .nextBeta(0.0d, jobsQueue.size(), resourcesGroupSizeMean,
                    resourcesGroupSizeStdDev));
        return Collections.getRandomSubGroup(size, jobsQueue.keySet(),
                RandomManager.getRandom("scheduling"));
    }

    private int jobIndex = 0;
	/**
	 * Requests a job for a given worker.
	 */
    private Job getJob(Worker worker, double resourcesGroupSizeMean,
            double resourcesGroupSizeStdDev) {
		if (jobsQueue.get(worker).isEmpty()) {
            /* Create a new job and find workers for it */
			Job job = new Job("job" + jobIndex++);
            Set<Worker> currentWorkers = getGroup(resourcesGroupSizeMean,
                    resourcesGroupSizeStdDev);

            /* Put the given job in the queues of every specified workers */
            for (Worker otherWorker : jobsQueue.keySet()) {
                jobsQueue.get(otherWorker).offer(job);
                workersJob.get(job).add(otherWorker);
            }

            /* Prepare structure */
            resultingResults.put(job, new ArrayList<Result>());

			logger.fine("Creation of a group for job " + job + ": "
					+ currentWorkers);
		}

        /* Get the next waiting job */
        Job job = jobsQueue.get(worker).poll();
        currentJobs.put(worker, job);
        return job;
	}

	/**
	 * Tries to get the best result for the given job.
	 */
	private Result getCertifiedResult(Job job, Evaluator evaluator) {
		List<Result> resultsList = resultingResults.get(job);
        Result result = null; // TODO error from reputationSytem & evaluator

        /* Cleaning */
        resultingResults.remove(job);
        workersJob.remove(job);

        return result;
	}

    /**
     * Main loop for simulating the main elements of a desktop grid from the
     * point of view of the reputation system.
     */
    protected void start(int stepsNumber, double resourcesGroupSizeMean,
            double resourcesGroupSizeStdDev, double resultArrivalHeterogeneity) {
        Map<Worker, Double> heterogeneity
            = generateArrivalHeterogeneity(resultArrivalHeterogeneity);

        /* Assign a job to each worker */
        for (Worker worker : jobsQueue.keySet())
            worker.submitJob(getJob(worker, resourcesGroupSizeMean,
                        resourcesGroupSizeStdDev));

        /* Main loop for getting results from workers and assigning them jobs */
        for (int step=0; step<stepsNumber; step++) {

            /* Notify the progress of the simulation */
            evaluator.setStep(step);

            /* Get a triple and inform the reputation system */
            Worker worker = getWorker(heterogeneity);
            Result result = worker.getResult(step);
            Job job = currentJobs.get(worker);
            reputationSystem.setWorkerResult(worker, job, result);

            /* In the case every worker has finished a job */
            resultingResults.get(job).add(result);
            if (resultingResults.get(job).size() == workersJob.get(job).size())
                reputationSystem.setCertifiedResult(job,
                        getCertifiedResult(job, evaluator));

            /* Assignment */
            worker.submitJob(getJob(worker, resourcesGroupSizeMean,
                        resourcesGroupSizeStdDev));

            logger.fine("Worker " + worker + " is selected at step " + step);

        }
        
        /* Final step notification */
        evaluator.setStep(stepsNumber);
    }

}