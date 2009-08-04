package simdeg.reputation.simulation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestJob {

    @Test public void job() {
        new Job("test");
    }

    @Test public void equals() {
        Job job1 = new Job("test");
        Job job2 = new Job("tes");
        Job job3 = new Job("test");
        assertTrue("Jobs should be equal", job1.equals(job1));
        assertFalse("Jobs shouldn't be equal", job1.equals(job2));
        assertTrue("Jobs should be equal", job1.equals(job3));
    }

    @Test public void duplicate() {
        Job job = new Job("test");
        Job duplicate = job.duplicate();
        assertTrue("Jobs should be equal", job.equals(duplicate));
    }

}
