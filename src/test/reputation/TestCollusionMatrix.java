package simdeg.reputation;

import simdeg.util.BTS;
import simdeg.util.Estimator;

import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class TestCollusionMatrix {

    private static Set<Worker> workers1;
    private static Set<Worker> workers2;
    private static Set<Worker> workers;

    private static final double EPSILON = 1E-2d;

    private CollusionMatrix matrix;

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
        matrix = new CollusionMatrix(new BTS());
    }

    @Test public void merge() {
        matrix.addAll(workers);
        for (int i=0; i<100; i++) {
            /* Collusion in set 2 */
            for (Worker worker : workers2)
                matrix.decreaseCollusion(worker, worker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.decreaseCollusion(worker, otherWorker);
            /* Collusion in set 1 */
            for (Worker worker : workers1)
                matrix.increaseCollusion(worker, worker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.increaseCollusion(worker, otherWorker);
            /* Collusion between set 1 and 2 */
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    matrix.decreaseCollusion(worker, otherWorker);
                    matrix.decreaseCollusion(otherWorker, worker);
                }
        }
        assertEquals(workers2.size(), matrix.getBiggest().size());
        final Estimator[][] collusion = matrix.getCollusions(workers);
        assertEquals(2, collusion.length);
        assertEquals(0.0d, collusion[0][1].getEstimate(), EPSILON);
        final Estimator[][] collusion1 = matrix.getCollusions(workers1);
        assertEquals(1, collusion1.length);
        assertEquals(1.0d, collusion1[0][0].getEstimate(), EPSILON);
        final Estimator[][] collusion2 = matrix.getCollusions(workers2);
        assertEquals(1, collusion2.length);
        assertEquals(0.0d, collusion2[0][0].getEstimate(), EPSILON);
    }

    @Test public void split() {
        matrix.addAll(workers);
        for (int i=0; i<100; i++)
            for (Worker worker : workers)
                for (Worker otherWorker : workers)
                    matrix.decreaseCollusion(worker, otherWorker);
        for (Worker worker : workers)
            for (Worker otherWorker : workers)
                matrix.increaseCollusion(worker, otherWorker);
        assertEquals(1, matrix.getBiggest().size());
        Estimator[][] collusion = matrix.getCollusions(workers);
        assertEquals(39, collusion.length);
    }

    @Test public void readapt() {
        matrix.addAll(workers);
        /* Merge all first workers in a non-colluding set */
        for (int i=0; i<100; i++)
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    matrix.decreaseCollusion(worker, otherWorker);
        final Estimator[][] collusion1 = matrix.getCollusions(workers1);
        assertEquals(1, collusion1.length);
        assertEquals(0.0d, collusion1[0][0].getEstimate(), EPSILON);
        /* Merge second workers in a colluding set until the size becomes
         * the largest */
        loop: while (true) {
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2) {
                    matrix.increaseCollusion(worker, otherWorker);
                    if (!matrix.getBiggest().containsAll(workers1))
                        break loop;
                }
        }
        /* At this stage, the biggest set has just changed */
        final Estimator[][] collusion = matrix.getCollusions(matrix.getBiggest());
        assertEquals(1, collusion.length);
        assertEquals(0.0d, collusion[0][0].getEstimate(), collusion[0][0].getError());
    }

    @Test public void getCollusionsSimple() {
        matrix.addAll(workers);
        for (int i=0, j=0; i<100; i++) {
            /* First the dominant that will be the first to merge */
            for (Worker worker : workers2)
                matrix.decreaseCollusion(worker, worker);
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.decreaseCollusion(worker, otherWorker);
            /* The non-dominant */
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    if ((i + j++) % 2 == 0) {
                        matrix.increaseCollusion(worker, otherWorker);
                        /* For symetry on the diagonal */
                        if (worker == otherWorker)
                            matrix.increaseCollusion(worker, otherWorker);
                    } else {
                        matrix.decreaseCollusion(worker, otherWorker);
                        if (worker == otherWorker)
                            matrix.decreaseCollusion(worker, otherWorker);
                    }
            /* All other relations */
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    matrix.decreaseCollusion(worker, otherWorker);
                    matrix.decreaseCollusion(otherWorker, worker);
                }
        }
        assertEquals(workers2.size(), matrix.getBiggest().size());
        final Estimator[][] collusion = matrix.getCollusions(workers);
        assertEquals(2, collusion.length);
        assertEquals(0.0d, collusion[0][1].getEstimate(), EPSILON);
        final Estimator[][] collusion1 = matrix.getCollusions(workers1);
        assertEquals(1, collusion1.length);
        assertEquals(0.5d, collusion1[0][0].getEstimate(), EPSILON);
        final Estimator[][] collusion2 = matrix.getCollusions(workers2);
        assertEquals(1, collusion2.length);
        assertEquals(0.0d, collusion2[0][0].getEstimate(), EPSILON);
    }

    @Test public void getCollusions() {
        matrix.addAll(workers);
        Random rand = new Random(0L);
        for (int i=0, j=0; i<100; i++) {
            for (Worker worker : workers2)
                for (Worker otherWorker : workers2)
                    matrix.decreaseCollusion(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers1)
                    if (rand.nextBoolean())
                        matrix.increaseCollusion(worker, otherWorker);
                    else
                        matrix.decreaseCollusion(worker, otherWorker);
            for (Worker worker : workers1)
                for (Worker otherWorker : workers2) {
                    matrix.decreaseCollusion(worker, otherWorker);
                    matrix.decreaseCollusion(otherWorker, worker);
                }
        }
        assertEquals(workers2.size(), matrix.getBiggest().size());
        final Estimator[][] collusion = matrix.getCollusions(workers);
        assertEquals(2, collusion.length);
        assertEquals(0.0d, collusion[0][1].getEstimate(), EPSILON);
        final Estimator[][] collusion1 = matrix.getCollusions(workers1);
        assertEquals(1, collusion1.length);
        assertEquals(0.5d, collusion1[0][0].getEstimate(), collusion1[0][0].getError());
        final Estimator[][] collusion2 = matrix.getCollusions(workers2);
        assertEquals(1, collusion2.length);
        assertEquals(0.0d, collusion2[0][0].getEstimate(), EPSILON);
    }

    @Test(expected=NoSuchElementException.class)
    public void getCollusionsException() {
        matrix.addAll(workers1);
        matrix.getCollusions(workers2);
    }

}
