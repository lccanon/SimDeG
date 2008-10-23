package simdeg.reputation.simulation;

import static simdeg.reputation.simulation.Result.ResultType.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

import simdeg.util.RandomManager;
import simdeg.util.Switcher;

/**
 * Workers are agents that treat more or less successfully the Jobs they are
 * assigned to.
 * They are characterized by their probability of failure and their collusion
 * behavior.
 */
class Worker implements simdeg.reputation.Worker {

    /** Failure-related variable */
    private Switcher<Double> reliability = null;

    /** Bugging collusion behavior */
    private Switcher<Set<CollusionGroup>> buggingGroups = null;


    /** History of the Worker */
    private Map<Job,Result> previousResults = new HashMap<Job,Result>();

    /** Current status of the Worker */
    private Job currentJob = null;

    protected Worker(String name, Switcher<Double> reliability,
            Switcher<Set<CollusionGroup>> buggingGroups) {
        this.name = name;
        setReliability(reliability);
        setBuggingGroups(buggingGroups);
        checkProbabilities();
    }

    private void setReliability(Switcher<Double> reliability) {
        assert(reliability.get(0.0d) >= 0.0d && reliability.get(0.0d) <= 1.0d)
            : "Not a proper probability: " + reliability;
        this.reliability = reliability;
    }

    private void setBuggingGroups(Switcher<Set<CollusionGroup>> buggingGroups) {
        this.buggingGroups = buggingGroups;
        for (Set<CollusionGroup> set : buggingGroups.getAll())
            for (CollusionGroup buggingGroup : set)
                buggingGroup.addWorker(this);
    }

    /**
     * Return the previous Result given for a Job
     */
    protected Result getPreviousResult(Job job) {
        return previousResults.get(job);
    }

    public String toString() {
        return getName();
    }

    /**
     * Checks internaly if the specified probabilities are achievables.
     */
    private void checkProbabilities() {
        double probabilityCollusion = 0.0d;
        for (CollusionGroup buggingGroup : buggingGroups.get(0.0d))
            probabilityCollusion = (1.0d - probabilityCollusion)
                * buggingGroup.getProbability() + probabilityCollusion;
        if (probabilityCollusion > reliability.get(0.0d))
            throw new IllegalArgumentException("Collusion probability are too high");
    }

    /**
     * Finds if a worker has already decided to bug.
     */
    private Set<CollusionGroup> whichBugging(Job job, int step) {
        Set<CollusionGroup> result = new HashSet<CollusionGroup>();

group:  for (CollusionGroup buggingGroup : buggingGroups.get(step)) {
            for (Worker worker : buggingGroup.getWorkers()) {
                /* If worker is different */
                if (worker == this)
                    continue;
                /* Get previous result of worker for this job */
                Result prevResult = worker.getPreviousResult(job);
                if (prevResult == null)
                    continue;
                /* It's already decided by a worker in the same bugging group */
                if (prevResult.getColludingGroup().contains(buggingGroup))
                    result.add(buggingGroup);
                continue group;
            }
            /* Make the decision according to the bugging probability */
            if (RandomManager.getRandom("").nextDouble()
                    < buggingGroup.getProbability())
                result.add(buggingGroup);
        }

        return result;
    }

    /**
     * Decides the result result based on the specified probability.
     */
    private Result computeAnswer(Job job, int step) {

        /* Buggy case */
        Set<CollusionGroup> groupsBugging = whichBugging(job, step);
        if (!groupsBugging.isEmpty())
            return new Result(BUG, groupsBugging);

        /* Compute left probability */
        double leftProbability = 1.0d;
        for (CollusionGroup buggingGroup : buggingGroups.get(step))
            leftProbability *= buggingGroup.getProbability();

        /* Reliability case */
        if (RandomManager.getRandom("").nextDouble() * leftProbability
                < 1.0d - reliability.get(step))
            return new Result(FAILED);

        return new Result(CORRECT);
    }

    protected void submitJob(Job job) {
        logger.fine("Worker " + this + " is assigned to job " + job);
        currentJob = job;
    }

    protected Result getResult(int step) {
        final Result result = computeAnswer(currentJob, step);
        previousResults.put(currentJob, result);
        logger.fine("Worker " + this + " has computed job " + currentJob
                + " and found result " + result + " at step " + step);
        currentJob = null;
        return result;
    }

    private final String name;

    protected String getName() {
        return name;
    }

    private static int count = 0;
    private final int hash = count++;
    /**
     * Used for HashSet.
     */
    public final int hashCode() {
        return hash;
    }

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Worker.class.getName());

}
