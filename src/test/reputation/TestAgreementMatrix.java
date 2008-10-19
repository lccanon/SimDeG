package simdeg.reputation;

import simdeg.util.BTS;
import simdeg.util.Estimator;

import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class TestAgreementMatrix {

    private static Set<Worker> workers1;
    private static Set<Worker> workers2;
    private static Set<Worker> workers;

    private static final double EPSILON = 1E-2d;

    private AgreementMatrix matrix;

    @BeforeClass public static void createWorkers() {
        workers1 = new HashSet<Worker>();
        for (int i=0; i<10; i++)
            workers1.add(new Worker() {});
        workers2 = new HashSet<Worker>();
        for (int i=0; i<29; i++)
            workers2.add(new Worker() {});
        workers = new HashSet<Worker>();
        workers.addAll(workers1);
        workers.addAll(workers2);
    }

    @Before public void createMatrix() {
        matrix = new AgreementMatrix(new BTS());
    }

    @Test(expected=IllegalArgumentException.class)
    public void agreementException() {
        matrix.addAllWorkers(workers1);
        matrix.getCollusion(workers2);
    }

    @Test public void getBiggest() {
        assertTrue(matrix.getBiggest().isEmpty());
        matrix.addAllWorkers(workers1);
        assertFalse(matrix.getBiggest().isEmpty());
        matrix.addAllWorkers(workers2);
        assertFalse(matrix.getBiggest().isEmpty());
        matrix.removeAllWorkers(workers1);
        assertFalse(matrix.getBiggest().isEmpty());
        matrix.removeAllWorkers(workers2);
        assertTrue(matrix.getBiggest().isEmpty());
    }

    private void checkFirstLine(Estimator[][] collusion) {
        double max = 0.0d;
        for (int j=0; j<collusion[0].length; j++)
            max = Math.max(max, collusion[0][j].getEstimate());
        assertEquals(max, 1.0d, EPSILON);
    }

    @Test public void merge() {
        matrix.addAllWorkers(workers);
        for (int i=0; i<100; i++) {
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2)
                    matrix.decreaseAgreement(worker, otherWorker);
        }
        assertEquals(matrix.getBiggest().size(), 29);
        Estimator[][] collusion = matrix.getCollusion(workers);
        assertEquals(collusion.length, 2);
        checkFirstLine(collusion);
        assertEquals(collusion[1][0].getEstimate(), 1.0d, EPSILON);
        assertEquals(collusion[1][1].getEstimate(), 0.0d, EPSILON);
    }

    @Test public void scatter() {
        matrix.addAllWorkers(workers);
        for (int i=0; i<100; i++) {
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2)
                    matrix.decreaseAgreement(worker, otherWorker);
        }
        for (int i=0; i<100; i++)
            for (Worker worker : workers)
                for (Worker otherWorker : workers)
                    matrix.decreaseAgreement(worker, otherWorker);
        assertEquals(matrix.getBiggest().size(), 1);
        Estimator[][] collusion = matrix.getCollusion(workers);
        assertEquals(collusion.length, 39);
        checkFirstLine(collusion);
        for (int i=1; i<collusion.length; i++)
            for (int j=0; j<collusion[i].length; j++)
                if (i == j+1)
                    assertEquals(collusion[i][j].getEstimate(), 1.0d, EPSILON);
                else
                    assertEquals(collusion[i][j].getEstimate(), 0.0d, EPSILON);
    }

    @Test public void getCollusionSimple() {
        matrix.addAllWorkers(workers);
        for (int i=0, j=0; i<100; i++) {
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    if ((i + j++) % 2 == 0)
                        matrix.increaseAgreement(worker, otherWorker);
                    else
                        matrix.decreaseAgreement(worker, otherWorker);
                }
        }
        Estimator[][] collusion = matrix.getCollusion(workers);
        checkFirstLine(collusion);
        assertEquals(matrix.getBiggest().size(), 29);
        assertEquals(collusion.length, 2);
        assertEquals(collusion[1][0].getEstimate(), 1.0d, EPSILON);
        assertEquals(collusion[1][1].getEstimate(), 0.5d, EPSILON);
    }

    @Test public void getCollusion() {
        matrix.addAllWorkers(workers);
        Random rand = new Random(0L);
        for (int i=0; i<200; i++) {
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    if (rand.nextBoolean())
                        matrix.increaseAgreement(worker, otherWorker);
                    else
                        matrix.decreaseAgreement(worker, otherWorker);
                }
        }
        Estimator[][] collusion = matrix.getCollusion(workers);
        checkFirstLine(collusion);
        assertEquals(matrix.getBiggest().size(), 29);
        assertEquals(collusion.length, 2);
        assertEquals(collusion[1][0].getEstimate(), 1.0d, EPSILON);
        assertEquals(collusion[1][1].getEstimate(), 0.5d, 0.2d);
    }

}
