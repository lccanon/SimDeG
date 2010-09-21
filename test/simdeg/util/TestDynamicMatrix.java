package simdeg.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDynamicMatrix {

    private static Set<Object> objects1;
    private static Set<Object> objects2;
    private static Set<Object> objects;

    private DynamicMatrix<Object> matrix;

    @BeforeClass public static void createObjects() {
        objects1 = new HashSet<Object>();
        for (int i=0; i<10; i++) {
            final int hash = i * 2;
            objects1.add(new Object() {public int hashCode() { return hash; }});
        }
        objects2 = new HashSet<Object>();
        for (int i=0; i<29; i++) {
            final int hash = i * 2 + 1;
            objects2.add(new Object() {public int hashCode() { return hash; }});
        }
        objects = new HashSet<Object>();
        objects.addAll(objects1);
        objects.addAll(objects2);
    }

    @Before public void createMatrix() {
        matrix = new DynamicMatrix<Object>(new BTS());
    }

    @Test public void addAll() {
        matrix.addAll(objects1);
        matrix.addAll(objects2);
        /* Add twice without exception */
        matrix.addAll(objects1);
    }

    @Test public void removeAll() {
        matrix.removeAll(objects1);
        matrix.addAll(objects1);
        matrix.removeAll(objects1);
        /* Remove only a part */
        matrix.addAll(objects1);
        matrix.addAll(objects2);
        matrix.removeAll(objects1);
        /* Remove twice without exception */
        matrix.removeAll(objects1);
    }

    @Test public void getAll() {
        assertEquals(0, matrix.getAll().size());
        matrix.addAll(objects1);
        assertEquals(objects1.size(), matrix.getAll().size());
        matrix.addAll(objects2);
        assertEquals(objects.size(), matrix.getAll().size());
        /* Add twice without impact */
        matrix.addAll(objects1);
        assertEquals(objects.size(), matrix.getAll().size());
        /* Remove only a part */
        matrix.removeAll(objects1);
        assertEquals(objects2.size(), matrix.getAll().size());
        /* Remove twice without impact */
        matrix.removeAll(objects1);
        assertEquals(objects2.size(), matrix.getAll().size());
        /* Remove all */
        matrix.removeAll(objects2);
        assertEquals(0, matrix.getAll().size());
    }

    @Test public void getSets() {
        matrix.addAll(objects);
        final Set<Set<Object>> sets = matrix.getSets(objects);
        assertEquals(objects.size(), sets.size());
        /* Get only part of the sets */
        final Set<Set<Object>> sets1 = matrix.getSets(objects1);
        assertEquals(objects1.size(), sets1.size());
        /* Perform merges of all objects from first set */
        final Object object1 = objects1.iterator().next();
        for (Object object : objects1)
            matrix.merge(matrix.getSet(object1), matrix.getSet(object));
        final Set<Set<Object>> sets1AfterMerge = matrix.getSets(objects1);
        assertEquals(1, sets1AfterMerge.size());
    }

    @Test public void getEstimator() {
        matrix.addAll(objects);
        final Set<Object> set1 = matrix.getSet(objects1.iterator().next());
        final Set<Object> set2 = matrix.getSet(objects2.iterator().next());
        assertSame(matrix.getEstimator(set1, set2),
                matrix.getEstimator(set2, set1));
    }

    @Test(expected=NoSuchElementException.class)
    public void getEstimatorException() {
        matrix.addAll(objects1);
        final Set<Object> set1 = matrix.getSet(objects1.iterator().next());
        final Set<Object> set2 = matrix.getSet(objects2.iterator().next());
        matrix.getEstimator(set1, set2);
    }

    @Test public void setEstimator() {
        matrix.addAll(objects);
        final Set<Object> set1 = matrix.getSet(objects1.iterator().next());
        final Set<Object> set2 = matrix.getSet(objects2.iterator().next());
        final Estimator estimator = new BTS();
        matrix.setEstimator(set1, set2, estimator);
        assertSame(estimator, matrix.getEstimator(set1, set2));
    }

    @Test(expected=NoSuchElementException.class)
    public void setEstimatorException() {
        matrix.addAll(objects1);
        final Set<Object> set1 = matrix.getSet(objects1.iterator().next());
        final Set<Object> set2 = matrix.getSet(objects2.iterator().next());
        final Estimator estimator = new BTS();
        matrix.setEstimator(set1, set2, estimator);
    }

    @Test public void getBiggest() {
        assertTrue(matrix.getLargest().isEmpty());
        matrix.addAll(objects1);
        assertFalse(matrix.getLargest().isEmpty());
        /* Force an update of the biggest */
        matrix.addAll(objects2);
        matrix.removeAll(objects1);
        assertFalse(matrix.getLargest().isEmpty());
        /* Remove all objects */
        matrix.removeAll(objects2);
        assertTrue(matrix.getLargest().isEmpty());
        /* Perform merges of all objects from first set */
        matrix.addAll(objects);
        final Object object1 = objects1.iterator().next();
        for (Object object : objects1)
            matrix.merge(matrix.getSet(object1), matrix.getSet(object));
        assertEquals(objects1.size(), matrix.getLargest().size());
    }

    @Test public void merge() {
        matrix.addAll(objects);
        /* Merge all object from first set */
        final Object object1 = objects1.iterator().next();
        for (Object object : objects1) {
            matrix.merge(matrix.getSet(object1), matrix.getSet(object));
            if (object != object1)
                assertSame(matrix.getSet(object1), matrix.getLargest());
            assertEquals(matrix.getSets(objects).size(),
                    objects.size() - matrix.getSet(object1).size() + 1);
        }
        /* Merge all object from second set */
        final Object object2 = objects2.iterator().next();
        for (Object object : objects2)
            matrix.merge(matrix.getSet(object2), matrix.getSet(object));
        /* Merge all objects */
        matrix.merge(matrix.getSet(object1), matrix.getSet(object2));
        assertEquals(objects.size(), matrix.getSet(object2).size());
        /* Remove part of the objects */
        matrix.removeAll(objects1);
        assertEquals(objects2.size(), matrix.getSet(object2).size());
    }

    @Test public void split() {
        matrix.addAll(objects);
        final Object object1 = objects1.iterator().next();
        for (Object object : objects)
            matrix.merge(matrix.getSet(object1), matrix.getSet(object));
        for (Object object : objects2) {
            matrix.split(matrix.getSet(object1), object);
            assertSame(matrix.getSet(object1), matrix.getLargest());
            assertEquals(matrix.getSets(objects).size(),
                    objects.size() - matrix.getSet(object1).size() + 1);
        }
        /* Remove part of the objects */
        matrix.removeAll(objects1);
        assertEquals(1, matrix.getLargest().size());
    }

}
