package simdeg.reputation;

/**
 * Characterise the kind of result for scheduling algorithms.
 */
public interface Result {

    /**
     * Class implementing this interface should override this method in order
     * to test corretly the equality between 2 answers.
     */
    public boolean equals(Object aAnswer);

    /**
     * Used for HashSet.
     */
    public int hashCode();

}
