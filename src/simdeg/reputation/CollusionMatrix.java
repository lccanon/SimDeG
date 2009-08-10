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

//class CollusionMatrix extends DynamicMatrix<Worker> {
public class CollusionMatrix extends DynamicMatrix<Worker> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(CollusionMatrix.class.getName());

    protected CollusionMatrix(Estimator estimatorBase) {
        super(estimatorBase);
    }

    /**
     * Updates the matrix by considering a change in the set that is the
     * biggest.
     */
    protected final void readapt(Set<Worker> previousBiggest) {
        final Set<Worker> biggest = getBiggest();

        /* Test if there is a significant change in the structure of the
         * dominant set */        
        if (previousBiggest == biggest || biggest.containsAll(previousBiggest))
            return;
        /* If the current dominant set is thought to not collude, then the
         * readaptation will only bring loss of precision and not estimation
         * changes */
        if (getEstimator(biggest, biggest).getMean()
                < getEstimator(biggest, biggest).getError())
            return;
//System.out.println("Readapt");

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
                if (previousEstimates.get(set).get(otherSet).getMean()
                        == getEstimator(set, otherSet).getMean()) {
                    Estimator estimator = previousEstimates.get(set).get(otherSet).clone();
                    estimator.add(previousEstimates.get(biggest).get(biggest));
                    estimator.subtract(previousEstimates.get(biggest).get(set));
                    estimator.subtract(previousEstimates.get(biggest).get(otherSet));
                    estimator.truncateRange(0.0d, 1.0d);
//if (estimator.getError() < previousEstimates.get(set).get(otherSet).getError())
//    System.out.println(previousEstimates.get(set).get(otherSet) + " " + previousEstimates.get(biggest).get(biggest) + " " + previousEstimates.get(biggest).get(set) + " " + previousEstimates.get(biggest).get(otherSet) + " = " + previousEstimates.get(set).get(otherSet).clone().add(previousEstimates.get(biggest).get(biggest)).subtract(previousEstimates.get(biggest).get(set)).subtract(previousEstimates.get(biggest).get(otherSet)) + " -> " + estimator);
                    setEstimator(set, otherSet, estimator);
                }
    }

    protected final void increaseCollusion(Worker worker, Worker otherWorker) {
        final Set<Worker> previousBiggest = getBiggest();
        /* Test the possibility of removing workers from the dominant set */
        if (getBiggest().size() > 1 && getBiggest().contains(worker))
            split(getSet(worker), worker);
        if (getBiggest().size() > 1 && getBiggest().contains(otherWorker))
            split(getSet(otherWorker), otherWorker);
        /* Readapt if needed */
        readapt(previousBiggest);
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
        final double diff12 = Math.abs(getEstimator(set1, set1).getMean()
                - getEstimator(set1, set2).getMean());
        final double diff23 = Math.abs(getEstimator(set1, set2).getMean()
                - getEstimator(set2, set2).getMean());
        final double diff13 = Math.abs(getEstimator(set1, set1).getMean()
                - getEstimator(set2, set2).getMean());
        final double meanEstimate = (getEstimator(set1, set1).getMean()
                + getEstimator(set1, set2).getMean()
                + getEstimator(set1, set2).getMean()) / 3.0d;
        /* The multiplier alpha is used in order to limit the mergings of sets
         * whose size would exceed the biggest size except if the set are
         * non-colluders. The risk to allow colluding set to be merged sooner
         * is to merge them before the non-colluder. Value above 1.5 are
         * working. It should not however be too high, otherwise the
         * readaptation never happens. */
        double alpha = 1.0d;
        if (set1.size() + set2.size() > getBiggest().size()
                && meanEstimate > 1.0d / (2.0d + set1.size() + set2.size()))
            alpha = 2.1d;
        /* A single number of necessary observation for merging is not
         * sufficient, because we want to promote merging of small sets (where
         * it is likelely that more colluding workers are present) and limit it
         * for larger sets (the dominant set should be still) */
        // XXX take into account the difference between the product (1.0d -
        // diff12) * (1.0d - diff13) * (1.0d - diff23) and the best achievable
        // at this iteration
        final double sampleCountLimit = (set1.size() + set2.size()) * alpha
            / ((1.0d - diff12) * (1.0d - diff13) * (1.0d - diff23));
        if (diff12 < error1 + error2 && diff23 < error2 + error3 && diff13 < error1 + error3
                && getEstimator(set1, set1).getSampleCount() > sampleCountLimit
                && getEstimator(set1, set2).getSampleCount() > sampleCountLimit
                && getEstimator(set2, set2).getSampleCount() > sampleCountLimit) {
//System.out.println("Merge when " + getEstimator(set1, set1) + " " +
//        getEstimator(set1, set2) + " " + getEstimator(set2, set2) + " " +
//        getEstimator(set1, set2).getSampleCount() + " "
//        + sampleCountLimit + " " + set1 + " " + set2);
            final Set<Worker> previousBiggest = getBiggest();
            merge(set1, set2);
            /* Readapt if needed */
            readapt(previousBiggest);
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

    protected final int countNonColluder() {
        int count = 0;
        for (Set<Worker> set : getSets(getAll()))
            if (getEstimator(set, set).getMean() < 1.0d / (2.0d
                        + set.size()) && getEstimator(set, set).getMean() <
                    getEstimator(set, set).getError())
                count += set.size();
        return count;
    }

}
