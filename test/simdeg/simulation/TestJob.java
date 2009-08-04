package simdeg.simulation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import simgrid.msg.JniException;
import simgrid.msg.Msg;
import simgrid.msg.NativeException;

public class TestJob {

    @BeforeClass public static void msgInit() {
        Msg.init(null);
    }

    @Test public void job() throws JniException, NativeException {
        new Job("test", 123d, null, 12d);
    }

    @Test public void equals() throws JniException, NativeException {
        Job job1 = new Job("test", 123d, null, 12d);
        Job job2 = new Job("tes", 123d, null, 12d);
        Job job3 = new Job("test", 12d, null, 1d);
        assertTrue(job1.equals(job1));
        assertFalse(job1.equals(job2));
        assertTrue(job1.equals(job3));
    }

    @Test public void duplicate() throws JniException, NativeException {
        Job job = new Job("test", 123d, null, 12d);
        Job duplicate = job.duplicate(null);
        assertTrue(job.equals(duplicate));
    }

}
