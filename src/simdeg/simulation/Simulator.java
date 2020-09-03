package simdeg.simulation;

import static simdeg.util.Collections.getRandomSubGroup;
import static simdeg.util.Collections.parseList;
import static simdeg.util.Collections.parseListOfList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import simdeg.reputation.ReputationSystem;
import simdeg.scheduling.ResultCertificator;
import simdeg.scheduling.Scheduler;
import simdeg.scheduling.SchedulerListener;
import simdeg.scheduling.VotingPool;
import simdeg.util.OutOfRangeException;
import simdeg.util.RandomManager;

/**
 * The Simulator is the supervisor of the simulated system. It generates and has
 * complete knowledge over all workers and jobs. Basically, it reads several
 * input files for specifying the speed of the workers, their availabilities and
 * the cost of the jobs. When the platform is created (speed of the workers and
 * fault models), a new job is submitted to the scheduler whenever it needs one,
 * until there is no more available job. In the same time, the availability
 * trace file is read and events are created to specify the starting and the
 * ending time at which a worker becomes available and then unavailable for
 * computation. When a worker pull a job from the scheduler, two other kinds of
 * events are created: a timeout event for the computation and a completion
 * event that indicates the end of the computation. The simulator works by
 * traversing the events that are created in a chronological order and by
 * updating dynamically this list of events depending of the events that are
 * successively considered. For example, both an unavailability and a completion
 * event for the same worker can cohabit at the same time. When one of these
 * events is polled from the list, the other one may be deleted or replaced by
 * another event.
 * 
 * At any time, there is six possible configurations for the events related to a
 * given worker in the list of events:
 * 
 * 1. no event: goes to 2 if the availability trace is read and if the line is
 * related to the current worker
 * 
 * 2. {@link AvailabilityEvent} and {@link UnavailabilityEvent}: goes to 3 if
 * the {@link AvailabilityEvent} is polled.
 * 
 * 3. {@link UnavailabilityEvent}, {@link ProcessCompletionEvent} and
 * {@link ProcessTimeoutEvent}: goes to 4 if the availability trace is read and
 * if the line is related to the current worker; goes to 5 if the
 * {@link UnavailabilityEvent} is polled; stays to 3 if either the
 * {@link ProcessCompletionEvent} or the {@link ProcessTimeoutEvent} is polled.
 * 
 * 4. {@link UnavailabilityEvent}, {@link AvailabilityEvent},
 * {@link UnavailabilityEvent}, {@link ProcessCompletionEvent} and
 * {@link ProcessTimeoutEvent}: goes to 6 if the {@link UnavailabilityEvent} is
 * polled; stays in 4 if either the {@link ProcessCompletionEvent} or the
 * {@link ProcessTimeoutEvent} is polled.
 * 
 * 5. {@link ProcessTimeoutEvent}: goes to 6 if the availability trace is read
 * and if the line is related to the current worker; goes to 1 if the
 * {@link ProcessTimeoutEvent} is polled.
 * 
 * 6. {@link AvailabilityEvent}, {@link UnavailabilityEvent} and
 * {@link ProcessTimeoutEvent}: goes to 3 if the {@link AvailabilityEvent} is
 * polled; goes to 2 if the {@link ProcessTimeoutEvent} is polled.
 * 
 */
class Simulator implements SchedulerListener {

	/** Logger */
	private static final Logger logger = Logger.getLogger(Simulator.class
			.getName());

	/** Length of the timeout: 10 days */
	private static final double TIMEOUT = 10 * 24 * 60 * 60;

	private static final String SETI_AVAILABILITY_NAME = "event_trace.tab";

	private long startingTime;

	/** Number of jobs that needs to be computed */
	private final int jobsNumber;

	/**
	 * Scanner from which are iteratively read the availability and
	 * unavailability events.
	 */
	private final Scanner availabilityTrace;

	/** Contains the job costs */
	private final Scanner jobsTrace;

	/** Specifies if the performance file corresponds to the SETI@Home trace */
	private final boolean completeSetiSettings;

	/** Maps the id in the trace files to the created workers */
	private final Map<Integer, Worker> correspondence;

	private final FileWriter output;

