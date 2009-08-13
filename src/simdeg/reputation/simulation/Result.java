package simdeg.reputation.simulation;

/**
 * Specify the kind of possible results for the simulator and the scheduling
 * algorithms.
 */
class Result implements simdeg.reputation.Result {

    private long id; 

    /**
     * Used during workload creation.
     */
    protected Result(long id) {
        this.id = id;
    }

    /**
     * A Result is unique to a server and its index on this last. Many duplication
     * can exist (for each worker).
     */
    public boolean equals(Object aResult) {
        return this == aResult
            || (aResult instanceof Result && id == aResult.hashCode());
    }

    /**
     * Used for HashSet.
     */
    public int hashCode() {
        return (int)id;
    }

    public String toString() {
        return "result" + id;
    }

}
