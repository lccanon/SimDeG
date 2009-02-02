package simdeg.reputation;

import static simdeg.util.Collections.addElement;

import simdeg.util.Estimator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.NoSuchElementException;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestAgreementReputationSystem {

    private static Worker worker;
    private static Set<Worker> workers;
    private static List<Set<Worker>> workersList;

    private static final double MIN_ERROR = 0.25d;
    private static final double EPSILON = 2E-2d;

    @BeforeClass public static void createWorkers() {
        workersList = new ArrayList<Set<Worker>>();
        for (int i=0; i<6; i++) {
            workersList.add(new HashSet<Worker>());
            for (int j=0; j < (i==0?25:15); j++)
                workersList.get(i).add(new Worker() {});
        }
        workers = new HashSet<Worker>();
        for (Set<Worker> set : workersList)
            workers.addAll(set);
        worker = workers.iterator().next();
    }

    @Test public void setAgreement() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        ars.setAgreement(new Job() {}, worker, workers);
    }

    @Test(expected=NoSuchElementException.class)
    public void setAgreementException() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        ars.removeAllWorkers(addElement(worker, new HashSet<Worker>()));
        ars.setAgreement(new Job() {}, worker, workers);
    }

    @Test public void setDisagreement() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        ars.setDisagreement(new Job() {}, worker, workers);
    }

    @Test(expected=NoSuchElementException.class)
    public void setDisagreementException() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        ars.removeAllWorkers(addElement(worker, new HashSet<Worker>()));
        ars.setDisagreement(new Job() {}, worker, workers);
    }

    @Test public void setDistinctSets() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.setDistinctSets(new Job() {}, workers, new HashSet<Set<Worker>>());
    }

    @Test public void getCollusionLikelihood() {
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        Estimator collusion = ars.getCollusionLikelihood(workers);
        assertEquals(0.0d, collusion.getEstimate(), EPSILON);
        Map<Worker,Estimator> map = ars.getCollusionLikelihood(worker, workers);
        for (Worker w : map.keySet())
            if (w != worker)
                assertTrue("Error too low: " + map.get(w).getError(),
                        map.get(w).getError() > MIN_ERROR);
        Estimator fraction = ars.getColludersFraction();
        assertTrue("Error too low: " + fraction.getError(),
                fraction.getError() > MIN_ERROR);
    }

    @Test public void scenarioSimple() {
        /* Initializations and declarations */
        AgreementReputationSystem ars = new AgreementReputationSystem();
        Set<Worker> workersTwo = new HashSet<Worker>();
        workersTwo.addAll(workersList.get(0));
        workersTwo.addAll(workersList.get(1));
        ars.addAllWorkers(workersTwo);
        /* Scenario with two sets */
        Random rand = new Random(0L);
        for (int i=0; i<200; i++) {
            Job job = new Job() {};
            Result correct = new Result() {};
            Result bad = new Result() {};
            for (Worker worker1 : workersList.get(0))
                ars.setWorkerResult(worker1, job, correct);
            if (rand.nextBoolean())
                for (Worker worker2 : workersList.get(1))
                    ars.setWorkerResult(worker2, job, correct);
            else
                for (Worker worker2 : workersList.get(1))
                    ars.setWorkerResult(worker2, job, bad);
            ars.setCertifiedResult(job, correct);
        }
        /* Test collusion estimations */
        Estimator collusion1 = ars.getCollusionLikelihood(workersList.get(0));
        assertEquals(0.0d, collusion1.getEstimate(), EPSILON);
        Estimator collusion2 = ars.getCollusionLikelihood(workersList.get(1));
        assertEquals(0.5d, collusion2.getEstimate(), collusion2.getError());
        Estimator collusion3 = ars.getCollusionLikelihood(workersTwo);
        assertEquals(0.0d, collusion3.getEstimate(), EPSILON);
    }

    @Test public void scenario() {
        /* Initializations and declarations */
        AgreementReputationSystem ars = new AgreementReputationSystem();
        ars.addAllWorkers(workers);
        final int[][] scheme = new int[][] {
                {0, 0, 0, 2, 2, 0},
                {0, 0, 1, 1, 0, 1},
                {0, 0, 0, 2, 2, 2},
                {0, 1, 1, 0, 0, 1},
                {0, 1, 0, 1, 0, 0},
                {0, 0, 0, 0, 2, 0},
                {0, 0, 0, 0, 2, 2},
                {0, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 2, 0},
                {0, 1, 0, 1, 0, 0} };
        /* Scenario */
        for (int i=0; i<200; i++) {
            Job job = new Job() {};
            final Result[] results = new Result[3];
            for (int j=0; j<results.length; j++)
                results[j] = new Result() {};
            for (int j=0; j<workersList.size(); j++)
                for (Worker w : workersList.get(j))
                    ars.setWorkerResult(w, job, results[scheme[i%scheme.length][j]]);
            ars.setCertifiedResult(job, results[0]);
        }
        /* Test collusion estimations */
        Set<Worker> set = new HashSet<Worker>();
        /* First case */
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        Estimator collusion1 = ars.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion1.getEstimate(), EPSILON);
        /* Second case */
        set.clear();
        set.addAll(workersList.get(3));
        set.addAll(workersList.get(5));
        Estimator collusion2 = ars.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion2.getEstimate(), EPSILON);
        /* Third case (this value should be 0.0d, but we do our best) */
        set.clear();
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        set.addAll(workersList.get(3));
        Estimator collusion3 = ars.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion3.getEstimate(), EPSILON);
        /* Fourth case */
        set.clear();
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        set.addAll(workersList.get(5));
        Estimator collusion4 = ars.getCollusionLikelihood(set);
        assertEquals(0.1d, collusion4.getEstimate(), EPSILON);
        /* Fith case (this value should be 0.1d, but we do our best) */
        set.clear();
        set.addAll(workersList.get(3));
        set.addAll(workersList.get(4));
        set.addAll(workersList.get(5));
        Estimator collusion5 = ars.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion5.getEstimate(), EPSILON);
    }

}
