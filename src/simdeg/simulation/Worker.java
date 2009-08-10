package simdeg.simulation;

import static simdeg.simulation.Result.ResultType.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

import simdeg.util.RandomManager;
import simdeg.util.Switcher;
import simdeg.util.OutOfRangeException;

import simgrid.msg.Msg;
import simgrid.msg.Host;
import simgrid.msg.Task;
import simgrid.msg.JniException;
import simgrid.msg.NativeException;

/**
 * Workers are agents that treat more or less successfully the Jobs they are
 * assigned to.
 * They are characterized by their probability of failure and their collusion
 * behavior.
 */
class Worker extends simgrid.msg.Process implements simdeg.reputation.Worker {

    /** Failure-related variable */
    private Switcher<Double> reliability = null;

    /** Bugging collusion behavior */
    private Switcher<Set<CollusionGroup>> buggingGroups = null;

    /** Attacking collusion behavior */
    private Switcher<CollusionGroup> attackingGroup = null;


    /** History of the Worker */
    private Set<Job> previousJobs = new HashSet<Job>();

    /** Current status of the Worker */
    private Job currentJob = null;

    /** For idle time measure (temp) */
    private double previousTime = 0.0d;

    /** For idle time measure (total) */
    private double idleTime = 0.0d;


    /** Guarantees the reliability probability */
    private double shortTermFail;

    /** An estimator of the probability to be used */
    private double shortTermFailProba;

    /** Guarantees the bugging behavior */
    private Map<CollusionGroup,Double> shortTermBugs
        = new HashMap<CollusionGroup,Double>();

    /** An estimator of the probability to be used */
    private Map<CollusionGroup,Double> shortTermBugsProba
        = new HashMap<CollusionGroup,Double>();

    private static final double SHORT_TERM_STD_DEV = 0.1d;

    private static final double SHORT_TERM_WEIGHT
        = 8.0d * Math.pow(SHORT_TERM_STD_DEV, 2.0d)
        / (4.0d * Math.pow(SHORT_TERM_STD_DEV, 2.0d) + 1.0d);


    /**
     * Send ending job to specified workers
     */
    static void terminateAllWorkers(Set<? extends Worker> workers)
            throws JniException, NativeException {
        for (Worker worker : workers) {
            Job terminate = new Job(new Result(LAST));
            worker.taskSend(terminate);
        }
        simgrid.msg.Process.waitFor(0.0d);
    }

    Worker(Host host, String name, Switcher<Double> reliability,
            Switcher<Set<CollusionGroup>> buggingGroups,
            Switcher<CollusionGroup> attackingGroup)
                throws JniException, NativeException {
        super(host, name, null);
        setName(name);
        setReliability(reliability);
        setBuggingGroups(buggingGroups);
        setAttackingGroup(attackingGroup);
        previousTime = Msg.getClock();
        checkProbabilities();
    }

    private void setReliability(Switcher<Double> reliability) {
        assert(reliability.get(0.0d) >= 0.0d && reliability.get(0.0d) <= 1.0d)
            : "Not a proper probability: " + reliability;
        this.reliability = reliability;
        shortTermFail = 1.0d - reliability.get(Msg.getClock());
        shortTermFailProba = 1.0d - reliability.get(Msg.getClock());
    }

    private void setBuggingGroups(Switcher<Set<CollusionGroup>> buggingGroups) {
        this.buggingGroups = buggingGroups;
        for (Set<CollusionGroup> set : buggingGroups.getAll())
            for (CollusionGroup buggingGroup : set) {
                buggingGroup.addWorker(this);
                shortTermBugs.put(buggingGroup,
                        buggingGroup.getProbability());
                shortTermBugsProba.put(buggingGroup,
                        buggingGroup.getProbability());
        }
    }

    private void setAttackingGroup(Switcher<CollusionGroup> attackingGroup) {
        this.attackingGroup = attackingGroup;
        for (CollusionGroup attacking : attackingGroup.getAll())
            if (attacking != null)
                attacking.addWorker(this);
    }


    double getIdleTime() {
        return this.idleTime;
    }

    /**
     * Return the previous Result given for a Job
     */
    Result getPreviousResult(Job job) {
        if (previousJobs.contains(job))
            for (Job previousJob : previousJobs)
                if (previousJob.equals(job))
                    return previousJob.getResult();
        return null;
    }

    /**
     * Return the current processing Job
     */
    Job getCurrentJob() {
        return currentJob;
    }

    public String toString() {
        return getName();
    }

    /**
     * Checks internaly if the specified probabilities are achievables.
     */
    private void checkProbabilities() {
        double probabilityTotal = 1.0d - reliability.get(Msg.getClock());
        if (attackingGroup.get(Msg.getClock()) != null)
            probabilityTotal
                += attackingGroup.get(Msg.getClock()).getProbability();
        double maxProbaCollusion = 0.0d;
        for (CollusionGroup buggingGroup : buggingGroups.get(Msg.getClock()))
            maxProbaCollusion = Math.max(maxProbaCollusion,
                    buggingGroup.getProbability());
        probabilityTotal += maxProbaCollusion;
        if (probabilityTotal < 0.0d || probabilityTotal > 1.0d)
            throw new OutOfRangeException(probabilityTotal, 0.0d, 1.0d);
    }

