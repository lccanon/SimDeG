package simdeg.simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import simdeg.scheduling.BOINCResultCertificator;
import simdeg.scheduling.Scheduler;

/**
 * Builds a complete simulation environment with temporary trace files. The
 * complete simulations are expected to run without raising exception.
 */
public class TestSimulator {

	private static final String JOBS_TRACE_FILE = "test.jobsTraceFile";

	private static final String AVAILABILITY_TRACE_FILE = "test.availabilityTraceFile";

	private static final String WORKERS_SPEED_FILE = "test.workersSpeedFile";

	private static final int BIGNUM = 100;

	@BeforeClass
	public static void setLocale() {
		Locale.setDefault(Locale.ENGLISH);
	}

	@BeforeClass
	public static void activateLogger() {
		final Logger logger = Logger.getLogger(Simulator.class.getName());
		logger.getParent().setLevel(Level.OFF);
		/* Activate one of the following depending to what need to be tested */
		logger.setLevel(Level.OFF);
		Logger.getLogger(Scheduler.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(BOINCResultCertificator.class.getName()).setLevel(
				Level.OFF);
		for (Handler hand : logger.getParent().getHandlers())
			hand.setLevel(Level.ALL);
	}

	@AfterClass
	public static void clean() {
		final File jobsTraceFile = new File(JOBS_TRACE_FILE);
		jobsTraceFile.delete();
		final File availabilityTraceFile = new File(AVAILABILITY_TRACE_FILE);
		availabilityTraceFile.delete();
		final File workersSpeedFile = new File(WORKERS_SPEED_FILE);
		workersSpeedFile.delete();
	}

	private static Properties getInputProperties(int jobs, int workers,
			boolean continuous, boolean adversity) throws IOException {
		generateJobsTraceFile(jobs);
		generateAvailabilityTraceFile(workers, continuous);
		generateWorkersSpeedFile(workers);
		return prepareInputProperties(jobs, workers, adversity);
	}

	private static void generateJobsTraceFile(int n) throws IOException {
		final FileWriter output = new FileWriter(JOBS_TRACE_FILE);
		output
				.write("sent_time	received_time	report_deadline	cpu_time	host_fops	estimated_fops\n");
		for (int i = 0; i < n; i++)
			output
					.write("1248597600	1251884889	1249802400	16513.39	2218163323.53342	2\n");
		output.close();
	}

	private static void generateAvailabilityTraceFile(int workers,
			boolean continuous) throws IOException {
		final FileWriter output = new FileWriter(AVAILABILITY_TRACE_FILE);
		if (continuous) {
			for (int i = 0; i < workers / 2; i++)
				output.write(i + " " + 0 + " " + BIGNUM + '\n');
			for (int i = workers / 2; i < workers; i++)
				output.write(i + " " + 0.5d + " " + (BIGNUM + 0.5d) + '\n');
		} else {
			for (int i = 0; i < BIGNUM; i += 2) {
				for (int j = 0; j < workers / 2; j++)
					output.write(j + " " + i + " " + (i + 1) + '\n');
				for (int j = workers / 2; j < workers; j++)
					output.write(j + " " + (i + 0.5d) + " " + (i + 1 + 0.5d)
							+ '\n');
			}
		}
		output.close();
	}

	private static void generateWorkersSpeedFile(int n) throws IOException {
		final FileWriter output = new FileWriter(WORKERS_SPEED_FILE);
		output
				.write("# metric_id node_id platform_id sfpop_speed dfpop_speed iop_speed i_val f_val s_val\n");
		for (int i = 0; i < n; i++)
			output.write("         0	" + i
					+ "	    1	 NULL	   1	  NULL	  NULL	  NULL	  NULL\n");
		output.close();
	}

	private static Properties prepareInputProperties(int jobs, int workers,
			boolean adversity) {
		final Properties result = new Properties();
		result.setProperty("scheduler", "BOINCScheduler");
		result.setProperty("resultCertificator", "BOINCResultCertificator");
		result.setProperty("reputationSystem", "null");
		result.setProperty("jobsTraceFile", JOBS_TRACE_FILE);
		result.setProperty("jobsNumber", "" + jobs);
		result.setProperty("availabilityTraceFile", AVAILABILITY_TRACE_FILE);
		result.setProperty("workersNumber", "" + workers);
		result.setProperty("workersSpeedFile", WORKERS_SPEED_FILE);
		if (!adversity) {
			result.setProperty("reliabilityFraction", "1");
			result.setProperty("reliabilityProbability", "1");
			result.setProperty("collusionFraction", "[]");
			result.setProperty("collusionProbability", "[]");
			result.setProperty("interCollusionFraction", "()");
			result.setProperty("interCollusionProbability", "[]");
		} else {
			result.setProperty("reliabilityFraction", "0.7");
			result.setProperty("reliabilityProbability", "0.7");
			result.setProperty("collusionFraction", "[0.15,0.15,0.15,0.15]");
			result.setProperty("collusionProbability", "[0.45,0.15,0.24,0.2]");
			result.setProperty("interCollusionFraction", "([0,1];[1,2];[2,3])");
			result.setProperty("interCollusionProbability", "[0.1,0.2,0.4]");
		}
		return result;
	}

	/**
	 * Tests availability event, completion event and reassignment.
	 */
	@Test
	public void oneWorkerContinuous() throws IOException {
		final Properties properties = getInputProperties(10, 1, true, false);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Tests unavailability event and update of completion event.
	 */
	@Test
	public void oneWorkerDiscontinuous() throws IOException {
		final Properties properties = getInputProperties(10, 1, false, false);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Tests timeout event.
	 */
	@Test
	public void oneWorkerContinuousTimeout() throws IOException {
		final Properties properties = getInputProperties(BIGNUM, 1, true, false);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Tests multiple workers and end of simulations before end of availability
	 * traces.
	 */
	@Test
	public void severalWorkersContinuous() throws IOException {
		final Properties properties = getInputProperties(10, 3, true, false);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Tests multiple discontinuous workers.
	 */
	@Test
	public void severalWorkersDiscontinuous() throws IOException {
		final Properties properties = getInputProperties(10, 3, false, false);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Tests multiple discontinuous workers with adversity.
	 */
	@Test
	public void severalWorkersDiscontinuousAdversity() throws IOException {
		final Properties properties = getInputProperties(10, 20, false, true);
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

}