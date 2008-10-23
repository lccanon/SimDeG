package simdeg.scheduling;


import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.junit.Test;
import org.junit.BeforeClass;

import simdeg.reputation.OptimisticGridCharacteristics;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Worker;
import simdeg.util.BTS;
import simdeg.util.Collections;
import simdeg.util.Estimator;
import static org.junit.Assert.*;

public class TestGreedyGracefulResourcesGrouper {

    private static final int MIN_GREEDY = 5;

    private static Worker worker;
    private static Set<Worker> workerSet;
    private static Set<Worker> workers;

    private static GreedyGracefulResourcesGrouper groupCreator;

    @BeforeClass public static void createWorkers() {
        workers = new HashSet<Worker>();
        for (int i=0; i<100; i++)
            workers.add(new Worker() {});
        worker = workers.iterator().next();
        workerSet = Collections.addElement(worker, new HashSet<Worker>());
        groupCreator = new GreedyGracefulResourcesGrouper();
        groupCreator.addAllWorkers(workers);
    }

    @Test public void getGroupGraceful() {
        ReputationSystem gc = new OptimisticGridCharacteristics();
        groupCreator.setReputationSystem(gc);
        assertEquals(groupCreator.getGroup(worker).size(), 1);
    }

    @Test public void getGroupGreedySimple() {
        ReputationSystem gc = new OptimisticGridCharacteristics() {
            public Estimator getColludersFraction() {
                return new BTS(0.0d, 0.2d);
            }
        };
        groupCreator.setReputationSystem(gc);
        Set<Worker> group = groupCreator.getGroup(worker);
        while (group.size() == 1)
            group = groupCreator.getGroup(worker);
        assertEquals(MIN_GREEDY, group.size());
    }

    @Test public void getGroupGreedy() {
        ReputationSystem gc = new OptimisticGridCharacteristics() {
            public Estimator getColludersFraction() {
                return new BTS(0.31d, 0.2d);
            }
        };
        groupCreator.setReputationSystem(gc);
        Set<Worker> group = groupCreator.getGroup(worker);
        while (group.size() == 1)
            group = groupCreator.getGroup(worker);
        assertTrue("Greedy groups should be larger :" + group.size(),
                group.size() >= MIN_GREEDY); 
        assertEquals(GreedyGracefulResourcesGrouper.minSize(new BTS(0.31d, 0.2d).getEstimate()), group.size());
    }

    @Test public void getGroupExtensionSimple() {
        ReputationSystem gc = new OptimisticGridCharacteristics() {
            public Map<Worker,Estimator> getCollusionLikelihood(Worker worker,
                    Set<Worker> workers) {
                Map<Worker,Estimator> result = new HashMap<Worker,Estimator>();
                for (Worker otherWorker : workers)
                    result.put(otherWorker, new BTS(0.0d, 1.0d));
                return result;
            }
        };
        groupCreator.setReputationSystem(gc);
        Set<Worker> group = groupCreator.getGroupExtension(workerSet);
        assertEquals(group.size(), 1);
    }

    @Test public void getGroupExtension() {
        ReputationSystem gc = new OptimisticGridCharacteristics() {
            public Map<Worker,Estimator> getCollusionLikelihood(Worker worker,
                    Set<Worker> workers) {
                Map<Worker,Estimator> result = new HashMap<Worker,Estimator>();
                for (Worker otherWorker : workers)
                    result.put(otherWorker, new BTS(1.0d));
                result.put(workers.iterator().next(), new BTS(0.0d));
                return result;
            }
        };
        groupCreator.setReputationSystem(gc);
        Set<Worker> group = groupCreator.getGroupExtension(workerSet);
        Set<Worker> candidateWorkers = new HashSet<Worker>(workers);
        candidateWorkers.removeAll(workerSet);
        assertEquals(group.iterator().next(), candidateWorkers.iterator().next());
    }

}
