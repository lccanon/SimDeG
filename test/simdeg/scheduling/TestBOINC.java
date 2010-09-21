package simdeg.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.reputation.Job;
import simdeg.reputation.Result;
import simdeg.reputation.Worker;

/**
 * Test that the BOINC scheduler work for simple cases. It ensures that the
 * {@link Scheduler} class is correct.
 */
public class TestBOINC implements SchedulerListener {

	Map<Job, Result> certifiedResult = null;

	@Override
	public void endOfJobQueue() {
		// TODO test event
	}

	@Override
	public <J extends Job, R extends Result> void setCertifiedResult(
			VotingPool<R> votingPool, R result) {
		certifiedResult.put(votingPool.getJob(), result);
	}

	private Result getCertifiedResult(Job job) {
		if (!certifiedResult.containsKey(job))
			return null;
		return certifiedResult.get(job);
	}

	/* To be ignored if the output has to be analyzed */
	@BeforeClass
	public static void desactivateLogger() {
		final Logger logger = Logger.getLogger(Scheduler.class.getName());
		logger.setLevel(Level.OFF);
	}

	@Before
	public void initCertifiedResult() {
		certifiedResult = new HashMap<Job, Result>();
	}

	/**
	 * Tests that the mechanism works for a simple iteration.
	 */
	@Test
	public void oneJobOneWorker() {
		/* One worker, one job, one result and no duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				1, 1, 1);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/* Before the scheduler knows the job */
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker, null, null);
		assertNull("No job must be available", pulledJob1);

