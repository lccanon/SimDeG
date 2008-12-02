package simdeg.reputation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestReliableReputationSystem {

    private static Worker workerFirst;
    private static Set<Worker> workersFirst;
    private static List<Set<Worker>> workersList;
    private static Set<Worker> workers;

    private static final double EPSILON = 2E-2d;
    private static final double BIG_EPSILON = 2E-1d;

    @BeforeClass public static void createWorkers() {
        workersList = new ArrayList<Set<Worker>>();
        workersList.add(new HashSet<Worker>());
        for (int i=0; i<25; i++)
            workersList.get(0).add(new Worker() {});
        workerFirst =  workersList.get(0).iterator().next();
        workersFirst = new HashSet<Worker>();
        workersFirst.add(workerFirst);
        for (int i=1; i<6; i++) {
            workersList.add(new HashSet<Worker>());
            for (int j=0; j<15; j++)
                workersList.get(i).add(new Worker() {});
        }
        workers = new HashSet<Worker>();
        for (Set<Worker> set : workersList)
            workers.addAll(set);
        //scenarios();
    }

    @Test(expected=NoSuchElementException.class)
    public void reliabilityHistoryException() {
        AgreementReputationSystem gc
            = new AgreementReputationSystem();
        gc.addAllWorkers(workersList.get(2));
        gc.getReliability(workerFirst);
    }

    /*
    @Test public void getReliability() {
        AgreementReputationSystem gc
            = new AgreementReputationSystem();
        gc.addAllWorkers(workers);
        for (int i=0; i<100; i++)
            gc.setSuccess(workersFirst);
        assertEquals(1.0d, gc.getReliability(workerFirst).getEstimate(),
                EPSILON);
        for (int i=0; i<100; i++)
            gc.setFailure(workersFirst);
        assertEquals(0.0d, gc.getReliability(workerFirst).getEstimate(),
                EPSILON);
    }
    */

}
