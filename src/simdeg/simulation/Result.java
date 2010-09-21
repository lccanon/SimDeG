package simdeg.simulation;

import java.util.HashMap;
import java.util.Map;

import simdeg.util.HashableObject;

/**
 * Specify the kind of possible results for the simulator and the scheduling
 * algorithms.
 */
class Result extends HashableObject implements simdeg.reputation.Result {

	/** All the correct result are the same */
	private final static Result correct = new Result();

	/** The results for each group of collusion */
	private static Map<CollusionGroup, Result> colludedResults = new HashMap<CollusionGroup, Result>();

	/** The results for each group of inter-collusion */
	private static Map<InterCollusionGroup, Result> interColludedResults = new HashMap<InterCollusionGroup, Result>();

	protected static Result getCorrectResult() {
		return correct;
	}

	/**
	 * Returns a new result each time a worker failed. Each new result are
	 * distincts.
	 */
	protected static Result getFailedResult() {
		return new Result();
	}

	protected static Result getColludedResult(CollusionGroup collusionGroup) {
		if (!colludedResults.containsKey(collusionGroup))
			colludedResults.put(collusionGroup, new Result());
		return colludedResults.get(collusionGroup);
	}

	protected static Result getInterColludedResult(
			InterCollusionGroup interCollusionGroup) {
		if (!interColludedResults.containsKey(interCollusionGroup))
			interColludedResults.put(interCollusionGroup, new Result());
		return interColludedResults.get(interCollusionGroup);
	}

	public boolean equals(Object aResult) {
		return this == aResult;
	}

	public String toString() {
		if (this == correct)
			return "T";
		if (colludedResults.containsValue(this))
			return "C";
		if (interColludedResults.containsValue(this))
			return "I";
		return "F";
	}

}