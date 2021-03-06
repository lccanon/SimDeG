package simdeg.reputation;

/**
 * A Job is defined by an result resulting from some instructions 
 * executed on a given worker.
 */
public interface Job {

    /**
     * Two jobs are considered equals if they model the same workload.
     */
    public boolean equals(Object aJob);

    /**
     * Used for HashSet.
     */
    public int hashCode();

}
