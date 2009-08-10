package simdeg.simulation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.scheduling.Scheduler;
import simdeg.util.RandomManager;
import simgrid.msg.Host;
import simgrid.msg.JniException;
import simgrid.msg.Msg;
import simgrid.msg.NativeException;
import simgrid.msg.Task;

/**
 * The Server is responsible of the workload creation, the submission of the
 * Jobs to the Workers and the reception of their results.
 */
class Server extends simgrid.msg.Process {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Server.class.getName());

    private final Set<Worker> workers;

    private final Scheduler<Job,Result> scheduler;

    private final int jobsNumber;

    private Set<Worker> waitingWorkers = new HashSet<Worker>();

    private Set<Job> begunJobs = new HashSet<Job>();

    private Set<Job> submittedJobs = new HashSet<Job>();

    Server(Set<Worker> workers, Scheduler<Job,Result> scheduler,
            int jobsNumber, double jobsCostMean, double jobsCostStdDev,
            double jobsArrivalRate) throws JniException, NativeException {
        super(Host.all()[0], "", null);
        this.workers = workers;
        this.scheduler = scheduler;
        this.jobsNumber = jobsNumber;
        this.waitingWorkers.addAll(workers);
        /* Add all current workers to the scheduler list */
        scheduler.addAllWorkers(workers);
        /* Start the workload generation */
        new WorkloadGenerator(this, 3*jobsNumber, jobsCostMean,
        		jobsCostStdDev, jobsArrivalRate);
    }

    /**
     * Request a new job to be traited for each waiting job.
     */
    private void sendWaitingJobs()
        throws JniException, NativeException {

        Collection<Worker> waitingCopy;
        synchronized(waitingWorkers) {
            waitingCopy = new HashSet<Worker>(waitingWorkers);
        }

        for (Worker worker : waitingCopy) {
            synchronized(waitingWorkers) {
                if (waitingWorkers.contains(worker))
                    waitingWorkers.remove(worker);
                else
                    continue;
            }

            Job newJob = scheduler.requestJob(worker);

            /* If valid, submit job with appropriate result */
            if (newJob != null) {
                Job workerJob = newJob.duplicate(worker);
                worker.taskSend(workerJob);

                logger.fine("Job " + workerJob + " is sent to worker "
                        + worker + " at time " + Msg.getClock());

                if (!begunJobs.contains(newJob)) {
                    begunJobs.add(newJob);
                    Evaluator.submitJob(newJob);
                }
            } else {
                synchronized(waitingWorkers) {
                    waitingWorkers.add(worker);
                }
            }

        }
    }

    /**
     * Submits to the Evaluator each result obtained.
     */
    private void submitCertifiedResults() {
        final Map<Job,Result> results = scheduler.getCertifiedResults();
        for (Job job : results.keySet()) {
            logger.fine("Job " + job + " is accepted"
                    + " with final result " + results.get(job));

            /* Duplicate the object for the Evaluator */
            Job selectedJob = job.duplicate(null);
            selectedJob.setResult(results.get(job));

            /* Compute the overhead for this job and submit it */
            int overhead = 0;
            for (Worker worker : workers)
                if (worker.getPreviousResult(selectedJob) != null)
                    overhead++;
            if (!isWorkloadCompleted())
                Evaluator.submitResult(selectedJob, overhead);

            /* Update for the stopping criterion */
            submittedJobs.add(selectedJob);
        }
    }

    private boolean isWorkloadCompleted() {
        return submittedJobs.size() == jobsNumber;
    }

    public void main(String[] args) throws JniException, NativeException {

        logger.info("Server is ready to receive results");

        do {
            /* Ask new jobs to the scheduler for currently waiting workers */
            sendWaitingJobs();

            /* Receive the completed job of a worker and submit it */
            Job job = (Job)Task.receive();
            scheduler.submitWorkerResult(job.getWorker(), job, job.getResult());

            /* Add the current worker to the waiting queue */
            waitingWorkers.add(job.getWorker());

            /* Retrieve any decided results */
            submitCertifiedResults();

        } while (!isWorkloadCompleted());

        logger.info("End of workload submission");
        Evaluator.end();

        /* End of computation */
        for (int i=0; i<workers.size() - waitingWorkers.size(); i++)
            Task.receive();
        Worker.terminateAllWorkers(workers);

    }

    /**
     * Inner class allowing to generate workload by waiting new job during the
     * correct amount of time while the server is waiting for results from
     * clients
     */
    private class WorkloadGenerator extends simgrid.msg.Process {

        private final Server server;

        private final int jobsNumber;
        private final double jobsCostMean;
        private final double jobsCostStdDev;
        private final double jobsArrivalRate;

        private int workloadSubmitted;

        WorkloadGenerator(Server server, int jobsNumber, double jobsCostMean,
                double jobsCostStdDev, double jobsArrivalRate)
            throws JniException, NativeException {
                super(Host.all()[0], "", null);
                this.server = server;
                this.jobsNumber = jobsNumber;
                this.jobsCostMean = jobsCostMean;
                this.jobsCostStdDev = jobsCostStdDev;
                this.jobsArrivalRate = jobsArrivalRate;
            }

        private double getJobCost() {
            if (jobsCostStdDev <= jobsCostMean / 1000.d)
                return jobsCostMean;
            final double beta = (jobsCostStdDev * jobsCostStdDev)
                / jobsCostMean;
            final double gamma = (jobsCostMean / jobsCostStdDev)
                * (jobsCostMean / jobsCostStdDev);
            return RandomManager.getPsRandom("workload")
                .nextGamma(0.0d, beta, gamma);
        }

        public void main(String[] args) throws JniException, NativeException {

            logger.info("Workload generation is starting");

            while (!isWorkloadCompleted() && workloadSubmitted != jobsNumber) {

                /* Create jobs according to its repartition cost */
                final double cost = getJobCost();
                Job job = new Job("job" + workloadSubmitted, cost, this.server,
                        Msg.getClock());
                workloadSubmitted++;

                /* Add current workload */
                scheduler.addJob(job);

                logger.fine("Job " + job + " is created at time "
                        + Msg.getClock() + " with cost " + cost);

                /* Ask new jobs for waiting workers to the scheduler */
                sendWaitingJobs();

                /* Wait between two events in a Poissonian process */
                final double waitTime = RandomManager.getRandom("workload")
                    .nextExponential(jobsArrivalRate);
                simgrid.msg.Process.waitFor(waitTime);

            }

            logger.info("End of workload generation");

        }

    }

}
