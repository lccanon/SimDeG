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

public class TestAgreementReputationSystem {

    private static Worker workerFirst;
    private static Set<Worker> workersFirst;
    private static List<Set<Worker>> workersList;
    private static Set<Worker> workers;
    private static List<AgreementReputationSystem> gcs
        = new ArrayList<AgreementReputationSystem>();

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
        scenarios();
    }

    private static void setAgreement(AgreementReputationSystem gc, Job job,
            Set<Worker> workers1, Set<Worker> workers2) {
        if (workers1 == workers2) {
            Set<Worker> done = new HashSet<Worker>();
            for (Worker worker1 : workers1) {
                if (!done.isEmpty())
                    gc.setAgreement(job, worker1, done);
                done.add(worker1);
            }
        } else
            for (Worker worker1 : workers1)
                gc.setAgreement(job, worker1, workers2);
    }

    private static void setDisagreement(AgreementReputationSystem gc, Job job,
            Set<Worker> workers1, Set<Worker> workers2) {
        for (Worker worker1 : workers1)
            gc.setDisagreement(job, worker1, workers2);
    }

    private static void scenarioTwoSimple(AgreementReputationSystem gc) {
        for (int i=0; i<200; i++) {
            Job job = new Job() {};
            int[] answers = null;
            switch (i % 2) {
                case 0:
                    answers = new int[] {0, 0};
                    break;
                case 1:
                    answers = new int[] {0, 1};
                    break;
            }
            for (int k=0; k<2; k++)
                for (int j=k; j<2; j++)
                    if (answers[k] == answers[j])
                        setAgreement(gc, job, workersList.get(k),
                                workersList.get(j));
                    else
                        setDisagreement(gc, job, workersList.get(k),
                                workersList.get(j));
        }
    }

    @Test public void getCollusionLikelihoodTwoSimple() {
        AgreementReputationSystem gc
            = new AgreementReputationSystem();
        Set<Worker> workersTwo = new HashSet<Worker>();
        workersTwo.addAll(workersList.get(0));
        workersTwo.addAll(workersList.get(1));
        gc.addAllWorkers(workersTwo);
        scenarioTwoSimple(gc);
        Set<Worker> set = new HashSet<Worker>();
        set.addAll(workersList.get(1));
        assertEquals(0.5d, gc.getCollusionLikelihood(set).getEstimate(),
                EPSILON);
        set.addAll(workersList.get(0));
        assertEquals(0.0d, gc.getCollusionLikelihood(set).getEstimate(),
                EPSILON);
    }

    private static void scenarioTwo(AgreementReputationSystem gc) {
        Random rand = new Random(0L);
        for (int i=0; i<80; i++) {
            Job job = new Job() {};
            setAgreement(gc, job, workersList.get(0), workersList.get(0));
            setAgreement(gc, job, workersList.get(1), workersList.get(1));
            if (rand.nextBoolean())
                setAgreement(gc, job, workersList.get(0), workersList.get(1));
            else
                setDisagreement(gc, job, workersList.get(0),
                        workersList.get(1));
        }
    }

    @Test public void getCollusionLikelihoodTwo() {
        AgreementReputationSystem gc
            = new AgreementReputationSystem();
        Set<Worker> workersTwo = new HashSet<Worker>();
        workersTwo.addAll(workersList.get(0));
        workersTwo.addAll(workersList.get(1));
        gc.addAllWorkers(workersTwo);
        scenarioTwo(gc);
        Set<Worker> set = new HashSet<Worker>();
        set.addAll(workersList.get(1));
        assertEquals(0.5d, gc.getCollusionLikelihood(set).getEstimate(),
                BIG_EPSILON);
        set.addAll(workersList.get(0));
        assertEquals(0.0d, gc.getCollusionLikelihood(set).getEstimate(), EPSILON);
    }

    private static int[][] generateAnswer(int n, int k) {
        int[][] scheme = new int[][] {
                {0, 0, 0, 2, 2, 0},
                {0, 0, 1, 1, 0, 1},
                {0, 1, 1, 0, 0, 1},
                {0, 1, 0, 1, 0, 0},
                {0, 0, 0, 0, 2, 0},
                {0, 0, 0, 0, 2, 2},
                {0, 1, 1, 0, 0, 0},
                {0, 0, 0, 2, 2, 2},
                {0, 0, 1, 1, 2, 0},
                {0, 1, 0, 1, 0, 0} };
        int[][] answers = new int[n][k];
        for (int i=0; i<n; i++)
            for (int j=0; j<k; j++)
                answers[i][j] = scheme[i%scheme.length][j];
        return answers;
    }

    private static AgreementReputationSystem scenario(int setNumber) {
        AgreementReputationSystem gc
            = new AgreementReputationSystem();
        gc.addAllWorkers(workers);
        final int n = 100;
        int[][] answers = generateAnswer(n, setNumber);
        for (int i=0; i<n; i++) {
            Job job = new Job() {};
            for (int j=0; j<setNumber; j++)
                for (int k=j; k<setNumber; k++)
                    if (answers[i][k] == answers[i][j])
                        setAgreement(gc, job, workersList.get(k),
                                workersList.get(j));
                    else
                        setDisagreement(gc, job, workersList.get(k),
                                workersList.get(j));
        }
        return gc;
    }

    private static void scenarios() {
        for (int i=1; i<=6; i++)
            gcs.add(scenario(i));
    }

    @Test public void getCollusionLikelihood1() {
        for (int i=2; i<6; i++) {
            Set<Worker> set = new HashSet<Worker>();
            set.addAll(workersList.get(1));
            set.addAll(workersList.get(2));
            assertEquals(0.2d, gcs.get(i).getCollusionLikelihood(set)
                    .getEstimate(), EPSILON);
        }
    }

    @Test public void getCollusionLikelihood2() {
        for (int i=5; i<6; i++) {
            Set<Worker> set = new HashSet<Worker>();
            set.addAll(workersList.get(3));
            set.addAll(workersList.get(5));
            assertEquals(0.2d, gcs.get(i).getCollusionLikelihood(set)
                    .getEstimate(), EPSILON);
        }
    }

    @Test public void getCollusionLikelihood3() {
        for (int i=3; i<6; i++) {
            Set<Worker> set = new HashSet<Worker>();
            set.addAll(workersList.get(1));
            set.addAll(workersList.get(2));
            set.addAll(workersList.get(3));
            /* This value should be 0.0d, but we do our best */
            assertEquals(0.2d, gcs.get(i).getCollusionLikelihood(set)
                    .getEstimate(), EPSILON);
        }
    }

    @Test public void getCollusionLikelihood4() {
        for (int i=5; i<6; i++) {
            Set<Worker> set = new HashSet<Worker>();
            set.addAll(workersList.get(1));
            set.addAll(workersList.get(2));
            set.addAll(workersList.get(5));
            assertEquals(0.1d, gcs.get(i).getCollusionLikelihood(set)
                    .getEstimate(), EPSILON);
        }
    }

    @Test public void getCollusionLikelihood5() {
        for (int i=5; i<6; i++) {
            Set<Worker> set = new HashSet<Worker>();
            set.addAll(workersList.get(3));
            set.addAll(workersList.get(4));
            set.addAll(workersList.get(5));
            /* This value should be 0.1d, but we do our best */
            assertEquals(0.2d, gcs.get(i).getCollusionLikelihood(set)
                    .getEstimate(), EPSILON);
        }
    }

}
