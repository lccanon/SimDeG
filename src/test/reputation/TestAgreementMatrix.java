package simdeg.reputation;

import simdeg.util.BTS;
import simdeg.util.Estimator;
import simdeg.util.OutOfRangeException;

import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.NoSuchElementException;

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
        matrix = new AgreementMatrix(new BTS(1.0d));
    }

    private void checkFirstLine(Estimator[][] agreement) {
        double max = 0.0d;
        for (int j=0; j<agreement[0].length; j++)
            max = Math.max(max, agreement[0][j].getEstimate());
        assertEquals(max, 1.0d, EPSILON);
    }

    @Test(expected=OutOfRangeException.class)
    public void agreementMatrixException() {
        new AgreementMatrix(new BTS(0.0d));
    }

    @Test public void merge() {
        matrix.addAll(workers);
        for (int i=0; i<100; i++) {
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.increaseAgreement(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    matrix.decreaseAgreement(worker, otherWorker);
                    matrix.decreaseAgreement(otherWorker, worker);
                }
        }
        assertEquals(29, matrix.getBiggest().size());
        Estimator[][] agreement = matrix.getAgreements(workers);
        assertEquals(2, agreement.length);
        checkFirstLine(agreement);
        assertEquals(1.0d, agreement[1][0].getEstimate(), EPSILON);
        assertEquals(0.0d, agreement[1][1].getEstimate(), EPSILON);
    }

    @Test public void split() {
        matrix.addAll(workers);
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
        assertEquals(1, matrix.getBiggest().size());
        Estimator[][] agreement = matrix.getAgreements(workers);
        assertEquals(39, agreement.length);
        checkFirstLine(agreement);
        for (int i=1; i<agreement.length; i++)
            for (int j=0; j<agreement[i].length; j++)
                if (i == j+1)
                    assertEquals(1.0d, agreement[i][j].getEstimate(), EPSILON);
                else
                    assertEquals(0.0d, agreement[i][j].getEstimate(), EPSILON);
    }

    @Test public void getAgreementsSimple() {
        matrix.addAll(workers);
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
        Estimator[][] agreement = matrix.getAgreements(workers);
        checkFirstLine(agreement);
        assertEquals(29, matrix.getBiggest().size());
        assertEquals(2, agreement.length);
        assertEquals(1.0d, agreement[1][0].getEstimate(), EPSILON);
        assertEquals(0.5d, agreement[1][1].getEstimate(), EPSILON);
    }

    @Test public void getAgreements() {
        matrix.addAll(workers);
        Random rand = new Random(0L);
        for (int i=0; i<100; i++) {
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
        Estimator[][] agreement = matrix.getAgreements(workers);
        checkFirstLine(agreement);
        assertEquals(29, matrix.getBiggest().size());
        assertEquals(2, agreement.length);
        assertEquals(1.0d, agreement[1][0].getEstimate(), EPSILON);
        assertEquals(0.5d, agreement[1][1].getEstimate(), 10.0d * EPSILON);
    }

    @Test(expected=NoSuchElementException.class)
    public void getAgreementsException() {
        matrix.addAll(workers1);
        matrix.getAgreements(workers2);
    }

}
