package simdeg.simulation;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import simdeg.reputation.ReputationSystem;
import simdeg.util.Switcher;
import simdeg.util.RV;
import simdeg.util.Beta;
import simdeg.util.OutOfRangeException;

public class TestEvaluator {

    /* To be ignored if the output has to be analysed */
    @BeforeClass
    public static void desactivateLogger() {
        Logger logger
            = Logger.getLogger(Evaluator.class.getName());
        logger.setLevel(Level.SEVERE);
    }

    private static ReputationSystem getReputationSystem() {
        return new ReputationSystem() {
            public void addAllWorkers(Set<? extends simdeg.reputation.Worker> workers) {}
            public void removeAllWorkers(Set<? extends simdeg.reputation.Worker> workers) {}
            public void setWorkerResult(simdeg.reputation.Worker worker,
                    simdeg.reputation.Job job, simdeg.reputation.Result result) {
            }
            public void setCertifiedResult(simdeg.reputation.Job job, simdeg.reputation.Result result) {
            }
            public RV getReliability(simdeg.reputation.Worker worker) {
                if (((Worker)worker).toString().equals("worker0"))
                    return new Beta(0.5d);
                return new Beta(1.0d);
            }
            public RV getCollusionLikelihood(Set<? extends simdeg.reputation.Worker> workers) {
                for (simdeg.reputation.Worker worker : workers)
                    if (((Worker)worker).toString().equals("worker0")
                            || ((Worker)worker).toString().equals("worker1"))
                        return new Beta(0.0d);
                return new Beta(0.5d);
            }
            public <W extends simdeg.reputation.Worker> Map<W, RV> getCollusionLikelihood(
                    W worker, Set<W> workers) {
                Map<W, RV> map = new HashMap<W, RV>();
                for (W otherWorker : workers)
                    if ((((Worker)worker).toString().equals("worker0")
                                || ((Worker)worker).toString().equals("worker1"))
                            && (((Worker)otherWorker).toString().equals("worker0")
                                || ((Worker)otherWorker).toString().equals("worker1")))
                        map.put(otherWorker, new Beta(0.5d));
                    else
                        map.put(otherWorker, new Beta(0.0d));
                return map;
            }
            public RV getColludersFraction() {
                return new Beta(0.4d);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void createWorker(
            Map<Worker,Switcher<Double>> workersReliability,
            Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups) {
        Double[] proba;
        Set<CollusionGroup>[] group;
        CollusionGroup colluder = new CollusionGroup(0.5d, "");
        /* Worker1 */
        proba = new Double[1];
        group = new Set[1];
        group[0] = new HashSet<CollusionGroup>();
        proba[0] = 0.5d;
        group[0].add(colluder);
        Switcher<Double> rel = new Switcher<Double>(proba, new double[0], new double[0]);
        Switcher<Set<CollusionGroup>> col
            = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        Worker worker1 = new Worker(rel, col);
        workersReliability.put(worker1, rel);
        buggingGroups.put(worker1, col);
        /* Worker2 */
        proba = new Double[1];
        proba[0] = 1.0d;
        rel = new Switcher<Double>(proba, new double[0], new double[0]);
        Worker worker2 = new Worker(rel, col);
        workersReliability.put(worker2, rel);
        buggingGroups.put(worker2, col);
        /* Worker 3, 4 and 5 */
        group = new Set[1];
        group[0] = new HashSet<CollusionGroup>();
        col = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        for (int i=0; i<3; i++) {
            Worker worker = new Worker(rel, col);
            workersReliability.put(worker, rel);
            buggingGroups.put(worker, col);
        }
    }

    /*
    @Test public void evaluator() {
        new Evaluator(getReputationSystem());
    }
    */

    /*
    @Test public void setStep() {
        Evaluator evaluator = new Evaluator(getReputationSystem());
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        for (int i=0; i<100; i++)
            evaluator.setStep(i);
    }
    */

    /*
    @Test(expected=OutOfRangeException.class)
        public void setStepException() {
        Evaluator evaluator = new Evaluator(getReputationSystem());
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        evaluator.setStep(-1);
    }
    */

    /**
     * Test that the number of checked collusion values corresponds to the
     * number of possible combination between each group.
     */
    /*
    @SuppressWarnings("unchecked")
    @Test public void convertCollusion() {
        Set<Set<Worker>> sets= new HashSet<Set<Worker>>();
        for (int i=0; i<10; i++) {
            Set<Worker> singleton = new HashSet<Worker>();
            singleton.add(new Worker(new Switcher<Double>(new Double[] { 0.0d },
                            new double[0], new double[0]),
                        new Switcher<Set<CollusionGroup>>(new Set[] { new HashSet<CollusionGroup>() },
                            new double[0], new double[0])));
            sets.add(singleton);
        }
        Map<Set<Worker>, Map<Set<Worker>, Double>> map
            = new HashMap<Set<Worker>, Map<Set<Worker>, Double>>();
        for (Set<Worker> set1 : sets) {
            map.put(set1, new HashMap<Set<Worker>, Double>());
            for (Set<Worker> set2 : sets)
                map.get(set1).put(set2, 0.0d);
        }
        Evaluator evaluator = new Evaluator(getReputationSystem());
        Map<Set<Worker>, Double> result = evaluator.convertCollusion(map);
        assertEquals(11 * 10 / 2, result.size());
    }
    */

}
