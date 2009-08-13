package simdeg.reputation.simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import simdeg.reputation.ReputationSystem;
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

    private double realFraction;

    private RV previousFraction;

    private Map<Worker, Double> realReliability = new HashMap<Worker, Double>();

    private Map<Worker, RV> previousReliability = new HashMap<Worker, RV>();

    private Map<Set<Worker>, Double> realCollusion = new HashMap<Set<Worker>, Double>();

    private Map<Set<Worker>, RV> previousCollusion = new HashMap<Set<Worker>, RV>();

    /**
     * Constructor specifying the measured reputation system.
     */
    protected Evaluator(ReputationSystem reputationSystem,
            String fileCharacteristic) {
        this.reputationSystem = reputationSystem;
        /* Read characteristic file */
        readCharacteristicFile(fileCharacteristic);
        logger.info("timestamp,simulation time,"
                + "fraction RMSD,fraction error RMSD,fraction smoothness,"
                + "reliability RMSD,reliability error RMSD,reliability smoothness,"
                + "collusion RMSD,collusion error RMSD,collusion smoothness");
    }

    private Map<Set<Worker>, Double> convertCollusion(Map<Set<Worker>,
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

    private void readCharacteristicFile(String fileCharacteristic) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(fileCharacteristic));
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "File " + fileCharacteristic + " was not found", e);
            System.exit(1);
        }
        scanner.useLocale(Locale.ENGLISH);

        /* Parse reliability and colluding group */
        final Map<Integer, Set<Worker>> colGroups = new TreeMap<Integer, Set<Worker>>();
        final int workerCount = scanner.nextInt();
        for (int i = 0 ; i < workerCount ; i++) {
            Worker worker = new Worker(scanner.nextLong());
            this.realReliability.put(worker, scanner.nextDouble());
            final Integer group = scanner.nextInt();
            if (!colGroups.containsKey(group))
                colGroups.put(group, new HashSet<Worker>());
            colGroups.get(group).add(worker);
        }
        logger.config("Reliability characteristics read");

        /* Compute fraction of colluders */
        realFraction = 1.0d - (double)colGroups.get(0).size()
            / this.realReliability.size();
        logger.config("Colluder fraction computed: " + realFraction);

        /* Parse collusion probabilities */
        final Map<Set<Worker>, Map<Set<Worker>, Double>> collusion
            = new HashMap<Set<Worker>, Map<Set<Worker>, Double>>();
        for (Integer i : colGroups.keySet()) {
            final Set<Worker> groupI = colGroups.get(new Integer(i));
            collusion.put(groupI, new HashMap<Set<Worker>, Double>());
            for (Integer j : colGroups.keySet())
                collusion.get(groupI).put(colGroups.get(j),
                        scanner.nextDouble());
        }
        scanner.close();
        this.realCollusion = convertCollusion(collusion);
        logger.config("Collusion characteristics read");
    }

    protected Set<Worker> getAllWorkers() {
        return realReliability.keySet();
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
    protected void setStep(long timestamp) {
        if (timestamp < 0)
            throw new OutOfRangeException(timestamp, 0, Long.MAX_VALUE);

        List<List<Double>> measures = performMeasures();

        logger.info(String.format("%d,%12g,%12g,%12g,%12g,%12g,%12g,%12g,"
                    + "%12g,%12g,%12g", timestamp,
                    (System.currentTimeMillis() - startTime) * 1E-3d,
                    measures.get(0).get(0), measures.get(0).get(1), measures.get(0).get(2),
                    measures.get(1).get(0), measures.get(1).get(1), measures.get(1).get(2),
                    measures.get(2).get(0), measures.get(2).get(1), measures.get(2).get(2)));
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
