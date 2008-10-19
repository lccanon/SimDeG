package simdeg.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class TestCollections {

    @Test public void getRandomSubGroupSet() {
        Set<Object> set = new HashSet<Object>();
        for (int i=0; i<5; i++)
            set.add(new Object());
        for (int i=0; i<5; i++) {
            Set<Object> subSet = Collections.getRandomSubGroup(i+1, set,
                    new MersenneTwisterFast(0L));
            assertEquals(subSet.size(), i+1);
            assertTrue(set.containsAll(subSet));
        }
    }

    @Test public void getRandomSubGroupList() {
        List<Object> list = new ArrayList<Object>();
        for (int i=0; i<5; i++)
            list.add(new Object());
        for (int i=0; i<5; i++) {
            List<Object> subList = Collections.getRandomSubGroup(i+1, list,
                    new MersenneTwisterFast(0L));
            assertEquals(subList.size(), i+1);
            assertTrue(list.containsAll(subList));
        }
    }

    @Test public void parseList() throws NoSuchMethodException {
        Double[] array = new Double[] {1.0d, 2.0d, 3.0d, 1.0d};
        String str = Arrays.asList(array).toString();
        List<Double> list = Collections.parseList(Double.class, str);
        assertArrayEquals((Object[])array, list.toArray(new Object[0]));
    }

    @Test(expected=NoSuchMethodException.class)
    public void parseListException() throws NoSuchMethodException {
        Object[] array = new Object[] {new Object(), new Object()};
        String str = Arrays.asList(array).toString();
        List<Object> list = Collections.parseList(Object.class, str);
        assertArrayEquals(array, list.toArray());
    }

}
