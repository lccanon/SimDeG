package simdeg.reputation;

import static simdeg.util.Collections.addElement;

import simdeg.util.RV;

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

/* TODO add test
 * This test should focus more on the internal operations such as split, merge,
 * and readapt. For instance, it should contains several independent scenarii.
 * Also, the number of transformation should be low.
 * A test with 2 sets with proba 1.
 * A test with 2 sets without random and proba 0.5.
 * A test with 2 sets with random and proba 0.5.
 * A test with 3 disjoint sets with proba 1.
 * A test with 3 disjoint sets without random and proba 0.5.
 * A test with 3 disjoint sets with random and proba 0.5.
 * A test with 3 overlapping sets with proba 1.
 * A test with 3 overlapping sets without random and proba 0.5.
 * A test with 3 overlapping sets with random and proba 0.5.
 * A test with the 6 overlapping sets without random.
 * A test with the 6 overlapping sets with random.
 * For each test, the certified result depends on the biggest or not.
 * Each set should make sure that: merges operations is limited (more than the
 * minimum but less than no pairwise merge + number of splits); split
 * operations is limited (only a few); at most one readaptation; lot of
 * operations in first iterations; few operations for medium set; quasi
 * no-operation after; the set are correctly characterized; the probabilities
 * are ok.
 */
public class TestCollusionReputationSystem {

    private static Worker worker;
    private static Set<Worker> workers;
    private static List<Set<Worker>> workersList;

    private static final double MIN_ERROR = 0.25d;
    private static final int ITERATION = 100;

    @BeforeClass public static void createWorkers() {
        workersList = new ArrayList<Set<Worker>>();
        for (int i=0; i<6; i++) {
            workersList.add(new HashSet<Worker>());
            for (int j=0; j < (i==0?25:15); j++) {
                final int hash = j * 6 + i;
                final int group = i;
                workersList.get(i).add(new Worker() {public int hashCode() {
                        return hash; }public String toString(){return group+"";}});
            }
        }
        workers = new HashSet<Worker>();
        for (Set<Worker> set : workersList)
            workers.addAll(set);
        worker = workers.iterator().next();
    }

    @Test public void setAgreement() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.addAllWorkers(workers);
        crs.setAgreement(new Job() {}, worker, workers);
    }

    @Test public void setDisagreement() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.addAllWorkers(workers);
        crs.setDisagreement(new Job() {}, worker, workers);
    }

    @Test(expected=NoSuchElementException.class)
    public void setDisagreementException() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.addAllWorkers(workers);
        crs.removeAllWorkers(addElement(worker, new HashSet<Worker>()));
        crs.setDisagreement(new Job() {}, worker, workers);
    }

    @Test public void setDistinctSets() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        Job job = new Job() {};
        crs.addAllWorkers(workers);
        crs.setAgreement(job, worker, workers);
        crs.setDistinctSets(job, workers, new HashSet<Set<Worker>>());
    }

    @Test(expected=NoSuchElementException.class)
    public void setDistinctSetsException() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.setDistinctSets(new Job() {}, workers, new HashSet<Set<Worker>>());
    }

    @Test public void getCollusionLikelihood() {
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.addAllWorkers(workers);
        RV collusion = crs.getCollusionLikelihood(workers);
        /* As there is several minimum (50,000), it is almost zero */
        assertEquals(0.0d, collusion.getMean(), collusion.getError());
        Map<Worker,RV> map = crs.getCollusionLikelihood(worker, workers);
        for (Worker w : map.keySet())
            if (w != worker)
                assertTrue("Error too low: " + map.get(w).getError(),
                        map.get(w).getError() > MIN_ERROR);
        RV fraction = crs.getColludersFraction();
        assertTrue("Error too low: " + fraction.getError(),
                fraction.getError() > MIN_ERROR);
    }

    @Test public void scenarioSimple() {
        /* Initializations and declarations */
        CollusionReputationSystem crs = new CollusionReputationSystem();
        Set<Worker> workersTwo = new HashSet<Worker>();
        workersTwo.addAll(workersList.get(0));
        workersTwo.addAll(workersList.get(1));
        crs.addAllWorkers(workersTwo);
        /* Scenario with two sets */
        Random rand = new Random(0L);
        for (int i=0; i<ITERATION; i++) {
            Job job = new Job() {};
            Result correct = new Result() {};
            Result bad = new Result() {};
            for (Worker worker1 : workersList.get(0))
                crs.setWorkerResult(worker1, job, correct);
            if (rand.nextBoolean())
                for (Worker worker2 : workersList.get(1))
                    crs.setWorkerResult(worker2, job, correct);
            else
                for (Worker worker2 : workersList.get(1))
                    crs.setWorkerResult(worker2, job, bad);
            crs.setCertifiedResult(job, correct);
        }
        /* Test collusion estimations */
        RV collusion1 = crs.getCollusionLikelihood(workersList.get(0));
        assertEquals(0.0d, collusion1.getMean(), collusion1.getError());
        RV collusion2 = crs.getCollusionLikelihood(workersList.get(1));
        assertEquals(0.5d, collusion2.getMean(), collusion2.getError());
        RV collusion3 = crs.getCollusionLikelihood(workersTwo);
        assertEquals(0.0d, collusion3.getMean(), collusion3.getError());
    }

    @Test public void scenario() {
        /* Initializations and declarations */
        CollusionReputationSystem crs = new CollusionReputationSystem();
        crs.addAllWorkers(workers);
        final int[][] scheme = new int[][] {
                {0, 0, 0, 2, 2, 0},
                {0, 0, 1, 1, 0, 1},
                {0, 1, 1, 0, 0, 1},
                {0, 1, 0, 1, 0, 0},
                {0, 0, 0, 0, 2, 0},
                {0, 0, 0, 0, 2, 2},
                {0, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 2, 0},
                {0, 0, 0, 2, 2, 2},
                {0, 1, 0, 1, 0, 0} };
        /* Scenario */
        for (int i=0; i<ITERATION; i++) {
            Job job = new Job() {};
            final Result[] results = new Result[3];
            for (int j=0; j<results.length; j++)
                results[j] = new Result() {};
            for (int j=0; j<workersList.size(); j++)
                for (Worker w : workersList.get(j))
                    crs.setWorkerResult(w, job, results[scheme[i%scheme.length][j]]);
            crs.setCertifiedResult(job, results[0]);
        }
        /* Test collusion estimations */
        Set<Worker> set = new HashSet<Worker>();
        /* First case */
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        RV collusion1 = crs.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion1.getMean(), collusion1.getError());
        /* Second case */
        set.clear();
        set.addAll(workersList.get(3));
        set.addAll(workersList.get(5));
        RV collusion2 = crs.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion2.getMean(), collusion2.getError());
        /* Third case (this value should be 0.0d, but we do our best) */
        set.clear();
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        set.addAll(workersList.get(3));
        RV collusion3 = crs.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion3.getMean(), collusion3.getError());
        /* Fourth case */
        set.clear();
        set.addAll(workersList.get(1));
        set.addAll(workersList.get(2));
        set.addAll(workersList.get(5));
        RV collusion4 = crs.getCollusionLikelihood(set);
        assertEquals(0.1d, collusion4.getMean(), collusion4.getError());
        /* Fith case (this value should be 0.1d, but we do our best) */
        set.clear();
        set.addAll(workersList.get(3));
        set.addAll(workersList.get(4));
        set.addAll(workersList.get(5));
        RV collusion5 = crs.getCollusionLikelihood(set);
        assertEquals(0.2d, collusion5.getMean(), collusion5.getError());
    }

}
