package simdeg.reputation.simulation;

/**
 * Workers are agents that treat more or less successfully the Jobs they are
 * assigned to.
 * They are characterized by their probability of failure and their collusion
 * behavior.
 */
class Worker implements simdeg.reputation.Worker {

    private long id;

    protected Worker(long id) {
        this.id = id;
    }

    public boolean equals(Object aWorker) {
        return this == aWorker
                || (aWorker instanceof Worker && id == aWorker.hashCode());
    }

    /**
     * Used for HashSet.
     */
    public final int hashCode() {
        return (int)id;
    }

    public String toString() {
        return "worker" + id;
    }

}