		/*
		 * After the scheduler knows the job, it must return it without
		 * certifying any result
		 */
		scheduler.addJob(job);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker, null, null);
		assertEquals("Jobs must be the same", job, pulledJob2);

		/* Another worker has no access to any job */
		Worker otherWorker = new Worker() {
		};
		Job pulledJob3 = scheduler.submitResultAndPullJob(otherWorker, null,
				null);
		assertNull("No job must be available", pulledJob3);

		/* After the result is returned, it must certified it */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker, pulledJob2,
				result);
		assertNull("No job must be available", pulledJob4);
		assertEquals("Results must be the same", result,
				getCertifiedResult(job));

		/* Another worker has no access to any job */
		Job pulledJob5 = scheduler.submitResultAndPullJob(otherWorker, null,
				null);
		assertNull("No job must be available", pulledJob5);
	}

	/**
	 * Tests that the mechanism detects forbidden situations.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void notAssignedJobException() {
		/* One worker, one job, one result and no duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				1, 1, 1);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/* The worker may not submit a result for this job */
		scheduler.addJob(job);
		scheduler.submitResultAndPullJob(worker, job, result);
	}

	/**
	 * Tests that the mechanism detects forbidden situations.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void multipleResultsException() {
		/* One worker, one job, one result and no duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				1, 1, 1);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/* The worker may not submit two results for this job */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker, pulledJob1,
				result);
		assertNull("No job must be available", pulledJob2);
		scheduler.submitResultAndPullJob(worker, pulledJob1, result);
	}

	/**
	 * Tests that the mechanism detects forbidden situations.
	 */
	@Test(expected = AssertionError.class)
	public void simultaneousComputationException() {
		/* One worker, one job, one result and no duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				1, 1, 1);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker = new Worker() {
		};

		/* The worker may not ask a second job */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);
		scheduler.submitResultAndPullJob(worker, null, null);
	}

	/**
	 * Tests that the mechanism works when there are two workers.
	 */
	@Test
	public void oneJobTwoWorkersOverlapping() {
		/* Two workers, one job, one result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				2, 2, 3);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/*
		 * After the scheduler knows the job, it must return it without
		 * certifying any result
		 */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker2, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);
		assertEquals("Jobs must be the same", job, pulledJob2);

		/* Another worker has no access to any job */
		Worker worker3 = new Worker() {
		};
		Job pulledJob3 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertNull("No job must be available", pulledJob3);

		/* After one result is returned, it must not certify it */
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result);
		assertNull("No job must be available", pulledJob4);

		/* After both results are returned, it must certify it */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
		Job pulledJob5 = scheduler.submitResultAndPullJob(worker2, pulledJob2,
				result);
		assertNull("No job must be available", pulledJob5);
		assertEquals("Results must be the same", result,
				getCertifiedResult(job));

		/* Another worker has no access to any job */
		Job pulledJob6 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertNull("No job must be available", pulledJob6);
	}

	/**
	 * Tests that the mechanism works when there are two workers that appears
	 * successively.
	 */
	@Test
	public void oneJobTwoWorkersSeparated() {
		/* Two workers, one job, one result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				2, 2, 3);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/*
		 * After the scheduler knows the job, it must return it without
		 * certifying any result
		 */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);

		/* After one result is returned, it must not certify it */
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result);
		assertNull("No job must be available", pulledJob2);

		/* After the second worker comes into play, it must not certify it */
		Job pulledJob3 = scheduler.submitResultAndPullJob(worker2, null, null);
		assertEquals("Jobs must be the same", job, pulledJob3);

		/* After both results are returned, it must certify it */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker2, pulledJob3,
				result);
		assertNull("No job must be available", pulledJob4);
		assertEquals("Results must be the same", result,
				getCertifiedResult(job));

		/* Another worker has no access to any job */
		Worker worker3 = new Worker() {
		};
		Job pulledJob5 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertNull("No job must be available", pulledJob5);
	}

	/**
	 * Tests that the mechanism works when there is one worker that comes and
	 * quit while the others is still processing.
	 */
	@Test
	public void oneJobTwoWorkersEncompassing() {
		/* Two workers, one job, one result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				3, 3, 3);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/*
		 * After the scheduler knows the job, it must return it without
		 * certifying any result
		 */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);

		/* After the second worker comes into play, it must not certify it */
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker2, null, null);
		assertEquals("Jobs must be the same", job, pulledJob2);
		Job pulledJob3 = scheduler.submitResultAndPullJob(worker2, pulledJob2,
				result);
		assertNull("No job must be available", pulledJob3);

		Job pulledJob4 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result);
		assertNull("No job must be available", pulledJob4);

		/* After both results are returned, it must not certify it */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
	}

	/**
	 * Tests that the mechanism duplicate disagreeing workers until the quorum
	 * is reached without reaching maximum.
	 */
	@Test
	public void oneJobSeveralWorkersUnreliableQuorum() {
		/* Several workers, one job, several result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				2, 2, 4);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Worker worker3 = new Worker() {
		};
		Result result1 = new Result() {
			public boolean equals(Object aResult) {
				return this == aResult;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Result result2 = new Result() {
			public boolean equals(Object aResult) {
				return this == aResult;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/*
		 * After the scheduler knows the job, it must return it without
		 * certifying any result
		 */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker2, null, null);
		Job pulledJob3 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);
		assertEquals("Jobs must be the same", job, pulledJob2);
		assertNull("No job must be available", pulledJob3);

		/* After two results are returned, it must not certify it */
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result1);
		Job pulledJob5 = scheduler.submitResultAndPullJob(worker2, pulledJob2,
				result2);
		assertNull("No job must be available", pulledJob4);
		assertNull("No job must be available", pulledJob5);

		/* After the third worker comes into play, it must not certify it */
		Job pulledJob6 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertEquals("Jobs must be the same", job, pulledJob6);

		/* After the last result is returned, it must certify it */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
		Job pulledJob7 = scheduler.submitResultAndPullJob(worker3, pulledJob6,
				result1);
		assertNull("No job must be available", pulledJob7);
		assertEquals("Results must be the same", result1,
				getCertifiedResult(job));
	}

	/**
	 * Tests that the mechanism manage majority. Duplicate until maximum and
	 * select majority without reaching quorum.
	 */
	@Test
	public void oneJobSeveralWorkersUnreliableMajority() {
		/* Several workers, one job, several result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				3, 3, 4);
		scheduler.putSchedulerListener(this);
		Job job = new Job() {
			public boolean equals(Object aJob) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Worker worker3 = new Worker() {
		};
		Worker worker4 = new Worker() {
		};
		Result result1 = new Result() {
			public boolean equals(Object aResult) {
				return this == aResult;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Result result2 = new Result() {
			public boolean equals(Object aResult) {
				return this == aResult;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Result result3 = new Result() {
			public boolean equals(Object aResult) {
				return this == aResult;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/* The first four workers give distinct results */
		scheduler.addJob(job);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		assertEquals("Jobs must be the same", job, pulledJob1);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result1);
		assertNull("No job must be available", pulledJob2);

		Job pulledJob3 = scheduler.submitResultAndPullJob(worker2, null, null);
		assertEquals("Jobs must be the same", job, pulledJob3);
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker2, pulledJob3,
				result2);
		assertNull("No job must be available", pulledJob4);

		Job pulledJob5 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertEquals("Jobs must be the same", job, pulledJob5);
		Job pulledJob6 = scheduler.submitResultAndPullJob(worker3, pulledJob5,
				result3);
		assertNull("No job must be available", pulledJob6);

		Job pulledJob7 = scheduler.submitResultAndPullJob(worker4, null, null);
		assertEquals("Jobs must be the same", job, pulledJob7);

		/* After the maximum duplication is reached, it must find the majority */
		assertNull("Result must not be certified yet", getCertifiedResult(job));
		Job pulledJob8 = scheduler.submitResultAndPullJob(worker4, pulledJob7,
				result1);
		assertNull("No job must be available", pulledJob8);
		assertEquals("Results must be the same", result1,
				getCertifiedResult(job));
	}

	/**
	 * Tests that the mechanism manage two jobs.
	 */
	@Test
	public void twoJobsSeveralWorkers() {
		/* Several workers, one job, several result and duplication */
		BOINCScheduler<Job, Result> scheduler = new BOINCScheduler<Job, Result>(
				2, 2, 3);
		scheduler.putSchedulerListener(this);
		Job job1 = new Job() {
			public boolean equals(Object aJob) {
				return this == aJob;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Job job2 = new Job() {
			public boolean equals(Object aJob) {
				return this == aJob;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};
		Worker worker1 = new Worker() {
		};
		Worker worker2 = new Worker() {
		};
		Worker worker3 = new Worker() {
		};
		Result result = new Result() {
			public boolean equals(Object aResult) {
				return true;
			}

			public int hashCode() {
				return super.hashCode();
			}
		};

		/* The first worker do the first job and is assigned to the second */
		scheduler.addJob(job1);
		scheduler.addJob(job2);
		Job pulledJob1 = scheduler.submitResultAndPullJob(worker1, null, null);
		assertEquals("Jobs must be the same", job1, pulledJob1);
		Job pulledJob2 = scheduler.submitResultAndPullJob(worker1, pulledJob1,
				result);
		assertEquals("Jobs must be the same", job2, pulledJob2);

		/* Give the jobs to the other workers */
		Job pulledJob3 = scheduler.submitResultAndPullJob(worker2, null, null);
		assertEquals("Jobs must be the same", job1, pulledJob3);
		Job pulledJob4 = scheduler.submitResultAndPullJob(worker3, null, null);
		assertEquals("Jobs must be the same", job2, pulledJob4);

		/* After two results are returned, it must certify the first job */
		assertNull("Result must not be certified yet", getCertifiedResult(job1));
		Job pulledJob5 = scheduler.submitResultAndPullJob(worker2, pulledJob3,
				result);
		assertNull("No job must be available", pulledJob5);
		assertEquals("Results must be the same", result,
				getCertifiedResult(job1));

		/* After the third worker comes into play, it must not certify it yet */
		Job pulledJob6 = scheduler.submitResultAndPullJob(worker3, pulledJob4,
				result);
		assertNull("No job must be available", pulledJob6);

		/* After the last result is returned, it must certify it */
		assertNull("Result must not be certified yet", getCertifiedResult(job2));
		Job pulledJob7 = scheduler.submitResultAndPullJob(worker1, pulledJob2,
				result);
		assertNull("No job must be available", pulledJob7);
		assertEquals("Results must be the same", result,
				getCertifiedResult(job2));
	}

}