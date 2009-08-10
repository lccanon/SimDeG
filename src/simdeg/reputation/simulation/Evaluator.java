package simdeg.reputation.simulation;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
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

    /** Reputation system being tested */
    private ReputationSystem reputationSystem = null;

    /** Simulator time */
    private static long startTime = System.currentTimeMillis();

    /**
     * Constructor specifying the measured reputation system.
     */
    protected Evaluator(ReputationSystem reputationSystem) {
        this.reputationSystem = reputationSystem;
    }

    private double realFraction;

    private RV previousFraction;

    private Map<Worker, Double> realReliability;

    private Map<Worker, RV> previousReliability = new HashMap<Worker, RV>();

    private Map<Set<Worker>, Double> realCollusion;

    private Map<Set<Worker>, RV> previousCollusion = new HashMap<Set<Worker>, RV>();

    protected Map<Set<Worker>, Double> convertCollusion(Map<Set<Worker>,
            Map<Set<Worker>, Double>> collusion) {
        Map<Set<Worker>, Double> result = new HashMap<Set<Worker>, Double>();
        for (Set<Worker> set : collusion.keySet())
            for (Set<Worker> otherSet : collusion.get(set).keySet()) {
                final Set<Worker> merge = new HashSet<Worker>(set);
                merge.addAll(otherSet);
                result.put(merge, collusion.get(set).get(otherSet));
            }
        return result;
    }

    private List<List<Double>> performMeasures() {
        /* Fraction case */
        List<MetricStructure> frac = new ArrayList<MetricStructure>();
        {
            final RV estimation = reputationSystem.getColludersFraction();
            frac.add(new MetricStructure(realFraction,
                        estimation, previousFraction));
            previousFraction = estimation.clone();
        }
        /* Reliability case */
        List<MetricStructure> rel = new ArrayList<MetricStructure>();
        for (Worker worker : realReliability.keySet()) {
            final RV estimation = reputationSystem.getReliability(worker);
            rel.add(new MetricStructure(realReliability.get(worker),
                        estimation, previousReliability.get(worker)));
            previousReliability.put(worker, estimation.clone());
        }
        /* Collusion case */
        List<MetricStructure> col = new ArrayList<MetricStructure>();
        for (Set<Worker> set : realCollusion.keySet()) {
            final RV estimation = reputationSystem.getCollusionLikelihood(set);
            col.add(new MetricStructure(realCollusion.get(set),
                        estimation, previousCollusion.get(set)));
            previousCollusion.put(set, estimation.clone());
        }
        /* Get the metrics' values */
        List<List<Double>> result = new ArrayList<List<Double>>();
        result.add(MetricStructure.computeMetric(frac));
        result.add(MetricStructure.computeMetric(rel));
        result.add(MetricStructure.computeMetric(col));
        return result;
    }

    /**
     * Notifies the progress of the simulation.
     */
    protected void setStep(int step) {
        if (step < 0)
            throw new OutOfRangeException(step, 0, Integer.MAX_VALUE);

        if (step == 0)
            logger.info("Step,simulation time,"
                    + "fraction RMSD,fraction error RMSD,fraction smoothness,"
                    + "reliability RMSD,reliability error RMSD,reliability smoothness,"
                    + "collusion RMSD,collusion error RMSD,collusion smoothness");

        realFraction = computeFraction(step);
        realReliability = computeReliability(step);
        realCollusion = convertCollusion(computeCollusion(step));
        
        List<List<Double>> measures = performMeasures();

        logger.info(String.format("%d,%12g,%12g,%12g,%12g,%12g,%12g,%12g,"
                    + "%12g,%12g,%12g", step,
                    (System.currentTimeMillis() - startTime) * 1E-3d,
                    measures.get(0).get(0), measures.get(0).get(1), measures.get(0).get(2),
                    measures.get(1).get(0), measures.get(1).get(1), measures.get(1).get(2),
                    measures.get(2).get(0), measures.get(2).get(1), measures.get(2).get(2)));
    }

    /* TODO delete */
    /** Pool of workers which are simulated */
    private Set<Worker> workers = null;

    /* TODO delete */
    /** Workers true reliability */
    private Map<Worker,Switcher<Double>> reliability
        = new HashMap<Worker,Switcher<Double>>();

    /* TODO delete */
    /** Workers true bugging groups */
    private Map<Worker,Switcher<Set<CollusionGroup>>> bugging
        = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();

    /* TODO delete */
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

    /* TODO change */
    private double computeFraction(int step) {
        /* Keep track of colluder fraction */
        int colluders = 0;
        for (Worker worker : workers)
            if (!bugging.get(worker).get(step).isEmpty())
                colluders++;
        return (double)colluders / workers.size();
    }

    /* TODO change */
    private Map<Worker, Double> computeReliability(int step) {
        Map<Worker, Double> result = new HashMap<Worker, Double>();
        for (Worker worker : reliability.keySet())
            result.put(worker, reliability.get(worker).get(step));
        return result;
    }

    /* TODO change */
    private Map<Set<Worker>,Map<Set<Worker>,Double>> computeCollusion(int step) {
        Map<Set<CollusionGroup>, Set<Worker>> transform = new
            HashMap<Set<CollusionGroup>, Set<Worker>>();
        for (Worker worker : workers) {
            final Set<CollusionGroup> groups = bugging.get(worker).get(step);
            if (!transform.containsKey(groups))
                transform.put(groups, new HashSet<Worker>());
            transform.get(bugging.get(worker).get(step)).add(worker);
        }

        Map<Set<Worker>,Map<Set<Worker>,Double>> result
            = new HashMap<Set<Worker>,Map<Set<Worker>,Double>>();
        for (Set<CollusionGroup> set1 : transform.keySet()) {
            result.put(transform.get(set1), new HashMap<Set<Worker>,Double>());
            for (Set<CollusionGroup> set2 : transform.keySet()) {
                final Set<CollusionGroup> inter = new HashSet<CollusionGroup>(set1);
                inter.retainAll(set2);
                final Set<CollusionGroup> notInter = new HashSet<CollusionGroup>(set1);
                notInter.addAll(set2);
                notInter.removeAll(inter);
                double probaDistinct = 0.0d;
                for (CollusionGroup group : notInter)
                    probaDistinct += (1.0d - probaDistinct) * group.getProbability();
                double proba = 0.0d;
                for (CollusionGroup group : inter)
                    proba += (1.0d - proba) * group.getProbability();
                result.get(transform.get(set1)).put(transform.get(set2),
                        proba * (1.0d - probaDistinct));
            }
        }

        return result;
    }

}

