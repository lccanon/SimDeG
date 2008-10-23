package simdeg.reputation.simulation;

import static simdeg.reputation.simulation.Result.ResultType.*;

import java.util.Set;
import java.util.HashSet;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import simdeg.util.Switcher;

public class TestWorker {

    private static Double[] proba;

    private static Set<CollusionGroup>[] group;

    @SuppressWarnings("unchecked")
    @Before
    public void allocateElements() {
        proba = new Double[1];
        proba[0] = 1.0d;
        group = new Set[1];
        group[0] = new HashSet<CollusionGroup>();
    }

    @Test public void worker() {
        proba[0] = 0.5d;
        Switcher<Double> rel = new Switcher<Double>(proba, new double[0], new double[0]);
        group[0].add(new CollusionGroup(0.5d, ""));
        Switcher<Set<CollusionGroup>> col
            = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        new Worker("worker", rel, col);
    }

    @Test(expected=IllegalArgumentException.class)
        public void workerException() {
        proba[0] = 0.5d;
        Switcher<Double> rel = new Switcher<Double>(proba, new double[0], new double[0]);
        group[0].add(new CollusionGroup(0.5d, ""));
        Switcher<Set<CollusionGroup>> col
            = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        group[0].add(new CollusionGroup(0.1d, ""));
        new Worker("worker", rel, col);
    }

    @Test public void getResult() {
        proba[0] = 0.5d;
        Switcher<Double> rel = new Switcher<Double>(proba, new double[0], new double[0]);
        group[0].add(new CollusionGroup(0.5d, ""));
        Switcher<Set<CollusionGroup>> col
            = new Switcher<Set<CollusionGroup>>(group, new double[0], new double[0]);
        Worker worker = new Worker("worker", rel, col);
        int numberCorrect = 0, numberFailed = 0, numberCollude = 0;
        for (int i=0; i<1000; i++) {
            worker.submitJob(new Job(""));
            final Result result = worker.getResult(0);
            switch (result.getType()) {
                case CORRECT:
                    numberCorrect++;
                    break;
                case FAILED:
                    numberFailed++;
                    break;
                case BUG:
                    numberCollude++;
                    break;
            }
        }
        assertEquals(0, numberCorrect);
        assertTrue("Some result should be failure", numberFailed >= 20);
        assertTrue("Some result should be collusion", numberCollude >= 20);
    }

}
