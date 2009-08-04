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
import simdeg.util.RV;
import simdeg.util.OutOfRangeException;

/**
 * Accomplish the evaluation of a reputation system based on some
 * given metrics.
 */
class Evaluator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Evaluator.class.getName());

    private static final double EPSILON = 1E-15;

    /** Pool of workers which are simulated */
    private Set<Worker> workers = null;

    /** Reputation system being tested */
    private ReputationSystem reputationSystem = null;

    /** Total number of step */
    private int stepsNumber;

    /** True switching steps */
    private int reliabilitySwitchStep, buggingSwitchStep;

    /** Workers true reliability */
    private Map<Worker,Switcher<Double>> reliability
        = new HashMap<Worker,Switcher<Double>>();

    /** Workers true bugging groups */
    private Map<Worker,Switcher<Set<CollusionGroup>>> bugging
        = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();

    /** Root mean squared deviation for collusion, reliability and fraction */
    private double[] RMSDc, RMSDr, RMSDa;

    /**
     * Root mean squared deviation for collusion, reliability and fraction errors
     */
    private double[] RMSDec, RMSDer, RMSDea;

    /** Correlation between true and estimated errors */
    private double[] corrEc, corrEr;

    /** Number of false negative */
    private int[] falseNegativeC, falseNegativeR, falseNegativeA;

    /** Length of adapation part */
    private int adaptationLengthC = 0, adaptationLengthR = 0, adaptationLengthA = 0;

    /** Smooth metric */
    private double[] smoothC, smoothR, smoothA;

    /** Current and previous estimated collusion matrix */
    private Map<Worker, Map<Worker, Double>> estimatedBugging, previousBugging, errorBugging;

    /** Current and previous estimated reliability */
    private Map<Worker, Double> estimatedReliability, previousReliability, errorReliability;

    /** Current and previous estimated fraction of colluders */
    private double estimatedFraction, previousFraction, errorFraction;

    /** Simulator time */
    private static long startTime = System.currentTimeMillis();

    /**
     * Constructor specifying the measured reputation system.
     */
    protected Evaluator(ReputationSystem reputationSystem) {
        this.reputationSystem = reputationSystem;
    }

    protected void setSteps(int stepsNumber, int reliabilitySwitchStep,
            int buggingSwitchStep) {
        this.stepsNumber = stepsNumber;
        this.reliabilitySwitchStep = reliabilitySwitchStep;
        this.buggingSwitchStep = buggingSwitchStep;
        RMSDc = new double[stepsNumber];
        RMSDr = new double[stepsNumber];
        RMSDa = new double[stepsNumber];
        RMSDec = new double[stepsNumber];
        RMSDer = new double[stepsNumber];
        RMSDea = new double[stepsNumber];
        corrEc = new double[stepsNumber];
        corrEr = new double[stepsNumber];
        falseNegativeC = new int[stepsNumber];
        falseNegativeR = new int[stepsNumber];
        falseNegativeA = new int[stepsNumber];
        smoothC = new double[stepsNumber];
        smoothR = new double[stepsNumber];
        smoothA = new double[stepsNumber];
    }

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

    /**
     * Notifies the progress of the simulation.
     */
    protected void setStep(int step) {
        if (step < 0 || step >= stepsNumber)
            throw new OutOfRangeException(step, 0, stepsNumber-1);

        if (step == 0)
            logger.info("Step,collusion RMSD,reliability RMSD,fraction RMSD,"
                    + "collusion error RMSD,reliability error RMSD,"
                    + "fraction error RMSD" + /*", collusion error correlation,"
                    + "reliability error correlation,collusion false negative,"
                    + "reliability false negative,fraction false negative,"
                    + "collusion smoothness,reliability smoothness,"
                    + "fraction smoothness" + */",simulation time");

        if (step != 100 && step != 300 && step != 1000 && step != 3000 && step != 10000
                && step != 30000 && step != 100000 && step != 300000 && step != 1000000)
            return;

        /* Retrieve the values from the reputation system */
        getMeandValues();

        /* Compute main metrics related to accuracy */
        computeErrorsCollusion(step);
        computeErrorsReliability(step);
        computeErrorsFraction(step);

        /* Measures the adaptiveness quality of the system */
        //measureAdaptiveness(step);

        /* Handles the smoothness metric */
        //measureSmoothness(step);
        //previousBugging = estimatedBugging;
        //previousReliability = estimatedReliability;
        //previousFraction = estimatedFraction;

        logger.info(String.format("%d,%12g,%12g,%12g,%12g,%12g,%12g,"
                    /*+ "%12g,%12g,%d,%d,%d,%12g,%12g,%12g,"*/ + "%12g", step,
                    RMSDc[step], RMSDr[step], RMSDa[step], RMSDec[step],
                    RMSDer[step], RMSDea[step], /*corrEc[step], corrEr[step],
                    falseNegativeC[step], falseNegativeR[step],
                    falseNegativeA[step], smoothC[step], smoothR[step],
                    smoothA[step],*/ (System.currentTimeMillis() - startTime) * 1E-3d));

        if (step + 1 == stepsNumber && false) {
            logger.info("collusion adaptation length,"
                    + "reliability adaptation length,fraction adaptation length");
            logger.info(String.format("%d,%d,%d", adaptationLengthC,
                        adaptationLengthR, adaptationLengthA));
        }
    }

    /**
     * Retrieves estimation from the reputation system at a given step.
     */
    private void getMeandValues() {
        estimatedBugging = new HashMap<Worker, Map<Worker, Double>>();
        errorBugging = new HashMap<Worker, Map<Worker, Double>>();
        for (Worker worker1 : workers) {
            Map<Worker, RV> row = reputationSystem.getCollusionLikelihood(worker1, workers);
            estimatedBugging.put(worker1, new HashMap<Worker, Double>());
            errorBugging.put(worker1, new HashMap<Worker, Double>());
            for (Worker worker2 : workers) {
                estimatedBugging.get(worker1).put(worker2, row.get(worker2).getMean());
                errorBugging.get(worker1).put(worker2, row.get(worker2).getError());
            }
        }
        estimatedReliability = new HashMap<Worker, Double>();
        errorReliability = new HashMap<Worker, Double>();
        for (Worker worker : workers) {
            estimatedReliability.put(worker, reputationSystem.getReliability(worker).getMean());
            errorReliability.put(worker, reputationSystem.getReliability(worker).getError());
        }
        estimatedFraction = reputationSystem.getColludersFraction().getMean();
        errorFraction = reputationSystem.getColludersFraction().getError();
    }

    /**
     * Computes metrics related to accuracy and introspection (RMSD,
     * correlation, and false negative) based on comparing estimatedBugging matrix
     * and the computed true value from bugging data structure (for collusion).
     */
    private void computeErrorsCollusion(int step) {
        RMSDc[step] = 0.0d;
        falseNegativeC[step] = 0;
        RMSDec[step] = 0.0d;
        double meanError = 0.0d, meanError2 = 0.0d, meanEstimatedError = 0.0d,
               meanEstimatedError2 = 0.0d, meanProd = 0.0d;
        Map<Worker, Map<Worker, Double>> trueBugging = computeCollusion(step);
        for (Worker worker1 : workers)
            for (Worker worker2 : workers) {
                final double estimation = estimatedBugging.get(worker1).get(worker2);
                final double proba = trueBugging.get(worker1).get(worker2);
                final double error = Math.abs(estimation - proba);
                final double estimatedError = errorBugging.get(worker1).get(worker2);
                RMSDc[step] += error * error;
                RMSDec[step] += (error - estimatedError) * (error - estimatedError);
                if (error > estimatedError)
                    falseNegativeC[step]++;
                meanError += error;
                meanError2 += error * error;
                meanEstimatedError += estimatedError;
                meanEstimatedError2 += estimatedError * estimatedError;
                meanProd += error * estimatedError;
            }
        meanError /= workers.size() * workers.size();
        meanError2 /= workers.size() * workers.size();
        meanEstimatedError /= workers.size() * workers.size();
        meanEstimatedError2 /= workers.size() * workers.size();
        meanProd /= workers.size() * workers.size();
        if (Math.abs(meanError2 - meanError * meanError) < EPSILON
                || Math.abs(meanEstimatedError2 - meanEstimatedError * meanEstimatedError) < EPSILON)
            corrEc[step] = 0.0d;
        else
            corrEc[step] = (meanProd - meanError * meanEstimatedError)
                / Math.sqrt((meanError2 - meanError * meanError)
                        * (meanEstimatedError2 - meanEstimatedError * meanEstimatedError));
        RMSDc[step] = Math.sqrt(RMSDc[step] / (workers.size() * workers.size()));
        RMSDec[step] = Math.sqrt(RMSDec[step] / (workers.size() * workers.size()));
    }

    /**
     * Computes metrics related to accuracy and introspection (RMSD,
     * correlation, and false negative) based on comparing estimatedReliability array
     * and the computed true value from reliability data structure.
     */
    private void computeErrorsReliability(int step) {
        RMSDr[step] = 0.0d;
        falseNegativeR[step] = 0;
        RMSDer[step] = 0.0d;
        double meanError = 0.0d, meanError2 = 0.0d, meanEstimatedError = 0.0d,
               meanEstimatedError2 = 0.0d, meanProd = 0.0d;
        for (Worker worker : workers) {
            final double estimation = estimatedReliability.get(worker);
            final double proba = reliability.get(worker).get(step);
            final double error = Math.abs(estimation - proba);
            final double estimatedError = errorReliability.get(worker);
            RMSDr[step] += error * error;
            RMSDer[step] += (error - estimatedError) * (error - estimatedError);
            if (error > estimatedError)
                falseNegativeR[step]++;
            meanError += error;
            meanError2 += error * error;
            meanEstimatedError += estimatedError;
            meanEstimatedError2 += estimatedError * estimatedError;
            meanProd += error * estimatedError;
        }
        meanError /= workers.size();
        meanError2 /= workers.size();
        meanEstimatedError /= workers.size();
        meanEstimatedError2 /= workers.size();
        meanProd /= workers.size();
        if (Math.abs(meanError2 - meanError * meanError) < EPSILON
                || Math.abs(meanEstimatedError2 - meanEstimatedError * meanEstimatedError) < EPSILON)
            corrEr[step] = 0.0d;
        else
            corrEr[step] = (meanProd - meanError * meanEstimatedError)
                / Math.sqrt((meanError2 - meanError * meanError)
                        * (meanEstimatedError2 - meanEstimatedError * meanEstimatedError));
        RMSDr[step] = Math.sqrt(RMSDr[step] / workers.size());
        RMSDer[step] = Math.sqrt(RMSDer[step] / workers.size());
    }

    /**
     * Computes metrics related to accuracy and introspection (RMSD,
     * correlation, and false negative) based on comparing estimatedFraction value
     * and the computed true value.
     */
    private void computeErrorsFraction(int step) {
        final double proba = computeColludersFraction(step);
        final double error = Math.abs(estimatedFraction - proba);
        final double estimatedError = errorFraction;
        RMSDa[step] = Math.abs(error);
        RMSDea[step] = Math.abs(error - estimatedError);
        if (error > estimatedError)
            falseNegativeA[step]++;
    }

    /** Optimization variables for adaptiveness */
    private double meanRMSDc = 0.0d, meanRMSDr = 0.0d, meanRMSDa = 0.0d;

    /**
     * Measures the adaptiveness metrics, namely the steps required to
     * estimated new situation.
     */
    private void measureAdaptiveness(int step) {
        /* Collusion case */
        if (step == buggingSwitchStep) {
            for (int i=0; i<step; i++)
                meanRMSDc += RMSDc[i];
            meanRMSDc /= step;
        } else if (step > buggingSwitchStep && adaptationLengthC == 0) {
            if (RMSDc[step] <= meanRMSDc)
                adaptationLengthC = step - buggingSwitchStep;
        }
        /* Reliability */
        if (step == reliabilitySwitchStep) {
            for (int i=0; i<step; i++)
                meanRMSDr += RMSDr[i];
            meanRMSDr /= step;
        } else if (step > reliabilitySwitchStep && adaptationLengthR == 0) {
            if (RMSDr[step] <= meanRMSDr)
                adaptationLengthR = step - reliabilitySwitchStep;
        }
        /* Fraction */
        if (step == buggingSwitchStep) {
            for (int i=0; i<step; i++)
                meanRMSDa += RMSDa[i];
            meanRMSDa /= step;
        } else if (step > buggingSwitchStep && adaptationLengthA == 0) {
            if (RMSDa[step] <= meanRMSDa)
                adaptationLengthA = step - buggingSwitchStep;
        }
    }

    private void measureSmoothness(int step) {
        if (step == 0)
            return;
        /* Collusion case */
        smoothC[step] = 0.0d;
        for (Worker worker1 : workers)
            for (Worker worker2 : workers) {
                final double old = previousBugging.get(worker1).get(worker2);
                final double current = estimatedBugging.get(worker1).get(worker2);
                smoothC[step] += Math.abs(old - current);
            }
        /* Reliability */
        smoothR[step] = 0.0d;
        for (Worker worker : workers) {
            final double old = previousReliability.get(worker);
            final double current = estimatedReliability.get(worker);
            smoothR[step] += Math.abs(old - current);
        }
        /* Fraction */
        smoothA[step] = Math.abs(previousFraction - estimatedFraction);
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
     * Computes the precision of the estimation of the fraction of colluders.
     */
    private double computeColludersFraction(int step) {
        /* Keep track of colluder fraction */
        int colluders = 0;
        for (Worker worker : workers)
            if (!bugging.get(worker).get(step).isEmpty())
                colluders++;
        return (double)colluders / workers.size();
    }

}