class MetricStructure {

    private Double real;

    private RV estimation;

    private RV previous;

    protected MetricStructure(Double real, RV estimation, RV previous) {
        this.real = real;
        this.estimation = estimation;
        this.previous = previous;
    }

    protected double getError() {
        if (real == null || real.isNaN() || estimation == null)
            return 0.0d;
        return Math.abs(real - estimation.getMean());
    }

    protected double getIntro() {
        if (real == null || real.isNaN() || estimation == null)
            return 0.0d;
        return Math.abs(estimation.getError() - Math.abs(real - estimation.getMean()));
    }

    protected double getChange() {
        if (estimation == null || previous == null)
            return 0.0d;
        return Math.abs(estimation.getMean() - previous.getMean());
    }

    protected static List<Double> computeMetric(List<MetricStructure> values) {
        double RMSDacc = 0.0d;
        double RMSDerr = 0.0d;
        double smooth = 0.0d;
        for (MetricStructure value : values) {
            RMSDacc += value.getError() * value.getError();
            RMSDerr += value.getIntro() * value.getIntro();
            smooth += value.getChange();
        }
        List<Double> result = new ArrayList<Double>();
        result.add(Math.sqrt(RMSDacc / values.size()));
        result.add(Math.sqrt(RMSDerr / values.size()));
        result.add(smooth / values.size());
        return result;
    }

}
