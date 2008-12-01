package simdeg.simulation;

import java.util.logging.Logger;
import java.util.logging.Level;

import simdeg.util.OutOfRangeException;

import simgrid.msg.Task;
import simgrid.msg.MsgException;
import simgrid.msg.JniException;
import simgrid.msg.NativeException;

/**
 * A Job is defined by a Task with a given cost to be computed on a given Worker
 * and given a specific result.
 * A Job is created on the Server and is submitted to the Algorithm which
 * duplicates it correctly and associate to each a worker. Then, the initial job
 * is submitted to the Simulator with the seleted result.
 */
class Job extends Task implements simdeg.reputation.Job<Result> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Job.class.getName());

    private Server server = null;

    private Worker worker = null;

    private Result result = null;

    private double submissionDate = 0.0d;

    /**
     * Used for communicating with workers through result (indicating the
     * last one).
     */
    Job(Result result) throws JniException, NativeException {
        this("", 0.0d, null, result);
    }

    /**
     * Used during workload creation.
     */
    Job(String name, double computeDuration, Server server,
            double submissionDate) throws JniException, NativeException {
        this(name, computeDuration, server, submissionDate, null);
    }

    /**
     * Used during duplication.
     */
    Job(String name, double computeDuration, Server server,
            double submissionDate, Worker worker)
                throws JniException, NativeException {
        this(name, computeDuration, server, null);
        /* Test for admissibility of parameter */
        if (submissionDate < 0.0d)
            throw new OutOfRangeException(submissionDate, 0.0d, Double.MAX_VALUE);

        this.submissionDate = submissionDate;
        this.worker = worker;
    }

    protected Job() throws JniException, NativeException {
        this.hash = 0;
    }

    protected Job(String name, double computeDuration, Server server,
            Result result) throws JniException, NativeException {
        super(name, computeDuration, 0.0d);
        this.server = server;
        this.result = result;
        if (server != null)
            this.hash = name.hashCode() + server.hashCode();
        else
            this.hash = name.hashCode();
    }

    Server getServer() {
        return this.server;
    }

    public final Worker getWorker() {
        return this.worker;
    }

    public Result getResult() {
        return this.result;
    }

    void setResult(Result result) {
        this.result = result;
    }

    double getSubmissionDate() {
        return this.submissionDate;
    }

    /**
     * A Job is unique to a server and its index on this last. Many duplication
     * can exist (for each worker).
     */
    public boolean equals(Object aJob) {
        if (this == aJob)
            return true;
        if (!(aJob instanceof Job))
            return false;
        Job job = (Job)aJob;
        // XXX not deep check
        if (hash == job.hashCode() && server == job.getServer())
            return true;
        return false;
    }

    public final int hash;
    /**
     * Used for HashSet.
     */
    public int hashCode() {
        return hash;
    }

    /**
     * Used when the algorithm duplicates a Job and associates it to a Worker.
     */
    Job duplicate(Worker worker) {
        try {
            return new Job(getName(), getComputeDuration(), getServer(),
                    getSubmissionDate(), worker);
        } catch (MsgException e) {
            logger.log(Level.SEVERE, "SimGrid exception", e);
            System.exit(1);
        }
        return null;
    }

    public String toString() {
        try {
            return getName();
        } catch (JniException e) {
            logger.log(Level.SEVERE, "Jni exception", e);
            System.exit(1);
        }
        return null;
    }

}