    /**
     * Decides to attack or not.
     */
    private boolean isAttacking(Job job) throws Exception {

        CollusionGroup attacking = attackingGroup.get(Msg.getClock());

        /* Optimization for non-attackers */
        if (attacking == null)
            return false;

        /* Check if another Worker already gave an anwer */
        for (Worker worker : attacking.getWorkers()) {
            /* If worker is different */
            if (worker == this)
                continue;
            /* Get previous result of worker for this job */
            Result result = worker.getPreviousResult(job);
            if (result == null || !result.isSyncSuccess())
                continue;
            /* It's already decided by a worker in the same bugging group */
            return result.getColludingGroup().contains(attacking);
        }

        /* Collusion if we are able to synchronize with at least another */
        for (Worker worker : attacking.getWorkers())
            if (job.equals(worker.getCurrentJob()) && worker != this)
                return attacking.isColluding();

        /* Missing the sync */
        throw new Exception();
    }

    /**
     * Finds if a worker has already decided to bug.
     */
    private Set<CollusionGroup> whichBugging(Job job) {
        Set<CollusionGroup> result = new HashSet<CollusionGroup>();

group:  for (CollusionGroup buggingGroup : buggingGroups.get(Msg.getClock())) {
            for (Worker worker : buggingGroup.getWorkers()) {
                /* If worker is different */
                if (worker == this)
                    continue;
                /* Get previous result of worker for this job */
                Result prevResult = worker.getPreviousResult(job);
                if (prevResult == null || prevResult.getType() == ATTACK
                        || prevResult.getType() == FAILED)
                    continue;
                /* It's already decided by a worker in the same bugging group */
                if (prevResult.getColludingGroup().contains(buggingGroup)) {
                    shortTermBugs.put(buggingGroup,
                            shortTermBugs.get(buggingGroup)
                            + SHORT_TERM_WEIGHT);
                    result.add(buggingGroup);
                }
                continue group;
            }
            /* Make the decision according to the bugging probability */
            if (RandomManager.getRandom("").nextDouble()
                    < shortTermBugsProba.get(buggingGroup)) {
                shortTermBugs.put(buggingGroup,
                        shortTermBugs.get(buggingGroup)
                        + SHORT_TERM_WEIGHT);
                result.add(buggingGroup);
            }
            shortTermBugsProba.put(buggingGroup,
                    shortTermBugsProba.get(buggingGroup)
                    * (1.0d - SHORT_TERM_WEIGHT));
            if (shortTermBugs.get(buggingGroup)
                    < buggingGroup.getProbability())
                shortTermBugsProba.put(buggingGroup,
                        shortTermBugsProba.get(buggingGroup)
                        + SHORT_TERM_WEIGHT);
        }

        return result;
    }

    /**
     * Decides the result result based on the specified probability.
     */
    private Result computeResult(Job job) {
        logger.finest("Failure (probability, measured, effective): "
                + (1.0d - reliability.get(Msg.getClock())) + ", "
                + shortTermFail + ", " + shortTermFailProba
                + " for worker " + this);
        for (CollusionGroup buggingGroup : buggingGroups.get(Msg.getClock()))
            logger.finest("Bugging (probability, measured, effective): "
                    + buggingGroup.getProbability() + ", "
                    + shortTermBugs.get(buggingGroup) + ", "
                    + shortTermBugsProba.get(buggingGroup)
                    + " for worker " + this);

        /* One new measures */
        shortTermFail *= (1.0d - SHORT_TERM_WEIGHT);
        for (CollusionGroup buggingGroup : buggingGroups.get(Msg.getClock()))
            shortTermBugs.put(buggingGroup,
                    shortTermBugs.get(buggingGroup)
                    * (1.0d - SHORT_TERM_WEIGHT));

        /* Attack case */
        boolean missSync = false;
        try {
            if (isAttacking(job))
                return new Result(ATTACK, attackingGroup.get(Msg.getClock()));
        } catch (Exception e) {
            missSync = true;
        }
        
        /* Reliability case */
        final boolean failure = RandomManager.getRandom("").nextDouble()
            < shortTermFailProba;
        if (failure)
            shortTermFail += SHORT_TERM_WEIGHT;
        shortTermFailProba *= (1.0d - SHORT_TERM_WEIGHT);
        if (shortTermFail < 1.0d - reliability.get(Msg.getClock()))
            shortTermFailProba += SHORT_TERM_WEIGHT;
        if (failure)
            return new Result(FAILED, missSync);

        /* Buggy case */
        Set<CollusionGroup> groupsBugging = whichBugging(job);
        if (!groupsBugging.isEmpty())
            return new Result(BUG, groupsBugging, missSync);

        return new Result(CORRECT, missSync);
    }

    public void main(String[] args) throws JniException, NativeException {

        logger.config("Worker " + this + " is starting");

        while (true) {

            /* Get a job to compute */
            logger.fine("Worker " + this + " is waiting");
            Job job = (Job)Task.receive();
            logger.fine("Worker " + this + " has finished waiting");

            /* Update idle time spend to wait for a job */
            idleTime += Msg.getClock() - previousTime;

            /* If it is the last, it quits */
            if (job.getResult() != null && job.getResult().isLast())
                break;

            /* Execution for the correct amount of time */
            currentJob = job;
            job.execute();

            synchronized(job.getServer()) {
                /* Set the asnwer to the appropriate flag */
                job.setResult(computeResult(job));

                /* Store the job in previous job for collusion purpose */
                previousJobs.add(job);
                currentJob = null;
            }

            /* Update the date at which the previous job was finished */
            previousTime = Msg.getClock();

            /* Send back the finished job */
            job.getServer().taskSend(job);

            logger.fine("Worker " + this + " has computed job "
                + job + " and found result " + job.getResult()
                + " at time " + Msg.getClock());

        }

        logger.config("Worker " + this + " has finished");

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
