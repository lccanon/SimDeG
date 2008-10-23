package simdeg.reputation.simulation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.lang.System;
import java.util.logging.Logger;

import simdeg.reputation.ReputationSystem;
import simdeg.util.Switcher;

/**
 * Accomplish the evaluation of a reputation system based on some
 * given metrics.
 */
class Evaluator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Evaluator.class.getName());

    /** Pool of workers which are simulated */
    private Set<Worker> workers = null;

    /** Reputation system being tested */
    private ReputationSystem reputationSystem = null;

    /**
     * Constructor specifying the measured reputation system.
     */
    protected Evaluator(ReputationSystem reputationSystem) {
        this.reputationSystem = reputationSystem;
    }

    /** Workers reliability */
    private Map<Worker,Switcher<Double>> reliability
        = new HashMap<Worker,Switcher<Double>>();

    /** Workers bugging groups */
    private Map<Worker,Switcher<Set<CollusionGroup>>> bugging
        = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();

     /**
     * Constructs internal reliability and collusion matrices.
     */
    protected void setWorkersFaultiness(
            Map<Worker,Switcher<Double>> workersReliability,
            Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups) {

        /* Don't do it twice */
        if (this.workers != null)
            return;

        /* Quick check of input */
        if (workersReliability.size() != buggingGroups.size())
            throw new IllegalArgumentException("Worker faultiness malformed");

        this.workers = workersReliability.keySet();
        reliability.putAll(workersReliability);
        bugging.putAll(buggingGroups);
    }

    private double computeColludersFraction(int step) {
        /* Keep track of colluder fraction */
        int colluders = 0;
        for (Worker worker : workers)
            if (!bugging.get(worker).get(step).isEmpty())
                colluders++;
        return (double)colluders / workers.size();
    }

    /**
     * Returns the precision of the estimation of the fraction of colluders.
     */
    private double getColludersFractionError(int step) {
        final double colludersFraction = computeColludersFraction(step);
        return abs(reputationSystem.getColludersFraction().getEstimate() - colludersFraction);
    }

    /**
     * Returns the mean absolute error of the estimated reliability vector.
     * Can be misleading when large groups are created at the end because
     * every success will be detected soon and every failure will be notified
     * only when the group is entirely finished. Then it biases the estimators
     * at the very end.
     */
    private double getReliabilityError(int step) {
        double error = 0.0d;
        for (Worker worker : workers)
            error += abs(reputationSystem.getReliability(worker).getEstimate()
                    - reliability.get(worker).get(step));
        return error / workers.size();
    }

    private Map<Worker,Map<Worker,Double>> computeCollusion(int step) {
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
                        : bugging.get(worker1).get(step))
                    if (bugging.get(worker2).get(step)
                            .contains(group)) {
                        final double previousProba
                            = collusion.get(worker1).get(worker2);
                        collusion.get(worker1).put(worker2,
                                group.getProbability()
                                * (1.0d - previousProba) + previousProba);
                    }

        return collusion;
    }

    /**
     * Returns the root mean squares deviation of the estimated collusion matrix.
     */
    private double getCollusionRMSD(int step) {
        final Map<Worker,Map<Worker,Double>> collusion = computeCollusion(step);
        double total = 0.0d;
        for (Worker worker1 : workers)
            for (Worker worker2 : workers) {
                Set<simdeg.reputation.Worker> pair = new HashSet<simdeg.reputation.Worker>();
                pair.add(worker1);
                pair.add(worker2);
                double error = reputationSystem.getCollusionLikelihood(pair).getEstimate()
                    - collusion.get(worker1).get(worker2);
                total += error * error;
            }
        return Math.sqrt(total / workers.size() / workers.size());
    }

    /**
     * Notifies the progress of the simulation.
     */
    protected void setStep(int step) {
        if (step == 0)
            logger.info(getPerformanceHeader());
        logger.info(getPerformanceMeasures(step));
    }

    /**
     * Returns the header of measures.
     */
    private String getPerformanceHeader() {
        return "Col.fraction, Reliability,   Collusion";
    }

    /**
     * Returns a String summarizing the measures.
     */
    private String getPerformanceMeasures(int step) {
        String result = String.format("%12g,%12g,%12g",
                    getColludersFractionError(step), getReliabilityError(step),
                    getCollusionError(step));
        return result;
    }

}
