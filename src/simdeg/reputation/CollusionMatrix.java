package simdeg.reputation;

import simdeg.util.Estimator;
import simdeg.util.DynamicMatrix;
import static simdeg.util.Estimator.max;
import static simdeg.util.Estimator.min;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

final class CollusionMatrix extends DynamicMatrix<Worker> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(CollusionMatrix.class.getName());

    /** 
     * Maximal error allowed for merging sets.
     */
    private static final double MAX_ERROR = 0.22d;

    private static final double MIN_ERROR = MAX_ERROR / 2.0d;

    protected CollusionMatrix(Estimator estimatorBase) {
        super(estimatorBase);
    }

    /**
     * Updates the matrix by considering a change in the set that is the
     * biggest.
     */
    protected final void readapt() {
        final Set<Worker> biggest = getBiggest();
        /* Use MAX_ERROR as a substitute constant for defining a threshold
         * below which the current biggest set is not a colluder */
        if (getEstimator(biggest, biggest).getEstimate() < getEstimator(biggest, biggest).getError()
                && getEstimator(biggest, biggest).getError() < MIN_ERROR)
            return;
        /* Store all previous values of the matrix */
        Map<Set<Worker>, Map<Set<Worker>, Estimator>> previousEstimates
            = new HashMap<Set<Worker>, Map<Set<Worker>, Estimator>>();
        final Set<Set<Worker>> sets = getSets(getAll());
        for (Set<Worker> set : sets) {
            previousEstimates.put(set, new HashMap<Set<Worker>, Estimator>());
            for (Set<Worker> otherSet : sets) {
                previousEstimates.get(set).put(otherSet,
                        getEstimator(set, otherSet).clone());
            }
        }
        /* Update current value with readaptation formula */
        for (Set<Worker> set : sets)
            for (Set<Worker> otherSet : sets)
                if (previousEstimates.get(set).get(otherSet).getEstimate()
                        == getEstimator(set, otherSet).getEstimate()) {
                    Estimator estimator = previousEstimates.get(set).get(otherSet).clone();
                    estimator.add(previousEstimates.get(biggest).get(biggest));
                    estimator.subtract(previousEstimates.get(biggest).get(set));
                    estimator.subtract(previousEstimates.get(biggest).get(otherSet));
                    estimator.setRange(0.0d, 1.0d);
                    setEstimator(set, otherSet, estimator);
                }
    }

    protected final void increaseCollusion(Worker worker, Worker otherWorker) {
        final Set<Worker> biggest = getBiggest();
        /* Test the possibility of removing workers from the dominant set */
        if (getBiggest().contains(worker))
            split(getSet(worker), worker);
        if (getBiggest().contains(otherWorker))
            split(getSet(otherWorker), otherWorker);
        /* Test wether to readapt or not */
        if (biggest != getBiggest())
            readapt();
        /* Proceed to estimator update */
        final Set<Worker> set1 = getSet(worker);
        final Set<Worker> set2 = getSet(otherWorker);
        if (set1 == null || set2 == null)
            return;
        getEstimator(set1, set2).setSample(1.0d);
        /* Test the possibility of merging both sets */
        testMerge(set1, set2);
    }

    protected final void decreaseCollusion(Worker worker, Worker otherWorker) {
        final Set<Worker> set1 = getSet(worker);
        final Set<Worker> set2 = getSet(otherWorker);
        if (set1 == null || set2 == null)
            return;
        /* Update estimator */
        getEstimator(set1, set2).setSample(0.0d);
        /* Test the possibility of merging both sets */
        testMerge(set1, set2);
    }

    /**
     * Tests the relevance of merging 2 sets.
     */
    private final void testMerge(Set<Worker> set1, Set<Worker> set2) {
        if (set1 == set2)
            return;
        final double error1 = getEstimator(set1, set1).getError();
        final double error2 = getEstimator(set1, set2).getError();
        final double error3 = getEstimator(set2, set2).getError();
        final double diff1 = getEstimator(set1, set1).getEstimate() - getEstimator(set1, set2).getEstimate();
        final double diff2 = getEstimator(set1, set2).getEstimate() - getEstimator(set2, set2).getEstimate();
        final double diff3 = getEstimator(set1, set1).getEstimate() - getEstimator(set2, set2).getEstimate();
        final double meanEstimate = (getEstimator(set1, set1).getEstimate()
                + getEstimator(set1, set2).getEstimate()
                + getEstimator(set1, set2).getEstimate()) / 3.0d;
        final double weightedError = Math.max(MIN_ERROR, MAX_ERROR * 4.0d * meanEstimate * (1.0d - meanEstimate)
            / Math.log(Math.min(set1.size(), set2.size()) + Math.E - 1.0d));
        if (Math.abs(diff1) < error1 + error2 && Math.abs(diff2) < error2 + error3 && Math.abs(diff3) < error1 + error3
                && error1 < weightedError && error2 < weightedError && error3 < weightedError) {
            final Set<Worker> biggest = getBiggest();
            merge(set1, set2);
            /* Test wether to readapt or not */
            if (biggest != getBiggest())
                readapt();
        }
    }

    @SuppressWarnings("unchecked")
    protected final Estimator[][] getCollusions(Set<? extends Worker> workers) {
        final List<Set<Worker>> sets = new ArrayList<Set<Worker>>(getSets(workers));
        Estimator[][] result = new Estimator[sets.size()][sets.size()];
        for (int i=0; i<result.length; i++)
            for (int j=0; j<result[i].length; j++)
                result[i][j] = getEstimator(sets.get(i), sets.get(j));
        return result;
    }

}
