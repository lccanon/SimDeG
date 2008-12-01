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
    private static final double MAX_ERROR = 0.2d;

    protected CollusionMatrix(Estimator estimatorBase) {
        super(estimatorBase);
    }

    protected final void readapt() {
        final Set<Worker> biggest = getBiggest();
        if (getEstimator(biggest, biggest).getEstimate() < getEstimator(biggest, biggest).getError()
                && getEstimator(biggest, biggest).getError() < MAX_ERROR)
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
                if (getEstimator(set, otherSet).getEstimate() == previousEstimates.get(set).get(otherSet).getEstimate()) {
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

    private final void testMerge(Set<Worker> set1, Set<Worker> set2) {
        if (set1 == set2)
            return;
        final double error1 = getEstimator(set1, set2).getError() + getEstimator(set1, set1).getError();
        final double error2 = getEstimator(set1, set2).getError() + getEstimator(set2, set2).getError();
        final double error3 = getEstimator(set1, set1).getError() + getEstimator(set2, set2).getError();
        final double diff1 = getEstimator(set1, set2).getEstimate() - getEstimator(set1, set1).getEstimate();
        final double diff2 = getEstimator(set1, set2).getEstimate() - getEstimator(set2, set2).getEstimate();
        final double diff3 = getEstimator(set1, set1).getEstimate() - getEstimator(set2, set2).getEstimate();
        if (Math.abs(diff1) < error1 && Math.abs(diff2) < error2 && Math.abs(diff3) < error3
                && error1 < MAX_ERROR && error2 < MAX_ERROR && error3 < MAX_ERROR) {
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
