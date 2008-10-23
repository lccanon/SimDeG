package simdeg.scheduling;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import simdeg.reputation.Worker;
import simdeg.util.Collections;
import simdeg.util.Estimator;
import simdeg.util.LUT;
import simdeg.util.RandomManager;
import flanagan.analysis.Stat;

/**
 * Implementation of the ResourcesGrouper process which create groups according to
 * 2 policies (greedy and graceful) depending of the fraction of colluders.
 */
public class GreedyGracefulResourcesGrouper extends ResourcesGrouper {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(GreedyGracefulResourcesGrouper.class.getName());

    /** Minimal size for graceful workers group */
    private static final int MIN_GREEDY = 5;

    /** Minimal probability of not having colluders */
    private static final double MIN_MAJORITY = 0.9d;

    /** Maximal error allowed when manipulating an estimator */
    private static final double MAX_ERROR = 1.0d / 3.0d;

    /** Precision of the LUT */
    private static final double LUT_PRECISION = 0.01d;

    private LUT<Double,Integer> minSizeValues = null;

    /**
     * Constructs the object and generates a binary LUT for fastening complex
     * computations.
     */
    public GreedyGracefulResourcesGrouper() {
        try {
            Method minSizeMethod = GreedyGracefulResourcesGrouper.class
                .getMethod("minSize", Double.TYPE);
            minSizeValues = new LUT<Double,Integer>(minSizeMethod,
                    new Double[] {0.0d, 1.0d, LUT_PRECISION});
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE,
                    "The method minSize was not found", e);
            System.exit(1);
        }
    }

    /**
     * Gets a group containing the worker given in argument.
     */
    Set<Worker> getGroup(Worker worker) {
        if (RandomManager.getRandom("scheduling").nextDouble() < this
                .reputationSystem.getColludersFraction().getError())
            return getGreedyGroup(worker);
        else
            return getGracefulGroup(worker);
    }

    private Set<Worker> getGreedyGroup(Worker worker) {
        /* Resulting set */
        Set<Worker> result = Collections.addElement(worker,
                new HashSet<Worker>());
        /* Other available workers */
        Set<Worker> currentWorkers = new HashSet<Worker>(this.workers);
        currentWorkers.remove(worker);
        /* New workers to be added */
        Set<Worker> addWorkers = Collections.getRandomSubGroup(Math.min(
                    workers.size(), MIN_GREEDY - 1), currentWorkers,
                RandomManager.getRandom("scheduling"));
        result.addAll(addWorkers);
        try {
            while (result.size() < minimumGroupSize()
                    && result.size() != this.workers.size()) {
                currentWorkers.removeAll(addWorkers);
                addWorkers = Collections.getRandomSubGroup(2, currentWorkers,
                        RandomManager.getRandom("scheduling"));
                result.addAll(addWorkers);
            }
        } catch (RuntimeException e) {
            return new HashSet<Worker>(this.workers);
        } finally {
            logger.fine("Greedy policy is chosen");
        }
        return result;
    }

    /**
     * Computes the minimal size for groups with the current fraction.
     */
    private int minimumGroupSize() {
        final Estimator fraction
                = reputationSystem.getColludersFraction();
            if (fraction.getError() > MAX_ERROR)
                throw new RuntimeException();
        return minSizeValues.getValue(fraction.getEstimate());
    }

    /**
     * Computes the probability that there is an absolute majority of
     * non-colluders in a group of a given size and with a given fracion
     * of colluders.
     */
    private static double nonColludersMajority(int size, double fraction) {
        double result = 0.0d;
        for (int i=0; 2*i < size; i++)
            result += Stat.binomialCoeff (size, i) * Math.pow (fraction, i)
                * Math.pow (1. - fraction, size - i);
        return result;
    }

    public static final int minSize(double fraction) {
        /* XXX with 0.48 and 0.49, it takes too long */
        if (fraction >= 0.48d)
            return Integer.MAX_VALUE;
        int i=0;
        while (nonColludersMajority(i, fraction) < MIN_MAJORITY)
            i++;
        return i;
    }

    private Set<Worker> getGracefulGroup(Worker worker) {
        logger.fine("Graceful policy is chosen");
        return Collections.addElement(worker, new HashSet<Worker>());
    }

    /**
     * Gets an extension of the group of workers given in argument (in case
     * of answers selection failure). Return null if all the known workers
     * are already in the initial group. Try to minimize the collusion
     * likelihood.
     */
    Set<Worker> getGroupExtension(Set<Worker> workers) {
        if (workers.size() == this.workers.size())
            return null;

        /* Retrieve the collusion data for the remaining workers */
        Set<Worker> candidateWorkers = new HashSet<Worker>(this.workers);
        candidateWorkers.removeAll(workers);
        final Worker worker = Collections.getRandomSubGroup(1, workers,
                RandomManager.getRandom("scheduling")).iterator().next();
        final Map<Worker,Estimator> collusion = this.reputationSystem
            .getCollusionLikelihood(worker, candidateWorkers);

        /* Compute each score */
        Map<Worker,Double> score = new HashMap<Worker,Double>();
        double total = 0.0d;
        for (Worker otherWorker : collusion.keySet()) {
            /* If too much collusion are detected, select the unkown worker */
            if (collusion.get(otherWorker).getError() > MAX_ERROR)
                return Collections.addElement(otherWorker,
                        new HashSet<Worker>());
            final double currentScore
                = (1.0d - collusion.get(otherWorker).getEstimate())
                * (1.0d - collusion.get(otherWorker).getError());
            score.put(otherWorker, currentScore);
            total += currentScore;
        }

        /* Select the worker according to its probability to collude */
        Worker selectedWorker = null;
        total *= RandomManager.getRandom("scheduling").nextDouble();
        for (Worker otherWorker : score.keySet()) {
            total -= score.get(otherWorker);
            if (total < 0.0d) {
                selectedWorker = otherWorker;
                logger.fine("Worker " + selectedWorker
                        + " is selected in group extension of " + workers
                        + " with score " + score.get(otherWorker)
                        + " and collusion " + collusion.get(otherWorker));
                break;
            }
        }

        assert(selectedWorker != null);
        return Collections.addElement(selectedWorker, new HashSet<Worker>());
    }

}
