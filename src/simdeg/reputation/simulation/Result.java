package simdeg.reputation.simulation;

import java.util.Set;
import java.util.HashSet;

/**
 * Specify the kind of possible answers for the simulator and the scheduling
 * algorithms.
 */
class Result implements simdeg.reputation.Result {

    enum ResultType {ATTACK, FAILED, BUG, CORRECT, LAST};

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

    public boolean equals(Object aAnswer) {
        if (this == aAnswer)
            return true;
        if (!(aAnswer instanceof Result))
            return false;
        Result result = (Result)aAnswer;
        if (type == ResultType.FAILED || result.getType() == ResultType.FAILED)
            return false;
        if (type == result.getType() && colludingGroup.equals(getColludingGroup()))
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
