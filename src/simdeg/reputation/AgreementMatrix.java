package simdeg.reputation;

import static simdeg.util.Collections.addElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import simdeg.util.Estimator;

class AgreementMatrix {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(AgreementMatrix.class.getName());

    private Map<Set<Worker>,Map<Set<Worker>,Estimator>> matrix
        = new HashMap<Set<Worker>,Map<Set<Worker>,Estimator>>();

    private Map<Worker,Set<Worker>> reverse = new HashMap<Worker,Set<Worker>>();

    private Set<Worker> biggest = null;

    private Estimator estimatorBase;

    /** Threshold below which a group is considering to not collude */
    private static final double AGREEMENT_THRESHOLD = 0.99d;

    /** 
     * Maximal error allowed for merging sets.
     */
    private static final double MAX_ERROR = 1.0d / 3.0d;

    AgreementMatrix(Estimator estimatorBase) {
        this.estimatorBase = estimatorBase;
    }

    Set<Worker> getSet(Worker worker) {
        if (reverse.containsKey(worker))
            return reverse.get(worker);
        else
            throw new IllegalArgumentException("Worker never intialized");
    }

    private Set<Set<Worker>> getSets(Set<? extends Worker> workers) {
        Set<Set<Worker>> sets = new HashSet<Set<Worker>>();
        for (Worker worker : workers)
            sets.add(getSet(worker));
        return sets;
    }

    private void addRowsWith(Set<Set<Worker>> sets) {
        /* Add new rows */
        for (Set<Worker> set : sets)
            matrix.put(set, new HashMap<Set<Worker>,Estimator>());
        /* Complete new rows */
        for (Set<Worker> set : sets)
            for (Set<Worker> otherSet : matrix.keySet()) {
                Estimator estimator = matrix.get(otherSet).get(set);
                if (estimator == null)
                    if (set == otherSet)
                        estimator = estimatorBase.clone(1.0d);
                    else
                        estimator = estimatorBase.clone(0.0d, 1.0d);
                matrix.get(set).put(otherSet, estimator);
            }
    }

    private void updateReverse(Set<Worker> workers) {
        for (Worker worker : workers)
            reverse.put(worker, workers);
        assert(checkReverse()) : "Reverse malformed";
    }

    private void updateBiggest() {
        for (Set<Worker> set : matrix.keySet())
            if (biggest == null || set.size() > biggest.size())
                biggest = set;
        if (biggest == null)
            biggest = new HashSet<Worker>();
    }

    void addAllWorkers(Set<? extends Worker> workers) {
        for (Worker worker : workers)
            reverse.put(worker, addElement(worker, new HashSet<Worker>()));
        Set<Set<Worker>> sets = getSets(workers);
        /* Complete existing rows */
        for (Set<Worker> set : matrix.keySet())
            for (Set<Worker> otherSet : sets)
                matrix.get(set).put(otherSet, estimatorBase.clone(0.0d, 1.0d));
        /* Add and complete last rows */
        addRowsWith(sets);
        biggest = null;
    }

    void removeAllWorkers(Set<? extends Worker> workers) {
        for (Set<Worker> set : new HashSet<Set<Worker>>(matrix.keySet())) {
            Set<Worker> newSet = new HashSet<Worker>(set);
            newSet.removeAll(workers);
            if (set.size() != newSet.size()) {
                copyAgreement(set, newSet);
                clean(set);
                updateReverse(newSet);
            }
        }
        for (Worker worker : workers)
            reverse.remove(worker);
        biggest = null;
    }

    private boolean checkReverse() {
        for (Set<Worker> set : matrix.keySet())
            for (Worker worker : set)
                if (reverse.get(worker) != set)
                    return false;
        return true;
    }

    private boolean checkMatrix() {
        for (Set<Worker> set : matrix.keySet())
            for (Set<Worker> otherSet : matrix.keySet())
                if (matrix.get(set).get(otherSet) == null)
                    return false;
        return true;
    }

    private void copyAgreement(Set<Worker> intialSet, Set<Worker> newSet) {
        if (newSet.isEmpty())
            return;
         /* Complete matrix */
        matrix.put(newSet, new HashMap<Set<Worker>,Estimator>());
        for (Set<Worker> set : matrix.keySet()) {
            if (set == newSet)
                matrix.get(set).put(newSet, estimatorBase.clone(1.0d));
            else if (set == intialSet)
                matrix.get(set).put(newSet, estimatorBase.clone(0.0d, 1.0d));
            else
                matrix.get(set).put(newSet,
                        matrix.get(set).get(intialSet).clone());
            matrix.get(newSet).put(set, matrix.get(set).get(newSet));
        }
        assert(checkMatrix()) : "Matrix malformed";
    }