	private final File outputRep;

	/** Ordered set of events that are chronologically considered */
	private final TreeSet<Event> events = new TreeSet<Event>(
			new Comparator<Event>() {
				public int compare(Event arg0, Event arg1) {
					if (arg0.getDate() < arg1.getDate())
						return -1;
					if (arg0.getDate() > arg1.getDate())
						return 1;
					if (arg0.hashCode() < arg1.hashCode())
						return -1;
					if (arg0.hashCode() > arg1.hashCode())
						return 1;
					return 0;
				}
			});

	/**
	 * Scheduler that gives a job for each worker request and receives the
	 * results.
	 */
	private final Scheduler<Job, Result> scheduler;

	/** Groups of collusion that are synthetically created */
	private final List<CollusionGroup> collusionGroups;

	/** Groups of inter-collusion that are synthetically created */
	private final List<InterCollusionGroup> interCollusionGroups;

	/** Number of distinct jobs that are submitted to the scheduler */
	private int submittedJobs;

	/** Number of distinct jobs that are certified */
	private int certifiedJobs;

	/** Builds a simulator with the given properties and output file */
	protected Simulator(Properties properties) throws IOException {
		/* Initialize the seeds */
		final long platformSeed = Long.parseLong(properties
				.getProperty("platformSeed"));
		RandomManager.setSeed("platform", platformSeed);
		final long reliabilitySeed = Long.parseLong(properties
				.getProperty("reliabilitySeed"));
		RandomManager.setSeed("reliability", reliabilitySeed);

		/* Detect if the availability trace file is the SETI@Home one */
		final String name = properties.getProperty("availabilityTraceFile");
		completeSetiSettings = name.substring(
				name.length() - SETI_AVAILABILITY_NAME.length()).equals(
				SETI_AVAILABILITY_NAME);

		/* Build workers */
		final int workersNumber = Integer.parseInt(properties
				.getProperty("workersNumber"));
		final File availabilityTraceFile = new File(name);
		correspondence = buildPlatform(workersNumber, availabilityTraceFile);
		logger.info("Found first " + workersNumber + " workers");

		final File workersSpeedFile = new File(properties
				.getProperty("workersSpeedFile"));
		setPlatformSpeed(correspondence, workersSpeedFile, completeSetiSettings);
		final Set<Worker> workers = new HashSet<Worker>(correspondence.values());
		logger.info("The speeds of the workers are set");

		final double reliabilityFraction = Double.parseDouble(properties
				.getProperty("reliabilityFraction"));
		final double reliabilityProbability = Double.parseDouble(properties
				.getProperty("reliabilityProbability"));
		setPlatformReliability(workers, reliabilityFraction,
				reliabilityProbability);
		logger.info("The reliability of the workers are set");

		/* Build collusion groups */
		final List<Double> collusionFraction = parseList(Double.class,
				properties.getProperty("collusionFraction"));
		final List<Double> collusionProbability = parseList(Double.class,
				properties.getProperty("collusionProbability"));
		collusionGroups = buildCollusionGroup(workers, collusionFraction,
				collusionProbability);
		logger.info("The workers are set in groups of collusion");

		/* Build inter-collusion groups */
		final List<List<Integer>> interCollusionFraction = parseListOfList(
				Integer.class, properties.getProperty("interCollusionFraction"));
		final List<Double> interCollusionProbability = parseList(Double.class,
				properties.getProperty("interCollusionProbability"));
		interCollusionGroups = buildInterCollusionGroup(workers,
				collusionGroups, interCollusionFraction,
				interCollusionProbability);
		logger.info("The groups of collusion are set"
				+ " in groups of inter-collusion");
		/* Decision tree creation */
		new InterCollusionDecisionTree(interCollusionGroups);
		logger.info("Decision tree is built");

		/* Workload parameters */
		jobsNumber = Integer.parseInt(properties.getProperty("jobsNumber"));

		/* Initialize the trace inputs */
		availabilityTrace = new Scanner(availabilityTraceFile);
		jobsTrace = new Scanner(new File(properties
				.getProperty("jobsTraceFile")));
		jobsTrace.nextLine();

		/* Build scheduling components */
		final String schedulerClassName = properties.getProperty("scheduler");
		final String resultCertificatorClassName = properties
				.getProperty("resultCertificator");
		final String reputationSystemClassName = properties
				.getProperty("reputationSystem");
		scheduler = getScheduler(schedulerClassName,
				resultCertificatorClassName, reputationSystemClassName);

		/* Initialize the scheduler */
		scheduler.addAllWorkers(workers);
		scheduler.putSchedulerListener(this);

		/* Initialize the output files */
		final String outputFile = properties.getProperty("outputFile");
		this.output = new FileWriter(new File(outputFile));
		final String reputationFile = properties.getProperty("reputationFile");
		this.outputRep = new File(reputationFile);
	}

