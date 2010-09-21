package simdeg.reputation.simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.lang.reflect.Field;

import simdeg.reputation.ReputationSystem;
import simdeg.util.OutOfRangeException;

/**
 * The Simulator is the supervisor of the simulated system. It generates and
 * has complete knowledge over all Worker and Job.
 */
class Simulator {

    /**
     * Type for defining the methods that sould be used for result
     * certification. PERFECT give always the best result, BEST give the best
     * among the ones computed, and MAJORITY give the one having the majority.
     */
    enum CertificationMethod {PERFECT, BEST, MAJORITY};

    /** Logger */
    private static Logger logger;

    /** This class should not be instantiated */
    protected Simulator() {
    }


    /* Configuration parsing part */

    private static ReputationSystem<Worker> reputationSystem = null;
    private static CertificationMethod certificationMethod = null;

    private static String fileTrace = null;
    private static String fileCharacteristic = null;

    /* TODO add parameters of the reputation system
     * - multiplier for hardening merges
     * - multiplier for collusion (readaptation)
     */
    private static enum Parameter {
        ReputationSystemComponent, EstimationLevel,
        CertificationMethod,
        FileTrace, FileCharacteristic;
    }

    @SuppressWarnings("unchecked")
	private static void addParameter(String name, String value) {
        switch (Parameter.valueOf(name)) {
            case EstimationLevel:
                if (Double.valueOf(value) < 0)
                    throw new OutOfRangeException(Double.valueOf(value),
                            0.0d, Double.MAX_VALUE);
                break;
            default:
        }
        try {
            switch (Parameter.valueOf(name)) {
                case ReputationSystemComponent:
                    try {
                        String gridCharacteristicsStr = "simdeg.reputation." + value;
                        reputationSystem = (ReputationSystem<Worker>)Class.forName
                            (gridCharacteristicsStr).newInstance();
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.SEVERE, "Reputation system " + name
                                + " is not implemented", e);
                        System.exit(1);
                    }
                    break;
                case EstimationLevel:
                    simdeg.util.Estimator.DEFAULT_REINIT_LEVEL = Double.valueOf(value);
                    break;
                case CertificationMethod:
                    certificationMethod = CertificationMethod.valueOf(value);
                    break;
                case FileTrace:
                    fileTrace = value;
                    break;
                case FileCharacteristic:
                    fileCharacteristic = value;
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error with parameter " + name
                    + " and value " + value, e);
            System.exit(1);
        }
        logger.config("Parameter " + name + " is: " + value);
    }

    private static void parseConfiguration(String fileName) {
        try {
            File file = new File(fileName);
            logger.config("Reading configuration file " + fileName);
            Scanner scanner = new Scanner(file);
            int i=0;
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                if (nextLine.equals(""))
                    continue;
                Scanner scannerComment = new Scanner(nextLine);
                scannerComment.useDelimiter("#");
                if (nextLine.charAt(0) == '#') {
                    if (scannerComment.hasNext())
                        logger.config("Read comment: " + scannerComment.next());
                    scannerComment.close();
                    continue;
                }
                Scanner scannerLine = new Scanner(scannerComment.next());
                if (scannerComment.hasNext())
                    logger.config("Read comment: " + scannerComment.next());
                scannerComment.close();
                scannerLine.useDelimiter(" = ");
                if (scannerLine.hasNext()) {
                    String name = scannerLine.next();
                    String value = scannerLine.next();
                    addParameter(name, value);
                }
                scannerLine.close();
                i++;
            }
            scanner.close();
            if (i != Parameter.values().length) {
                logger.severe("Only " + i + " on " + Parameter.values().length
                        + " required parameters in " + fileName);
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "File " + fileName + " was not found", e);
            System.exit(1);
        }
    }

    /**
     * Tries to get the best result for the given job.
     */
    private static Result getCertifiedResult(Job job, Map<Job,
            List<Result>> resultingResults) {
        if (!resultingResults.containsKey(job))
            throw new NoSuchElementException("Job not yet computed by any worker");

        Result certResult = null;
        if (certificationMethod == CertificationMethod.PERFECT)
            certResult = new Result(0);
        else {
            List<Result> resultsList = resultingResults.get(job);
            if (certificationMethod == CertificationMethod.BEST
                    && resultsList.contains(new Result(0)))
                certResult = new Result(0);
            else {
                Map<Result, Integer> map = new HashMap<Result, Integer>();
                for (Result result : resultsList)
                    map.put(result, 0);
                for (Result result : resultsList)
                    map.put(result, map.get(result) + 1);
                Result majorityResult = null;
                for (Result result : resultsList)
                    if (majorityResult == null
                            || map.get(result) > map.get(majorityResult))
                        majorityResult = result;
                certResult = majorityResult;
            }
        }
        /* Cleaning */
        resultingResults.remove(job);

        logger.fine("Certification of the result: " + certResult);
        return certResult;
    }

    /**
     * Main part of the simulator.
     */
    public static void main(String[] args) {

        /* Parse parameters from the configuration file */
        if (args.length < 2) {
            System.err.println("Usage: java simulator.simulator configuration_file output_file");
            System.exit(1);
        }

        /* Specify the output file containing the logs */
        final Field fields[] = LogManager.class.getDeclaredFields();
        for (Field field : fields)
            if (field.getName().equals("props")) {
                try {
                field.setAccessible(true);
                ((Properties)field.get(LogManager.getLogManager()))
                    .setProperty("java.util.logging.FileHandler.pattern", args[1]);
                } catch (IllegalAccessException e) {
                    System.err.println("Useless " + e);
                    System.exit(1);
                }
            }
        logger = Logger.getLogger(Simulator.class.getName());

        logger.config("Used with commands: " + Arrays.toString(args));

        /* Parse main parameters */
        parseConfiguration(args[0]);

        /* Initialize the evaluation of the reputation system */
        Evaluator evaluator = new Evaluator(reputationSystem,
                fileCharacteristic);

        /* Initialize the reputation system with the worker */
        reputationSystem.addAllWorkers(evaluator.getAllWorkers());

        /* Open trace file */
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(fileTrace));
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "File " + fileTrace + " was not found", e);
            System.exit(1);
        }
        scanner.useLocale(Locale.ENGLISH);

        /* List of results for each job (for progression status). 
         * This structure stores incomplete result sets. */
        Map<Job, List<Result>> resultingResults
            = new HashMap<Job, List<Result>>();

        /* Launch the simulation: getting results from workers and assigning
         * them jobs */
        logger.info("Start the traces");
        while (scanner.hasNextLine()) {
            final double timestamp = scanner.nextDouble();
            final long id = scanner.nextLong();
            Scanner scannerLine = new Scanner(scanner.nextLine());
            if (scannerLine.hasNext()) {
                /* Get a triple */
                final Worker worker = new Worker(id);
                final Job job = new Job(scannerLine.nextLong());
                final Result result = new Result(scannerLine.nextLong());
                logger.finer("Read " + timestamp + " " + worker + " " + job
                        + " " + result);

                /* Inform the reputation system */
                reputationSystem.setWorkerResult(worker, job, result);
                if (!resultingResults.containsKey(job))
                    resultingResults.put(job, new ArrayList<Result>());
                resultingResults.get(job).add(result);
            } else {
                final Job job = new Job(id);
                logger.finer("Read " + timestamp + " " + job);

                /* In the case every worker has finished a job */
                final Result result = getCertifiedResult(job,
                        resultingResults);
                reputationSystem.setCertifiedResult(job, result);
            }
            scannerLine.close();

            evaluator.setStep(timestamp);
        }
        scanner.close();
        logger.info("End of the traces");

        logger.info("Obtained reputation systems:\n"
                + reputationSystem.toString());
    }

}
