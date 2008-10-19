package simdeg.scheduling;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.reputation.OptimisticGridCharacteristics;
import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;
import simdeg.util.BTS;
import simdeg.util.Estimator;

public class TestCollusionAwareResultCertificator {

    private static Set<Worker> workers;
    
    private static List<Worker> workersList;
    
    private static List<Result> results;

    private static CollusionAwareResultCertificator answerSelector;

    static class TestResult implements Result {
        boolean k;
        TestResult(boolean k) { this.k = k; }
        @SuppressWarnings("unchecked")
        public boolean equals(Object ans) {
            return ((TestResult)ans).k == k;
        }
        public int hashCode() {
            return k?0:1;
        }
    }

    @BeforeClass public static void createWorkers() {
        workers = new HashSet<Worker>();
        for (int i=0; i<100; i++)
            workers.add(new Worker() {});
        workersList = new ArrayList<Worker>();
        results = new ArrayList<Result>();
        for (int i=0; i<10; i++) {
        	workersList.add(workers.toArray(new Worker[0])[i]);
        	results.add(new TestResult(i<4));
        }
        answerSelector = new CollusionAwareResultCertificator();
    }

    @SuppressWarnings("unchecked")
    @Test public void selectBestAnswer() throws ResultCertificationException {
        ReputationSystem gc = new OptimisticGridCharacteristics() {
            public Estimator getCollusionLikelihood(Set<Worker> workers) {
                if (workers.size() == 6)
                    return new BTS(0.5d);
                else
                    return new BTS(0.01d - 1E-3d);
            }
        };
        answerSelector.setReputationSystem(gc);
        assertTrue(((TestResult)answerSelector.selectBestResult(workersList, results)).k);;
    }

    @SuppressWarnings("unchecked")
    @Test(expected=ResultCertificationException.class)
    public void selectBestAnswerException() throws ResultCertificationException {
        ReputationSystem gc = new OptimisticGridCharacteristics();
        answerSelector.setReputationSystem(gc);
        answerSelector.selectBestResult(workersList.subList(0,1), results.subList(0,1));
    }

}