	/**
	 * Creates a simulator with the property and output files given in the
	 * arguments. It then runs the simulation.
	 */
	public static void main(String[] args) throws IOException {
//		System.out.println(System.getProperties().get("bibi"));
//		System.out.println(Long.parseLong((String) System.getProperties().get("bibo")));
//		System.out.println(System.getProperties().get("biba"));
		Locale.setDefault(Locale.ENGLISH);
		Properties properties;
		try {
			properties = new Properties();
			properties.load(new FileInputStream(args[0]));
		} catch (Exception e) {
			properties = System.getProperties();
		}
		final Simulator simulator = new Simulator(properties);
		simulator.run();
	}

	/**
	 * Reads in the availability trace file the {@link AvailabilityEvent} and
	 * {@link UnavailabilityEvent} events.
	 */
	private void addAvailabilityEvent() {
		if (!availabilityTrace.hasNext())
			return;
		int id = availabilityTrace.nextInt();
		while (!correspondence.containsKey(id)) {
			availabilityTrace.nextDouble();
			availabilityTrace.nextDouble();
			if (!availabilityTrace.hasNext())
				return;
			id = availabilityTrace.nextInt();
		}
		final Worker worker = correspondence.get(id);
		final double start = availabilityTrace.nextDouble();
		final double stop = availabilityTrace.nextDouble();
		events.add(new AvailabilityEvent(start, worker));
		events.add(new UnavailabilityEvent(stop, worker));
		logger.fine("New events added for worker " + id + " starting at time "
				+ start + " and stopping at " + stop);
	}

	/**
	 * Assigns a job to a given worker at the specified date. Updates associated
	 * structures such as the list of events. Also, creates the timeout event.
	 */
	private void assignNewJob(Worker worker, Job job, double date) {
		worker.assignJob(job);
		if (job == null)
			return;
		final ProcessTimeoutEvent timeout = new ProcessTimeoutEvent(date
				+ TIMEOUT, worker, job);
		worker.setNextProcessTimeoutEvent(timeout);
		events.add(timeout);
		logger.fine("Put timeout for worker " + worker + " and job " + job
				+ " at time " + (date + TIMEOUT));
	}

	/**
	 * Determine when the next completion event must take place given the
	 * current date in the simulation. Update associated structure such as the
	 * list of events.
	 */
	private void computeCompletionEvent(Worker worker, double date) {
		final Job job = worker.getCurrentJob();
		if (job == null)
			return;
		final double computationTime = worker.getRemainingTime();
		final ProcessCompletionEvent completion = new ProcessCompletionEvent(
				date + computationTime, worker, job);
		worker.setNextProcessCompletionEvent(completion);
		events.add(completion);
		logger.fine("Process completion time for worker " + worker
				+ " and job " + job + " estimated at time "
				+ (date + computationTime));
	}

