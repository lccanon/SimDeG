package simdeg.reputation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.util.RV;

public class TestReliableReputationSystem {

    private static Worker worker;
    private static Set<Worker> workers;

    private static final double LARGE_EPSILON = 1E-1d;
    private static final double MIN_ERROR = 0.25d;

    @BeforeClass public static void createWorkers() {
        workers = new HashSet<Worker>();
        for (int i=0; i<3; i++)
            workers.add(new Worker() {});
        worker = workers.iterator().next();
    }

    @Test public void getReliability() {
        ReliableReputationSystem<Worker> rrs
            = new ReliableReputationSystem<Worker>();
        rrs.addAllWorkers(workers);
        RV reliability = rrs.getReliability(worker);
        assertTrue("Error too low: " + reliability.getError(),
                reliability.getError() > MIN_ERROR);
    }

    @Test(expected=NoSuchElementException.class)
    public void getReliabilityException() {
        ReliableReputationSystem<Worker> rrs
            = new ReliableReputationSystem<Worker>();
        rrs.getReliability(worker);
    }

    @Test public void scenarioSimple() {
        /* Initializations and declarations */
        ReliableReputationSystem<Worker> rrs = new ReliableReputationSystem<Worker>();
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
        RV reliability = rrs.getReliability(worker);
        assertEquals(0.5d, reliability.getMean(), LARGE_EPSILON);
        assertTrue("Error too high: " + reliability.getError(),
                reliability.getError() < LARGE_EPSILON);
    }

}