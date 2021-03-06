package simdeg.util;

import static simdeg.util.Collections.addElement;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Matrix containing informations between sets of element. Splitting and
 * merging related operations are available.
 */
public class DynamicMatrix<E> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(DynamicMatrix.class.getName());

    /** Matrix containing an estimator for each pair of sets */
    private final Map<Set<E>,Map<Set<E>,Estimator>> matrix
        = new HashMap<Set<E>,Map<Set<E>,Estimator>>();

    /** Optimization members */
    private final Map<E,Set<E>> reverse = new HashMap<E,Set<E>>();

    /** Biggest set */
    private Set<E> largest = null;

    /** Estimator that will be cloned everywhere (directly on the diagonal) */
    private final Estimator estimatorBase;

    protected DynamicMatrix(Estimator estimatorBase) {
        this.estimatorBase = estimatorBase;
    }

    public void addAll(Collection<? extends E> elements) {
        /* Create new singleton sets and update reverse */
        for (E element : elements)
            if (!reverse.containsKey(element))
                reverse.put(element, addElement(element, new HashSet<E>()));
        /* Add and complete last rows */
        Set<Set<E>> sets = getSets(elements);
        for (Set<E> set : sets)
            insertSet(set);
        largest = null;
    }

    public void removeAll(Collection<? extends E> elements) {
        Collection<E> elementsToRemove = new HashSet<E>();
        for (E element : elements)
            if (reverse.containsKey(element))
                elementsToRemove.add(element);
        /* For each concerned set */
        final Set<Set<E>> sets = getSets(elementsToRemove);
        for (Set<E> set : sets) {
            /* In the case the set should completely disappear */
            if (elements.containsAll(set)) {
                clean(set);
                continue;
            }
            /* Build new smaller set */
            final Set<E> newSet = new HashSet<E>(set);
            newSet.removeAll(elements);
            /* Insert it with correct values */
            insertSet(newSet);
            copyEstimator(set, newSet);
            /* Update and clean data structures */
            clean(set);
            updateReverse(newSet);
        }
        /* Update reverse and biggest */
        for (E element : elements)
            reverse.remove(element);
        largest = null;
    }

    public Set<E> getAll() {
        return reverse.keySet();
    }

    public Set<E> getSet(E element) {
        if (reverse.containsKey(element))
            return reverse.get(element);
        else
            throw new NoSuchElementException("Element never initialized");
    }

    public Set<Set<E>> getSets(Collection<? extends E> elements) {
        Set<Set<E>> sets = new HashSet<Set<E>>();
        for (E element : elements)
            sets.add(getSet(element));
        return sets;
    }

    @SuppressWarnings("unchecked")
    protected void setEstimator(Set<E> set1,
            Set<E> set2, Estimator estimator) {
        testValidSet(set1, set2);
        if (estimator == null)
            throw new NullPointerException("Specified estimator is null");
        if (estimator.getLowerEndpoint() != 0.0d
                || estimator.getUpperEndpoint() != 1.0d)
            throw new IllegalArgumentException(
                    "Estimator not in valid boundaries: " + estimator);
        matrix.get(set1).put(set2, estimator);
        matrix.get(set2).put(set1, estimator);
    }

    @SuppressWarnings("unchecked")
    protected Estimator getEstimator(Set<E> set1,
            Set<E> set2) {
        testValidSet(set1, set2);
        return matrix.get(set1).get(set2);
    }

    public Set<E> getLargest() {
        if (largest == null || largest.isEmpty())
            updateLargest();
        return largest;
    }

    /* TODO getGeneralError()
     * Based on the number of groups, or the size of the biggest group, or the
     * mean of each error (can be done in O(1) if it is updated each time an
     * estimator is changed).
     */
    /**
     * Gives a quick error indications of the current estimations.
     */
    public double getGeneralError() {
        double result = 0.0d;
        for (Set<E> set : matrix.keySet())
            result += getEstimator(getLargest(), set).getError();
        result /= matrix.keySet().size();
        return result;
    }

    /**
     * Merges the two sets if they are not already contained one in the other.
     */
    @SuppressWarnings("unchecked")
    public Set<E> merge(Set<E> set1, Set<E> set2) {
        testValidSet(set1, set2);
        if (set1.containsAll(set2) || set2.containsAll(set1))
            return null;
        logger.fine("Merging of sets " + set1 + " and " + set2);
        /* Merge by putting set2 in set1 */
        Set<E> merge = new HashSet<E>();
        merge.addAll(set1);
        merge.addAll(set2);
        /* Insert the new set into the matrix */
        insertSet(merge);
        /* Duplicating the estimator with best estimators */
        copyEstimator(set1, set2, merge);
        /* Clean matrix by removing first sets */
        clean(set1);
        clean(set2);
        /* Update reverse for all concerned elements */
        updateReverse(merge);
        /* Postpone the update of biggest */
        if (largest != null && merge.size() > largest.size())
            largest = null;
        return merge;
    }

    /**
     * Splits the specified element from the set.
     */
    @SuppressWarnings("unchecked")
    public void split(Set<E> set, E element) {
        testValidSet(set);
        if (set.size() == 1 || !set.contains(element))
            return;
        logger.fine("Splitting of element " + element + " from set " + set);
        /* Create a new set without the element */
        Set<E> initialSet = new HashSet<E>(set);
        initialSet.remove(element);
        /* Create single set for the incriminated element */
        Set<E> newSet = addElement(element, new HashSet<E>());
        /* Insert these new sets into the matrix */
        insertSet(initialSet);
        insertSet(newSet);
        /* At this point, the previous estimators were copied. But, it is now
         * useless. */
        /* Clean matrix by removing the first set */
        clean(set);
        /* Update reverse for all concerned elements */
        reverse.put(element, newSet);
        updateReverse(initialSet);
        /* Postpone the update of biggest */
        if (largest == set)
            largest = null;
    }

    /**
     * Tests the validity of the set (its presence in the matrix).
     */
    protected void testValidSet(Set<E>... sets) {
        for (Set<E> set : sets)
            if (!matrix.containsKey(set))
                throw new NoSuchElementException("The considered set is not present in the matrix: "
                        + Arrays.toString(set.toArray()));
    }

    /**
     * Inserts newly created sets.
     */
    private void insertSet(Set<E> set) {
        if (set.isEmpty())
            return;
        /* Add new row */
        matrix.put(set, new HashMap<Set<E>,Estimator>());
        for (Set<E> otherSet : matrix.keySet()) {
            /* Special initial value for diagonal elements */
            if (set == otherSet)
                setEstimator(set, otherSet, estimatorBase.clone());
            /* Uncertainty for others */
            else
                setEstimator(set, otherSet, estimatorBase.clone().clear());
        }
        assert(checkMatrix()) : "Matrix malformed";
    }

    /**
     * Gives to the inserted set the cloned estimators of the initial set.
     */
    private void copyEstimator(Set<E> initialSet, Set<E> newSet) {
        for (Set<E> set : matrix.keySet())
            if (set != initialSet && set != newSet)
                setEstimator(newSet, set, getEstimator(initialSet, set).clone());
        setEstimator(newSet, newSet, getEstimator(initialSet, initialSet).clone());
    }

    /**
     * Inserts the new set that is similar to set1 or set2 (those with the
     * best estimation).
     */
    private void copyEstimator(Set<E> set1, Set<E> set2,
            Set<E> newSet) {
        for (Set<E> set : matrix.keySet())
            if (set != newSet && set != set1 && set != set2) {
                final Estimator merge = Estimator.merge(getEstimator(set1, set),
                        getEstimator(set2, set));
                setEstimator(newSet, set, merge);
            }
        final Estimator merge = Estimator.merge(getEstimator(set1, set1),
                getEstimator(set2, set2)).merge(getEstimator(set1, set2));
        //System.out.println(getEstimator(set1, set1) + " " + getEstimator(set2, set2) + " " + getEstimator(set1, set2) + " " + merge);
        setEstimator(newSet, newSet, merge);
    }

    private void updateReverse(Set<E> elements) {
        for (E element : elements)
            reverse.put(element, elements);
        assert (checkReverse()) : "Reverse malformed";
    }

    private void updateLargest() {
        for (Set<E> set : matrix.keySet())
            if (largest == null || set.size() > largest.size())
                largest = set;
        if (largest == null)
            largest = new HashSet<E>();
    }

    private void clean(Set<E> remove) {
        for (Set<E> set : matrix.keySet())
            matrix.get(set).remove(remove);
        matrix.remove(remove);
        assert (checkMatrix()) : "Matrix malformed";
    }

    private boolean checkReverse() {
        for (Set<E> set : matrix.keySet())
            for (E element : set)
                if (reverse.get(element) != set)
                    return false;
        return true;
    }

    private boolean checkMatrix() {
        for (Set<E> set : matrix.keySet())
            for (Set<E> otherSet : matrix.keySet())
                if (getEstimator(set, otherSet) == null)
                    return false;
        return true;
    }

    public String toString() {
        StringBuilder stbuild = new StringBuilder();
        for (Set<E> set1 : matrix.keySet())
            stbuild.append("(" + set1.size() + "): "
                    + Arrays.toString(set1.toArray()) + " = "
                    + Arrays.toString(matrix.get(set1).values().toArray())
                    + '\n');
        return stbuild.toString();
    }

}