	/**
	 * Starts the simulation by submitting a single job to the scheduler and by
	 * reading the first line of the availability trace file. It then processes
	 * each event as they appear in the list of events.
	 */
	protected void run() {
		startingTime = System.currentTimeMillis();
		endOfJobQueue();
		addAvailabilityEvent();
		while (!events.isEmpty()) {
			final Event event = events.pollFirst();
			final double date = event.getDate();
			final Worker worker = event.getWorker();
			if (event instanceof AvailabilityEvent) {
				logger.fine("Worker " + worker + " becomes available at time "
						+ date);
				if (worker.getPreviousAvailabilityEvent() != null
						&& worker.getPreviousAvailabilityEvent() instanceof AvailabilityEvent)
					throw new UnsupportedOperationException(
							"A worker may not be available twice successively at time "
									+ date);
				/*
				 * Find a job to compute for this worker, which appears for the
				 * first time.
				 */
				if (worker.getCurrentJob() == null) {
					/* Request a new job */
					final Job newJob = scheduler.submitResultAndPullJob(worker,
							null, null);
					/* Assign the job */
					assignNewJob(worker, newJob, date);
				}
				/* Compute the time by which this job will be completed */
				computeCompletionEvent(worker, date);
				/* Update current status of the worker */
				worker.setPreviousAvailabilityEvent(event);
				/* Add the next availability event */
				addAvailabilityEvent();
			} else if (event instanceof UnavailabilityEvent) {
				logger.fine("Worker " + worker
						+ " becomes unavailable at time " + date);
				if (worker.getPreviousAvailabilityEvent() == null
						|| worker.getPreviousAvailabilityEvent() instanceof UnavailabilityEvent)
					throw new UnsupportedOperationException(
							"A worker may not be unavailable twice successively at time "
									+ date);
				if (worker.getNextProcessCompletionEvent() != null) {
					/*
					 * Remove the completion event of the job assigned to the
					 * current worker, it will be recreated when the worker
					 * becomes available again.
					 */
					events.remove(worker.getNextProcessCompletionEvent());
					/* Update progress done on the job */
					worker.updateRemainingTime(date);
					worker.setNextProcessCompletionEvent(null);
				}
				/* Update current status of the worker */
				worker.setPreviousAvailabilityEvent(event);
			} else if (event instanceof ProcessCompletionEvent) {
				/* Retrieve the job this event is about */
				final Job job = ((ProcessCompletionEvent) event).getJob();
				logger.fine("Worker " + worker
						+ " has finished its assigned job " + job + " at time "
						+ date);
				if (worker.getPreviousAvailabilityEvent() == null
						|| worker.getPreviousAvailabilityEvent() instanceof UnavailabilityEvent)
					throw new UnsupportedOperationException(
							"A worker may not complete a job while being unavailable  at time "
									+ date);
				events.remove(worker.getNextProcessTimeoutEvent());
				/* Consider the result of the worker for this job */
				final Result result = worker.getResult(job);
				/* Submit result and request a new job */
				final Job newJob = scheduler.submitResultAndPullJob(worker,
						job, result);
				/* Assign the job */
				assignNewJob(worker, newJob, date);
				/* Compute the time by which this job will be completed */
				computeCompletionEvent(worker, date);
			} else if (event instanceof ProcessTimeoutEvent) {
				/* Retrieve the job this event is about */
				final Job job = ((ProcessTimeoutEvent) event).getJob();
				logger.fine("Worker " + worker
						+ " has reached its timeout for job " + job
						+ " at time " + date);
				/* Gives up on the job */
				scheduler.submitResultAndPullJob(worker, job, null);
				if (worker.getPreviousAvailabilityEvent() instanceof UnavailabilityEvent) {
					worker.assignJob(null);
					assert (worker.getNextProcessCompletionEvent() == null) : "No completion event if timeout is reached while unavailability phase";
				} else {
					events.remove(worker.getNextProcessCompletionEvent());
					/* Request a new job */
					final Job newJob = scheduler.submitResultAndPullJob(worker,
							null, null);
					/* Assign the job */
					assignNewJob(worker, newJob, date);
					/* Compute the time by which this job will be completed */
					computeCompletionEvent(worker, date);
				}
			} else
				throw new RuntimeException(
						"Not considering other type of events");
		}
		stop();
	}

