package simdeg.reputation;

import simdeg.util.BetaEstimator;
import simdeg.util.RV;
import simdeg.util.OutOfRangeException;

import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/* TODO add test
 * Test that most of merges happen with small sets and slow down when the
 * biggest size increases.
 * Test that splits happen when set are smalls and decrease after.
 */
public class TestAgreementMatrix {

    private static Set<Worker> workers1;
    private static Set<Worker> workers2;
    private static Set<Worker> workers;

    private static final double LARGE_EPSILON = 1E-1d;
    private static final double BIG_EPSILON = 1E-2d;
    private static final double EPSILON = 1E-5d;

    private AgreementMatrix matrix;

    @BeforeClass public static void createWorkers() {
        workers1 = new HashSet<Worker>();
        for (int i=0; i<10; i++) {
            final int hash = i * 2;
            workers1.add(new Worker() {
                    public int hashCode() { return hash; }
                    public String toString() { return "1"; }
                    });
        }
        workers2 = new HashSet<Worker>();
        for (int i=0; i<29; i++) {
            final int hash = i * 2 + 1;
            workers2.add(new Worker() {
                    public int hashCode() { return hash; }
                    public String toString() { return "2"; }
                    });
        }
        workers = new HashSet<Worker>();
        workers.addAll(workers1);
        workers.addAll(workers2);
    }

    @Before public void createMatrix() {
        matrix = new AgreementMatrix(new BetaEstimator(1.0d));
    }

    private void checkFirstLine(RV[][] agreement) {
        double max = 0.0d;
        for (int j=0; j<agreement[0].length; j++)
            max = Math.max(max, agreement[0][j].getMean());
        assertEquals(1.0d, max, BIG_EPSILON);
    }

    @Test(expected=OutOfRangeException.class)
    public void agreementMatrixException() {
        new AgreementMatrix(new BetaEstimator(0.0d));
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
        RV[][] agreement = matrix.getAgreements(workers);
        assertEquals(2, agreement.length);
        checkFirstLine(agreement);
        assertEquals(1.0d, agreement[1][0].getMean(), BIG_EPSILON);
        assertEquals(0.0d, agreement[1][1].getMean(), BIG_EPSILON);
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
        RV[][] agreement = matrix.getAgreements(workers);
        assertEquals(39, agreement.length);
        checkFirstLine(agreement);
        for (int i=1; i<agreement.length; i++)
            for (int j=0; j<agreement[i].length; j++)
                if (i == j+1)
                    assertEquals(1.0d, agreement[i][j].getMean(), BIG_EPSILON);
                else
                    assertEquals(0.0d, agreement[i][j].getMean(), BIG_EPSILON);
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
        RV[][] agreement = matrix.getAgreements(workers);
        checkFirstLine(agreement);
        assertEquals(29, matrix.getBiggest().size());
        assertEquals(2, agreement.length);
        assertEquals(1.0d, agreement[1][0].getMean(), BIG_EPSILON);
        assertEquals(0.5d, agreement[1][1].getMean(), BIG_EPSILON);
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
        RV[][] agreement = matrix.getAgreements(workers);
        checkFirstLine(agreement);
        assertEquals(29, matrix.getBiggest().size());
        assertEquals(2, agreement.length);
        assertEquals(1.0d, agreement[1][0].getMean(), BIG_EPSILON);
        assertEquals(0.5d, agreement[1][1].getMean(), LARGE_EPSILON);
    }

    @Test(expected=NoSuchElementException.class)
    public void getAgreementsException() {
        matrix.addAll(workers1);
        matrix.getAgreements(workers2);
    }

}
