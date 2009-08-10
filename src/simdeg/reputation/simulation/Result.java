package simdeg.reputation.simulation;

import java.util.Set;
import java.util.HashSet;

/**
 * Specify the kind of possible results for the simulator and the scheduling
 * algorithms.
 */
class Result implements simdeg.reputation.Result {

    enum ResultType {BUG, FAILED, CORRECT};

    private ResultType type;

    private Set<CollusionGroup> colludingGroup = new HashSet<CollusionGroup>();

    protected Result(ResultType type) {
        this.type = type;
    }

    protected Result(ResultType type, CollusionGroup colludingGroup) {
        this(type);
        this.colludingGroup.add(colludingGroup);
    }

    protected Result(ResultType type, Set<CollusionGroup> colludingGroup) {
        this(type);
        this.colludingGroup.addAll(colludingGroup);
    }

    protected boolean isCorrect() {
        return type == ResultType.CORRECT;
    }

    protected ResultType getType() {
        return type;
    }

    protected Set<CollusionGroup> getColludingGroup() {
        return colludingGroup;
    }

    public boolean equals(Object aResult) {
        if (this == aResult)
            return true;
        if (!(aResult instanceof Result))
            return false;
        Result result = (Result)aResult;
        if (type == ResultType.FAILED || result.getType() == ResultType.FAILED)
            return false;
        if (type == result.getType() && colludingGroup.equals(result.getColludingGroup()))
            return true;
        return false;
    }

    private static int count = 0;
    private final int hash = count++;
    /**
     * Used for HashSet.
     */
    public final int hashCode() {
        if (type == ResultType.FAILED)
            return hash;
        if (null == colludingGroup)
            return type.hashCode();
        return colludingGroup.hashCode();
    }

    public String toString() {
        return type + " " + colludingGroup;
    }

}