	/**
	 * Finalizes the simulation and generates the outputs.
	 */
	private void stop() {
		logger.fine("Simulation ends");
		availabilityTrace.close();
		jobsTrace.close();
		try {
			output.close();
			final FileWriter outputRep = new FileWriter(this.outputRep);
			outputRep.write(scheduler.getReputationSystem() + "");
			outputRep.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds the platform by making a correspondence between each id in the
	 * availability trace file to a worker.
	 */
	private static Map<Integer, Worker> buildPlatform(int workersNumber,
			File availabilityTraceFile) throws FileNotFoundException {
		if (workersNumber <= 0.0d)
			throw new OutOfRangeException(workersNumber, 1, Integer.MAX_VALUE);

		final Map<Integer, Worker> workers = new HashMap<Integer, Worker>();
		Scanner scanner = new Scanner(availabilityTraceFile);
		scanner.useDelimiter("\\s[0-9.]+\\s[0-9.]+\\n");
		Set<Integer> ids = new HashSet<Integer>();
		while (scanner.hasNext() && workers.size() < workersNumber) {
			final int id = scanner.nextInt();
			if (!ids.contains(id))
				workers.put(id, new Worker());
		}
		scanner.close();
		return workers;
	}

	/**
	 * Sets the speed of each worker with the workers performance file.
	 */
	private static void setPlatformSpeed(Map<Integer, Worker> correspondence,
			File workersSpeedFile, boolean completeSetiSettings)
			throws FileNotFoundException {
		int done = 0;
		Scanner scanner = new Scanner(workersSpeedFile);
		scanner.nextLine();
		if (completeSetiSettings) {
			while (scanner.hasNextLine() && done != correspondence.size()) {
				scanner.next();
				final int id = scanner.nextInt();
				if (correspondence.containsKey(id)) {
					scanner.next();
					scanner.next();
					try {
						final double fops = scanner.nextDouble();
						correspondence.get(id).setFOPS(fops);
						done++;
					} catch (InputMismatchException e) {
						System.err.println("Missing values in"
								+ " the platform file");
						System.exit(1);
					}
				}
				scanner.nextLine();
			}
			if (done != correspondence.size())
				throw new UnsupportedOperationException(
						"Some worker speeds are not set");
		} else {
			for (Worker worker : correspondence.values()) {
				for (int i = 0; i < 4; i++)
					scanner.next();
				final double fops = scanner.nextDouble();
				worker.setFOPS(fops);
				scanner.nextLine();
			}
		}
		scanner.close();
	}

	/**
	 * Assigns a reliability (probability of failure) to each worker.
	 */
	private static void setPlatformReliability(Set<Worker> workers,
			double reliabilityFraction, double reliabilityProbability) {
		if (reliabilityFraction <= 0.0d || reliabilityProbability > 1.0d)
			throw new OutOfRangeException(reliabilityFraction, 0.0d, 1.0d);
		if (reliabilityProbability < 0.0d || reliabilityProbability > 1.0d)
			throw new OutOfRangeException(reliabilityProbability, 0.0d, 1.0d);

		Set<Worker> unreliable = getRandomSubGroup(
				(int) ((1.0d - reliabilityFraction) * workers.size()), workers,
				RandomManager.getRandom("platform"));
		for (Worker worker : unreliable)
			worker.setReliability(reliabilityProbability);
	}

	/**
	 * Builds the groups of collusion.
	 */
	private static List<CollusionGroup> buildCollusionGroup(
			Set<Worker> workers, List<Double> collusionFraction,
			List<Double> collusionProbability) {
		double correct = 1.0d;
		for (double fraction : collusionFraction)
			correct -= fraction;
		for (double fraction : collusionFraction)
			if (fraction >= correct)
				throw new IllegalArgumentException(
						"Correct workers are not in the majority");
		if (collusionFraction.size() != collusionProbability.size())
			throw new IllegalArgumentException("Colluders incorrecly specified");

		Set<Worker> candidates = new HashSet<Worker>(workers);
		List<CollusionGroup> collusionGroups = new ArrayList<CollusionGroup>();
		for (int i = 0; i < collusionFraction.size(); i++) {
			CollusionGroup collusionGroup = new CollusionGroup(
					collusionProbability.get(i));
			Set<Worker> colluders = getRandomSubGroup((int) (collusionFraction
					.get(i) * workers.size()), candidates, RandomManager
					.getRandom("platform"));
			collusionGroup.addAll(colluders);
			candidates.removeAll(colluders);
			collusionGroups.add(collusionGroup);
		}
		return collusionGroups;
	}

	/**
	 * Builds the groups of inter-collusion.
	 */
	private static List<InterCollusionGroup> buildInterCollusionGroup(
			Set<Worker> workers, List<CollusionGroup> collusionGroups,
			List<List<Integer>> interCollusionFraction,
			List<Double> interCollusionProbability) {
		if (interCollusionFraction.size() != interCollusionProbability.size())
			throw new IllegalArgumentException(
					"Inter-colluders incorrecly specified");

		List<InterCollusionGroup> interCollusionGroups = new ArrayList<InterCollusionGroup>();
		for (int i = 0; i < interCollusionFraction.size(); i++) {
			if (interCollusionFraction.get(i).size() < 2)
				throw new IllegalArgumentException(
						"Inter-colluders need at least two colluder groups");
			InterCollusionGroup interColluderGroup = new InterCollusionGroup(
					interCollusionProbability.get(i));
			for (int index : interCollusionFraction.get(i))
				interColluderGroup.add(collusionGroups.get(index));
			interCollusionGroups.add(interColluderGroup);
		}
		return interCollusionGroups;
	}

	/**
	 * Instantiates the components used for scheduling, certifying and
	 * characterizing.
	 */
	@SuppressWarnings("unchecked")
	private static Scheduler<Job, Result> getScheduler(
			String schedulerClassName, String resultCertificatorClassName,
			String reputationSystemClassName) {
		ReputationSystem reputationSystem = null;
		if (!reputationSystemClassName.equals("null"))
			try {
				reputationSystem = (ReputationSystem) Class.forName(
						"simdeg.reputation." + reputationSystemClassName)
						.newInstance();
			} catch (Exception e) {
				System.err.println("ReputationSystem "
						+ reputationSystemClassName + " not found");
				System.exit(1);
			}

		ResultCertificator resultCertificator = null;
		try {
			resultCertificator = (ResultCertificator) Class.forName(
					"simdeg.scheduling." + resultCertificatorClassName)
					.newInstance();
		} catch (Exception e) {
			System.err.println("ResultCertificator "
					+ resultCertificatorClassName + " not found");
			System.exit(1);
		}

		/* Find the scheduler */
		try {
			Constructor<?> construct = Class.forName(
					"simdeg.scheduling." + schedulerClassName).getConstructor(
					ResultCertificator.class, ReputationSystem.class);
			return (Scheduler<Job, Result>) construct.newInstance(
					resultCertificator, reputationSystem);
		} catch (Exception e) {
			System.err
					.println("Scheduler " + schedulerClassName + " not found");
			System.exit(1);
		}

		return null;
	}

	public void endOfJobQueue() {
		if (submittedJobs < jobsNumber && jobsTrace.hasNext()) {
			for (int i = 0; i < 5; i++)
				jobsTrace.next();
			submittedJobs++;
			final double fops = jobsTrace.nextDouble();
			final Job job = new Job(completeSetiSettings ? fops : fops / 200);
			logger.fine("Create new job " + job + " with " + fops + " FOPS");
			scheduler.addJob(job);
		}
	}

	public <J extends simdeg.reputation.Job, R extends simdeg.reputation.Result> void setCertifiedResult(
			VotingPool<R> votingPool, R result) {
		assert (submittedJobs >= certifiedJobs) : "More certified jobs than submitted ones";
		certifiedJobs++;
		if (certifiedJobs == jobsNumber)
			events.clear();
		/* Printing progress information */
		if (certifiedJobs % 10000 == 0) {
			System.out
					.println(certifiedJobs + " " + System.currentTimeMillis());
			// System.out.println(scheduler.getReputationSystem());
		}
		/* Cleaning internal structures */
		for (CollusionGroup collusionGroup : collusionGroups)
			collusionGroup.clear((Job) votingPool.getJob());
		for (InterCollusionGroup interCollusionGroup : interCollusionGroups)
			interCollusionGroup.clear((Job) votingPool.getJob());
		/* Printing results in the output file */
		try {
			output.write(result + " " + votingPool.size() + " "
					+ (System.currentTimeMillis() - startingTime) + "\n");
			output.flush();
		} catch (IOException e) {
			System.err.println("Problem writing in the output file");
			System.exit(1);
		}
	}

}