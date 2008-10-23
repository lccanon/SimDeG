package simdeg.reputation.simulation;

import java.util.logging.Logger;

/**
 * A Job is created and is submitted to workers whith duplication.
 */
class Job implements simdeg.reputation.Job<Result> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Job.class.getName());

    private final String name;

    /**
     * Used during workload creation.
     */
    protected Job(String name) {
        this.name = name;
        this.hash = name.hashCode();
    }

    protected String getName() {
        return name;
    }

    /**
     * Used when the algorithm duplicates a Job and associates it to a Worker.
     */
    protected Job duplicate() {
        return new Job(getName());
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
        if (hash == job.hashCode())
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

    public String toString() {
        return getName();
    }

}
