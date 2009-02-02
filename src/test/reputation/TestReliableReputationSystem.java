package simdeg.reputation;

import simdeg.util.Estimator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestReliableReputationSystem {

    private static Worker worker;
    private static Set<Worker> workers;

    private static final double MIN_ERROR = 0.25d;
    private static final double EPSILON = 2E-2d;

    @BeforeClass public static void createWorkers() {
        workers = new HashSet<Worker>();
        workers.add(new Worker() {});
        workers.add(new Worker() {});
        workers.add(new Worker() {});
        worker = workers.iterator().next();
    }

    @Test public void getReliability() {
        ReliableReputationSystem rrs
            = new ReliableReputationSystem();
        rrs.addAllWorkers(workers);
        Estimator reliability = rrs.getReliability(worker);
        assertTrue("Error too low: " + reliability.getError(),
                reliability.getError() > MIN_ERROR);
    }

    @Test(expected=NoSuchElementException.class)
    public void getReliabilityException() {
        ReliableReputationSystem rrs
            = new ReliableReputationSystem();
        rrs.getReliability(worker);
    }

    @Test public void scenarioSimple() {
        /* Initializations and declarations */
        ReliableReputationSystem rrs = new ReliableReputationSystem();
        rrs.addAllWorkers(workers);
        /* Scenario with two sets */
        Random rand = new Random(0L);
        for (int i=0; i<100; i++) {
            Job job = new Job() {};
            Result correct = new Result() {};
            Result bad = new Result() {};
            for (Worker w : workers)
                if (w != worker)
                    rrs.setWorkerResult(w, job, correct);
            if (rand.nextBoolean())
                rrs.setWorkerResult(worker, job, correct);
            else
                rrs.setWorkerResult(worker, job, bad);
            rrs.setCertifiedResult(job, correct);
        }
        /* Test collusion estimations */
        Estimator reliability = rrs.getReliability(worker);
        assertEquals(0.5d, reliability.getEstimate(), reliability.getError());
    }

}
