package simdeg.reputation.simulation;

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
import simdeg.util.Estimator;
import simdeg.util.BTS;
import simdeg.util.OutOfRangeException;

public class TestScheduler {

    /* To be ignore if the output has to be analysed */
    @BeforeClass
    public static void desactivateLogger() {
        Logger logger1
            = Logger.getLogger(Evaluator.class.getName());
        logger1.setLevel(Level.SEVERE);
        Logger logger2
            = Logger.getLogger(Scheduler.class.getName());
        logger2.setLevel(Level.SEVERE);
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
            public Estimator getReliability(simdeg.reputation.Worker worker) {
                if (((Worker)worker).getName().equals("worker1"))
                    return new BTS(0.5d, 0.01d);
                return new BTS(1.0d, 0.01d);
            }
            public Estimator getCollusionLikelihood(Set<? extends simdeg.reputation.Worker> workers) {
                for (simdeg.reputation.Worker worker : workers)
                    if (((Worker)worker).getName().equals("worker1")
                            || ((Worker)worker).getName().equals("worker2"))
                        return new BTS(0.0d, 0.01d);
                return new BTS(0.5d, 0.01d);
            }
            public <W extends simdeg.reputation.Worker> Map<W, Estimator> getCollusionLikelihood(
                    W worker, Set<W> workers) {
                Map<W, Estimator> map = new HashMap<W, Estimator>();
                for (W otherWorker : workers)
                    if ((((Worker)worker).getName().equals("worker1")
                                || ((Worker)worker).getName().equals("worker2"))
                            && (((Worker)otherWorker).getName().equals("worker1")
                                || ((Worker)otherWorker).getName().equals("worker2")))
                        map.put(otherWorker, new BTS(0.5d, 0.01d));
                    else
                        map.put(otherWorker, new BTS(0.0d, 0.01d));
                return map;
            }
            public Estimator getColludersFraction() {
                return new BTS(0.4d, 0.01d);
            }
            public String toString() {
                return "test reputation system";
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
        Worker worker1 = new Worker("worker1", rel, col);
        workersReliability.put(worker1, rel);
        buggingGroups.put(worker1, col);
        /* Worker2 */
        proba = new Double[1];
        proba[0] = 1.0d;
        rel = new Switcher<Double>(proba, new double[0], new double[0]);
        Worker worker2 = new Worker("worker2", rel, col);
        workersReliability.put(worker2, rel);
        buggingGroups.put(worker2, col);
        /* Worker3 */
        group = new Set[1];
        group[0] = new HashSet<CollusionGroup>();
        col = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        Worker worker3 = new Worker("worker3", rel, col);
        workersReliability.put(worker3, rel);
        buggingGroups.put(worker3, col);
        /* Worker4 */
        Worker worker4 = new Worker("worker4", rel, col);
        workersReliability.put(worker4, rel);
        buggingGroups.put(worker4, col);
        /* Worker5 */
        Worker worker5 = new Worker("worker5", rel, col);
        workersReliability.put(worker5, rel);
        buggingGroups.put(worker5, col);
    }

    @Test public void scheduler() {
        ReputationSystem reputationSystem = getReputationSystem();
        Evaluator evaluator = new Evaluator(reputationSystem);
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        evaluator.setSteps(100, 100, 100);
        Scheduler scheduler = new Scheduler(reputationSystem, evaluator);
        scheduler.addAllWorkers(workersReliability.keySet());
    }

    @Test public void removeAllworkers() {
        ReputationSystem reputationSystem = getReputationSystem();
        Evaluator evaluator = new Evaluator(reputationSystem);
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        evaluator.setSteps(100, 100, 100);
        Scheduler scheduler = new Scheduler(reputationSystem, evaluator);
        scheduler.addAllWorkers(workersReliability.keySet());
        scheduler.removeAllWorkers(workersReliability.keySet());
    }

    @Test public void start() {
        ReputationSystem reputationSystem = getReputationSystem();
        Evaluator evaluator = new Evaluator(reputationSystem);
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        evaluator.setSteps(100, 100, 100);
        Scheduler scheduler = new Scheduler(reputationSystem, evaluator);
        scheduler.addAllWorkers(workersReliability.keySet());
        scheduler.start(100, 3.0d, 0.5d, 0.9d);
    }

    @Test(expected=OutOfRangeException.class)
        public void startException() {
        ReputationSystem reputationSystem = getReputationSystem();
        Evaluator evaluator = new Evaluator(reputationSystem);
        Map<Worker,Switcher<Double>> workersReliability = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        createWorker(workersReliability, buggingGroups);
        evaluator.setWorkersFaultiness(workersReliability, buggingGroups);
        evaluator.setSteps(100, 100, 100);
        Scheduler scheduler = new Scheduler(reputationSystem, evaluator);
        scheduler.addAllWorkers(workersReliability.keySet());
        scheduler.start(100, 3.0d, 1.01d, 0.9d);
    }

}