    private void copyAgreement(Set<Worker> set1, Set<Worker> set2,
            Set<Worker> newSet) {
        if (newSet.isEmpty())
            return;
         /* Complete matrix */
        matrix.put(newSet, new HashMap<Set<Worker>,Estimator>());
        for (Set<Worker> set : matrix.keySet()) {
            if (set == newSet)
                matrix.get(set).put(newSet, estimatorBase.clone(1.0d));
            else if (set == set1 || set == set2)
                matrix.get(set).put(newSet, estimatorBase.clone(0.0d, 1.0d));
            else if (matrix.get(set).get(set1).getError()
                    < matrix.get(set).get(set2).getError())
                matrix.get(set).put(newSet,
                        matrix.get(set).get(set1).clone());
            else
                matrix.get(set).put(newSet,
                        matrix.get(set).get(set2).clone());
            matrix.get(newSet).put(set, matrix.get(set).get(newSet));
        }
        assert(checkMatrix()) : "Matrix malformed";
    }

    private void clean(Set<Worker> remove) {
        for (Set<Worker> set : matrix.keySet())
            matrix.get(set).remove(remove);
        matrix.remove(remove);
        assert(checkMatrix()) : "Matrix malformed";
    }

    private void print() {
        for (Set<Worker> set1 : matrix.keySet()) {
            for (Set<Worker> set2 : matrix.keySet())
                System.out.print(" " + matrix.get(set1).get(set2));
            System.out.println();
        }
    }

    void increaseAgreement(Worker worker, Worker otherWorker) {
        final Set<Worker> set1 = reverse.get(worker);
        final Set<Worker> set2 = reverse.get(otherWorker);
        if (set1 == null || set2 == null)
            return;
        matrix.get(set1).get(set2).setSample(1.0d);
        /* Test the possibility of merging both sets */
        if (set1 != set2 && matrix.get(set1).get(set2).getEstimate()
                > AGREEMENT_THRESHOLD
                && matrix.get(set1).get(set2).getError() < MAX_ERROR) {
            logger.finer("Merging of set " + set1 + " and " + set2);
            /* Merge by putting set2 in set1 */
            Set<Worker> merge = new HashSet<Worker>();
            merge.addAll(set1);
            merge.addAll(set2);
            /* Complete matrix by duplicating the estimator with
             * best estimators */
            copyAgreement(set1, set2, merge);
            /* Clean matrix */
            clean(set1);
            clean(set2);
            /* Update reverse */
            updateReverse(merge);
            /* Update biggest */
            biggest = null;
        }
    }

    void decreaseAgreement(Worker worker, Worker otherWorker) {
        Set<Worker> set1 = reverse.get(worker);
        Set<Worker> set2 = reverse.get(otherWorker);
        if (set1 == null || set2 == null || worker == otherWorker)
            return;
        /* Test the possibility of scattering the current set */
        if (set1 != set2) {
            matrix.get(set1).get(set2).setSample(0.0d);
            return;
        }
        logger.finer("Scattering of workers " + worker + " and " + otherWorker
                + " in set " + set1);
        Set<Worker> initialSet = new HashSet<Worker>(set1);
        initialSet.remove(worker);
        initialSet.remove(otherWorker);
        /* Update sets that disagree */
        Set<Worker> newSet1 = addElement(worker, new HashSet<Worker>());
        Set<Worker> newSet2 = addElement(otherWorker, new HashSet<Worker>());
        /* Update matrix and copy the estimators between the initial set
         * and every other sets */
        copyAgreement(set1, newSet1);
        copyAgreement(set1, newSet2);
        copyAgreement(set1, initialSet);
        clean(set1);
        /* Put these single elements in new sets and update reverse */
        reverse.put(worker, newSet1);
        reverse.put(otherWorker, newSet2);
        updateReverse(initialSet);
        /* Update biggest */
        biggest = null;
        matrix.get(newSet1).get(newSet2).setSample(0.0d);
    }

    @SuppressWarnings("unchecked")
    Estimator[][] getCollusion(Set<? extends Worker> workers) {
        List<Set<Worker>> sets = new ArrayList<Set<Worker>>(getSets(workers));
        Estimator[][] result = new Estimator[sets.size()][sets.size()];
        for (int j=0; j<result[0].length; j++)
            result[0][j] = matrix.get(getBiggest()).get(sets.get(j));
        for (int i=1; i<result.length; i++)
            for (int j=0; j<result[i].length; j++)
                result[i][j] = matrix.get(sets.get(i-1)).get(sets.get(j));
        return result;
    }

    Set<Worker> getBiggest() {
        if (biggest == null || biggest.isEmpty())
        updateBiggest();
        return biggest;
    }

    double getBiggestError() {
        double result = 0.0d;
        for (Set<Worker> set : matrix.keySet())
            result += matrix.get(getBiggest()).get(set).getError();
        result /= matrix.keySet().size();
        return result;
    }

}
