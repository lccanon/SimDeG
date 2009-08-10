package simdeg.reputation.simulation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestJob {

    @Test public void job() {
        new Job();
    }

    @Test public void equals() {
        Job job1 = new Job();
        Job job2 = new Job();
        assertTrue("Jobs should be equal", job1.equals(job1));
        assertFalse("Jobs shouldn't be equal", job1.equals(job2));
    }

    @Test public void duplicate() {
        Job job = new Job();
        Job duplicate = job.duplicate();
        assertTrue("Jobs should be equal", job.equals(duplicate));
    }

}
