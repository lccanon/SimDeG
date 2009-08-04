package simdeg.simulation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.lang.System;
import java.util.logging.Logger;
import java.util.logging.Level;

import static java.lang.Math.abs;

import simdeg.reputation.ReputationSystem;
import simdeg.util.Switcher;
import simgrid.msg.Msg;

/**
 * Accomplish the evaluation of the scheduling policy based on some
 * given metrics.
 */
public class Evaluator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Evaluator.class.getName());

    /** Pool of workers which are simulated */
    private static Set<Worker> workers = null;

    private static ReputationSystem gc = null;

    /**
     * No instance of this class.
     */
    protected Evaluator() {
    }

    /** Measures the reliability of the scheduling algorithm */
    private static int correctJob = 0;

    /** Measures the current number of completed and not forced jobs */
    private static int completedJob = 0;

    /** Measures the current number of actual executed jobs */
    private static int executedJob = 0;

    /** Measures the number of currently unfinished active jobs */
    private static int begunJobs = 0;

    /** Measures the latency of the scheduling algorithm */
    private static List<Double> latencies = new ArrayList<Double>(0);

    /** Simulator time */
    private static long startTime = System.currentTimeMillis();

   /** Workers reliability */
    private static Map<Worker,Switcher<Double>> reliability
        = new HashMap<Worker,Switcher<Double>>();

    /** Workers bugging groups */
    private static Map<Worker,Switcher<Set<CollusionGroup>>> bugging
        = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();

    /** Workers attacking groups */
    private static Map<Worker,Switcher<CollusionGroup>> attacking
        = new HashMap<Worker,Switcher<CollusionGroup>>();

     /**
     * Constructs internal reliability and collusion matrices.
     */
    static void setWorkersFaultiness(
            Map<Worker,Switcher<Double>> workersReliability,
            Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups,
            Map<Worker,Switcher<CollusionGroup>> attackingGroups) {

        /* Don't do it twice */
        if (Evaluator.workers != null)
            return;

        /* Quick check of input */
        if (workersReliability.size() != buggingGroups.size()
                || buggingGroups.size() != attackingGroups.size())
            throw new IllegalArgumentException("Worker faultiness malformed");

        Evaluator.workers = workersReliability.keySet();
        reliability.putAll(workersReliability);
        bugging.putAll(buggingGroups);
        attacking.putAll(attackingGroups);
    }

    static void setGridCharacteristics(ReputationSystem gc) {
        Evaluator.gc = gc;
    }

    /**
     * Updates begun jobs metrics.
     */
    static void submitJob(Job job) {
        if (begunJobs == 0)
            logger.info(getPerformanceHeader());
        begunJobs++;
        if (completedJob != 0)
            logger.fine(getPerformanceMeasures(false));
    }

    /**
     * Tests if the job result is correct and updates related metrics,
     * considering that the given result was forced.
     */
    static void submitResult(Job job, int overhead) {
        executedJob += overhead;
        if (job.getResult().isCorrect())
            correctJob++;
        completedJob++;
        latencies.add(Msg.getClock() - job.getSubmissionDate());
        logger.fine(getPerformanceMeasures(false));
    }

    /*
     * Notifies that the simulation has ended.
     */
    static void end() {
        logger.info(getPerformanceMeasures(true));
    }


    /**
     * Returns the accuracy which is defined as the ratio between the number of
     * correct job on the number of accepted job.
     */
    private static double getAccuracy() {
        return (double)correctJob / completedJob;
    }

    /**
     * Returns the overhead which is defined as the ratio between the number of
     * replica on the number of accepted job (only for the first concerned
     * jobs).
     */
    private static double getOverhead() {
        return (double)executedJob / completedJob;
    }

    /**
     * Returns the number of currently accepted job.
     */
    private static int getCompletedJob() {
        return completedJob;
    }

    /**
     * Returns the number of unfinished active jobs
     */
    private static int getBegunJobs() {
        return begunJobs;
    }

    /**
     * Returns the time needed for executing all the jobs.
     */
    private static double getTime() {
        return Msg.getClock();
    }

    /**
     * Computes the mean latency for each executed job.
     */
    private static double getLatency() {
        double meanLatency = 0.0d;
        for (Double d : latencies)
            meanLatency += d;
        return meanLatency / latencies.size();
    }

    /**
     * Computes the mean idle time for each worker.
     */
    private static double getIdleTime() {
        double totalIdleness = 0.0d;
        for (Worker worker : workers)
            totalIdleness += worker.getIdleTime();
        return totalIdleness / workers.size();
    }

    /**
     * Returns the current actual time spend for the simulation.
     */
    private static double getSimulatorTime() {
        return (System.currentTimeMillis() - startTime) * 1E-3d;
    }

    private static double computeColludersFraction() {
        /* Keep track of colluder fraction */
        int colluders = 0;
        for (Worker worker : workers)
            if (!bugging.get(worker).get(Msg.getClock()).isEmpty()
                    || attacking.get(worker).get(Msg.getClock()) != null)
                colluders++;
        return (double)colluders / workers.size();
    }

    /**
     * Returns the precision of the estimation of the fraction of colluders.
     */
    private static double getColludersFractionError() {
        final double colludersFraction = computeColludersFraction();
        return abs(gc.getColludersFraction().getMean() - colludersFraction);
    }

    /**
     * Returns the mean absolute error of the estimated reliability vector.
     * Can be misleading when large groups are created at the end because
     * every success will be detected soon and every failure will be notified
     * only when the group is entirely finished. Then it biases the estimators
     * at the very end.
     */
    private static double getReliabilityError() {
        double error = 0.0d;
        for (Worker worker : workers)
            error += abs(gc.getReliability(worker).getMean()
                    - reliability.get(worker).get(Msg.getClock()));
        return error / workers.size();
    }

    private static Map<Worker,Map<Worker,Double>> computeCollusion() {
        Map<Worker,Map<Worker,Double>> collusion
            = new HashMap<Worker,Map<Worker,Double>>();

         /* Initialization */
        for (Worker worker1 : workers) {
            collusion.put(worker1, new HashMap<Worker,Double>());
            for (Worker worker2 : workers)
                collusion.get(worker1).put(worker2, 0.0d);
        }

        /* Take into account bugging */
        for (Worker worker1 : workers)
            for (Worker worker2 : workers)
                for (CollusionGroup group
                        : bugging.get(worker1).get(Msg.getClock()))
                    if (bugging.get(worker2).get(Msg.getClock())
                            .contains(group)) {
                        final double previousProba
                            = collusion.get(worker1).get(worker2);
                        collusion.get(worker1).put(worker2,
                                group.getProbability()
                                * (1.0d - previousProba) + previousProba);
                    }

        /* Take into account attacking */
        for (Worker worker1 : workers) 
            for (Worker worker2 : workers)
                for (CollusionGroup group
                        : bugging.get(worker1).get(Msg.getClock()))
                    if (attacking.get(worker2).get(Msg.getClock()) == group)
                        collusion.get(worker1).put(worker2,
                                group.getProbability()
                                + collusion.get(worker1).get(worker2));

        return collusion;
    }

    /**
     * Returns the mean absolute error of the estimated collusion matrix.
     */
    private static double getCollusionError() {
        final Map<Worker,Map<Worker,Double>> collusion = computeCollusion();
        double error = 0.0d;
        for (Worker worker1 : workers)
            for (Worker worker2 : workers) {
                Set<simdeg.reputation.Worker> pair = new HashSet<simdeg.reputation.Worker>();
                pair.add(worker1);
                pair.add(worker2);
                error += abs(gc.getCollusionLikelihood(pair).getMean()
                        - collusion.get(worker1).get(worker2));
            }
        return error / workers.size() / workers.size();
    }

    /**
     * Returns the header of measures.
     */
    private static String getPerformanceHeader() {
        return "    Accuracy,    Overhead,Completed.jobs,Begun jobs,"
            +  "Simulated.time, Idle time,     Latency,   Real time,"
            +  "Col.fraction, Reliability,   Collusion";
    }

    /**
     * Returns a String summarizing the measures.
     */
    private static String getPerformanceMeasures(boolean complete) {
        String result = String.format("%12g,%12g,%12g,%12g,%12g,%12g,"
                + "%12g,%12g", getAccuracy(), getOverhead(),
                (double)getCompletedJob(), (double)getBegunJobs(), getTime(),
                getIdleTime(), getLatency(), getSimulatorTime());
        if (complete || logger.isLoggable(Level.FINER))
            result += String.format(",%12g,%12g,%12g",
                    getColludersFractionError(), getReliabilityError(),
                    getCollusionError());
        return result;
    }

}
