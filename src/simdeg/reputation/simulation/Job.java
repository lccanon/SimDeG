package simdeg.reputation.simulation;

/**
 * A Job is created and is submitted to workers whith duplication.
 */
class Job implements simdeg.reputation.Job {

    private long id;

    /**
     * Used during workload creation.
     */
    protected Job(long id) {
        this.id = id;
    }

    /**
     * A Job is unique to a server and its index on this last. Many duplication
     * can exist (for each worker).
     */
    public boolean equals(Object aJob) {
        return this == aJob
            || (aJob instanceof Job && id == aJob.hashCode());
    }

    /**
     * Used for HashSet.
     */
    public int hashCode() {
        return (int)id;
    }

    public String toString() {
        return "job" + id;
    }

}
