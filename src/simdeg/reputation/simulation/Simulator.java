package simdeg.reputation.simulation;

import static simdeg.util.Collections.getRandomSubGroup;
import static simdeg.util.Collections.parseList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import simdeg.reputation.ReputationSystem;
import simdeg.util.RandomManager;
import simdeg.util.Switcher;

/**
 * The Simulator is the supervisor of the simulated system. It generates and
 * has complete knowledge over all Worker and Job.
 */
class Simulator {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(Simulator.class.getName());

    /** This class should not be instanciated */
    protected Simulator() {
    }


    /* Configuration parsing part */

    private static ReputationSystem reputationSystem = null;

    private static double resourcesGroupSizeMean = 0.0d;
    private static double resourcesGroupSizeStdDev = 0.0d;

    private static int stepsNumber = 0;
    private static int workersNumber = 0;
    private static double resultArrivalHeterogeneity = 0.0d;

    private static double reliabilityWorkersFraction1 = 0.0d;
    private static double reliabilityProbability1 = 0.0d;
    private static double reliabilityWorkersFraction2 = 0.0d;
    private static double reliabilityProbability2 = 0.0d;
    private static int reliabilitySwitchStep = 0;
    private static int reliabilitySwitchSpeed = 0;
    private static double reliabilityPropagationRate = 0.0d;

    private static int buggingGroupNumber1 = 0;
    private static List<Double> buggingWorkersFraction1 = null;
    private static List<Double> buggingJobsFraction1 = null;
    private static int buggingGroupNumber2 = 0;
    private static List<Double> buggingWorkersFraction2 = null;
    private static List<Double> buggingJobsFraction2 = null;
    private static int buggingSwitchStep = 0;
    private static int buggingSwitchSpeed = 0;
    private static double buggingPropagationRate = 0.0d;

    private static enum Parameter {
        ReputationSystem, SchedulingSeed,
        StepsNumber, WorkersNumber, ResultArrivalHeterogeneity, ResultArrivalSeed,
        ReliabilityWorkersFraction1, ReliabilityProbability1,
        ReliabilityWorkersFraction2, ReliabilityProbability2,
        ReliabilitySwitchStep, ReliabilitySwitchSpeed,
        ReliabilityPropagationRate, ReliabilitySeed,
        BuggingGroupNumber1, BuggingWorkersFraction1, BuggingJobsFraction1,
        BuggingGroupNumber2, BuggingWorkersFraction2, BuggingJobsFraction2,
        BuggingSwitchStep, BuggingSwitchSpeed, BuggingPropagationRate,
        BuggingSeed;
    }

    @SuppressWarnings("unchecked")
    private static void addParameter(String name, String value) {
        switch (Parameter.valueOf(name)) {
            case StepsNumber: case WorkersNumber: case ReliabilitySwitchStep:
            case ReliabilitySwitchSpeed: case BuggingGroupNumber1:
            case BuggingGroupNumber2: case BuggingSwitchStep:
            case BuggingSwitchSpeed:
                if (Integer.valueOf(value) < 0)
                    throw new IllegalArgumentException(value + " is negative");
            case ResultArrivalHeterogeneity: case ReliabilityWorkersFraction1:
            case ReliabilityProbability1: case ReliabilityWorkersFraction2:
            case ReliabilityProbability2: case ReliabilityPropagationRate:
            case BuggingPropagationRate:
                if (Double.valueOf(value) < 0)
                    throw new IllegalArgumentException(value + " is negative");
                break;
            default:
        }
        try {
            switch (Parameter.valueOf(name)) {
                case ReputationSystem:
                    try {
                        String gridCharacteristicsStr = "simdeg.reputation." + value;
                        reputationSystem = (ReputationSystem)Class.forName
                            (gridCharacteristicsStr).newInstance();
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.SEVERE, "Reputation system " + name
                                + " is not implemented", e);
                        System.exit(1);
                    }
                    break;
                case SchedulingSeed:
                    RandomManager.setSeed("scheduling", Long.valueOf(value));
                    break;
                case StepsNumber:
                    stepsNumber = Integer.valueOf(value);
                    break;
                case WorkersNumber:
                    workersNumber = Integer.valueOf(value);
                    break;
                case ResultArrivalHeterogeneity:
                    resultArrivalHeterogeneity = Double.valueOf(value);
                    break;
                case ResultArrivalSeed:
                    RandomManager.setSeed("result", Long.valueOf(value));
                    break;
                case ReliabilityWorkersFraction1:
                    reliabilityWorkersFraction1 = Double.valueOf(value);
                    break;
                case ReliabilityProbability1:
                    reliabilityProbability1 = Double.valueOf(value);
                    break;
                case ReliabilityWorkersFraction2:
                    reliabilityWorkersFraction2 = Double.valueOf(value);
                    break;
                case ReliabilityProbability2:
                    reliabilityProbability2 = Double.valueOf(value);
                    break;
                case ReliabilitySwitchStep:
                    reliabilitySwitchStep = Integer.valueOf(value);
                    break;
                case ReliabilitySwitchSpeed:
                    reliabilitySwitchSpeed = Integer.valueOf(value);
                    break;
                case ReliabilityPropagationRate:
                    reliabilityPropagationRate = Double.valueOf(value);
                    break;
                case ReliabilitySeed:
                    RandomManager.setSeed("reliability", Long.valueOf(value));
                    break;
                case BuggingGroupNumber1:
                    buggingGroupNumber1 = Integer.valueOf(value);
                    break;
                case BuggingWorkersFraction1:
                    buggingWorkersFraction1 = parseList(Double.class, value);
                    break;
                case BuggingJobsFraction1:
                    buggingJobsFraction1 = parseList(Double.class, value);
                    break;
                case BuggingGroupNumber2:
                    buggingGroupNumber2 = Integer.valueOf(value);
                    break;
                case BuggingWorkersFraction2:
                    buggingWorkersFraction2 = parseList(Double.class, value);
                    break;
                case BuggingJobsFraction2:
                    buggingJobsFraction2 = parseList(Double.class, value);
                    break;
                case BuggingSwitchStep:
                    buggingSwitchStep = Integer.valueOf(value);
                    break;
                case BuggingSwitchSpeed:
                    buggingSwitchSpeed = Integer.valueOf(value);
                    break;
                case BuggingPropagationRate:
                    buggingPropagationRate = Double.valueOf(value);
                    break;
                case BuggingSeed:
                    RandomManager.setSeed("bugging", Long.valueOf(value));
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
            Scanner scanner = new Scanner(file);
            int i=0;
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                if (nextLine.equals("") || nextLine.charAt(0) == '#')
                    continue;
                Scanner scannerComment = new Scanner(nextLine);
                scannerComment.useDelimiter("#");
                Scanner scannerLine = new Scanner(scannerComment.next());
                scannerLine.useDelimiter(" = ");
                if (scannerLine.hasNext()){
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
        assert(buggingWorkersFraction1.size() >= buggingGroupNumber1
                && buggingJobsFraction1.size() >= buggingGroupNumber1
                && buggingWorkersFraction2.size() >= buggingGroupNumber2
                && buggingJobsFraction2.size() >= buggingGroupNumber2)
            : "Not enough values in lists for the groups";
    }

    /**
     * Creates of switchers for reliability.
     */
    private static Set<Switcher<Double>> createReliabilitySwitchers() {
        /* Create pairs for reliability */
        Set<Double[]> pairReliability = new HashSet<Double[]>();
        for (int i=0; i<workersNumber; i++)
            pairReliability.add(new Double[2]);
        /* Divide reliable and unreliable in the first and second phase */
        Set<Double[]> firstReliable = getRandomSubGroup
            ((int)(reliabilityWorkersFraction1 * workersNumber), pairReliability,
             RandomManager.getRandom("reliability"));
        Set<Double[]> secondReliable = getRandomSubGroup
            ((int)(reliabilityWorkersFraction2 * workersNumber), pairReliability,
             RandomManager.getRandom("reliability"));
        /* Determine start-time offsets */
        Set<Double> startTimes = new HashSet<Double>();
        double last = 0.0d;
        for (int i=0; i<workersNumber; i++) {
            startTimes.add(last);
            last += RandomManager.getRandom("reliability")
                .nextExponential(1.0d / reliabilityPropagationRate);
        }
        /* Generate switchers */
        Set<Switcher<Double>> switchers = new HashSet<Switcher<Double>>();
        for (Double[] switcher : pairReliability) {
            switcher[0] = reliabilityProbability1;
            if (firstReliable.contains(switcher))
                switcher[0] = 1.0d;
            switcher[1] = reliabilityProbability2;
            if (secondReliable.contains(switcher))
                switcher[1] = 1.0d;
            final Double offset = getRandomSubGroup(1, startTimes,
                    RandomManager.getRandom("reliability")).iterator().next();
            startTimes.remove(offset);
            final double start = reliabilitySwitchStep + offset;
            final double end = start + 1.0d / reliabilitySwitchSpeed;
            switchers.add(new Switcher<Double>(switcher, start, end));
        }
        assert(switchers.size() == workersNumber)
            : "Not enough reliability switchers";
        assert(startTimes.isEmpty()) : "Non deletion of double in a Set";
        return switchers;
    }

    /**
     * Creates switchers for bugging groups.
     */
    @SuppressWarnings("unchecked")
    private static Set<Switcher<Set<CollusionGroup>>>
        createBuggingSwitchers() {
        /* Create pairs for bugging groups */
        Set<Set<CollusionGroup>[]> pairBugging
            = new HashSet<Set<CollusionGroup>[]>();
        for (int i=0; i<workersNumber; i++) {
            Set<CollusionGroup>[] pair = new Set[2];
            pair[0] = new HashSet<CollusionGroup>();
            pair[1] = new HashSet<CollusionGroup>();
            pairBugging.add(pair);
        }
        /* Divide pairs in bugging groups for the first and second phase */
        List<Set<Set<CollusionGroup>[]>> firstBugging
            = new ArrayList<Set<Set<CollusionGroup>[]>>();
        for (int i=0; i<buggingGroupNumber1; i++)
            firstBugging.add(getRandomSubGroup
                    ((int)(buggingWorkersFraction1.get(i) * workersNumber),
                     pairBugging, RandomManager.getRandom("bugging")));
        List<Set<Set<CollusionGroup>[]>> secondBugging
            = new ArrayList<Set<Set<CollusionGroup>[]>>();
        for (int i=0; i<buggingGroupNumber2; i++)
            secondBugging.add(getRandomSubGroup
                    ((int)(buggingWorkersFraction2.get(i) * workersNumber),
                     pairBugging, RandomManager.getRandom("bugging")));
        /* Determine start-time offsets */
        Set<Double> startTimes = new HashSet<Double>();
        double last = 0.0d;
        for (int i=0; i<workersNumber; i++) {
            startTimes.add(last);
            last += RandomManager.getRandom("bugging")
                .nextExponential(1.0d / buggingPropagationRate);
        }
        /* Generate bugging groups */
        List<CollusionGroup> buggingGroups1 = new ArrayList<CollusionGroup>();
        for (int i=0; i<buggingGroupNumber1; i++)
            buggingGroups1.add(new CollusionGroup(buggingJobsFraction1.get(i), "bugging"));
        List<CollusionGroup> buggingGroups2 = new ArrayList<CollusionGroup>();
        for (int i=0; i<buggingGroupNumber2; i++)
            buggingGroups2.add(new CollusionGroup(buggingJobsFraction2.get(i), "bugging"));
        /* Generate switchers */
        Set<Switcher<Set<CollusionGroup>>> switchers
            = new HashSet<Switcher<Set<CollusionGroup>>>();
        for (Set<CollusionGroup>[] switcher : pairBugging) {
            for (int i=0; i<buggingGroupNumber1; i++)
                if (firstBugging.get(i).contains(switcher))
                    switcher[0].add(buggingGroups1.get(i));
            for (int i=0; i<buggingGroupNumber2; i++)
                if (secondBugging.get(i).contains(switcher))
                    switcher[1].add(buggingGroups2.get(i));
            final Double offset = getRandomSubGroup(1, startTimes,
                    RandomManager.getRandom("bugging")).iterator().next();
            startTimes.remove(offset);
            final double start = buggingSwitchStep + offset;
            final double end = start + 1.0d / buggingSwitchSpeed;
            switchers.add(new Switcher<Set<CollusionGroup>>(switcher, start, end));
        }
        assert(switchers.size() == workersNumber)
            : "Not enough bugging switchers";
        assert(startTimes.isEmpty()) : "Non deletion of double in a Set";
        return switchers;
    }

    /**
     * Creation of workers with corresponding reliability and collusion
     * behavior.
     */
    private static Set<Worker> initializeWorkers(Evaluator evaluator) {

        /* Reliability and bugging probabilities generation */
        Set<Switcher<Double>> reliabilitySwitchers
            = createReliabilitySwitchers();
        Set<Switcher<Set<CollusionGroup>>> buggingSwitchers
            = createBuggingSwitchers();

        /* Create data structure for the Evaluator */
        Map<Worker,Switcher<Double>> workersReliability
            = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups
            = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();

        /* Build workers with the generated switchers */
        for (int i=0; i<workersNumber; i++) {
            final Switcher<Double> reliable
                = reliabilitySwitchers.iterator().next();
            reliabilitySwitchers.remove(reliable);
            final Switcher<Set<CollusionGroup>> bugging
                = buggingSwitchers.iterator().next();
            buggingSwitchers.remove(bugging);
            final Worker worker = new Worker("worker"+i, reliable, bugging);
            workersReliability.put(worker, reliable);
            buggingGroups.put(worker, bugging);
            logger.config("Worker " + worker.getName()
                    + " created with reliability switcher " + reliable
                    + ", bugging groups switcher " + bugging);
        }

        /* Inform the evaluator of the workers characteristics */
        evaluator.setWorkersFaultiness(workersReliability,
                buggingGroups);

        return workersReliability.keySet();
    }

    /**
     * Main part of the simulator.
     */
    public static void main(String[] args) {

        if(args.length < 1) {
            logger.severe("Usage : java simulator.simulator configuration_file");
            System.exit(1);
        }

        /* Parse parameters from the configuration file */
        parseConfiguration(args[0]);

        /* Initialize the evaluation of the reputation system */
        Evaluator evaluator = new Evaluator(reputationSystem);
        evaluator.setSteps(stepsNumber, reliabilitySwitchStep,
                buggingSwitchStep);

        /* Set the platform characteristics */
        Set<Worker> workers = initializeWorkers(evaluator);

        /* Prepare the scheduler */
        Scheduler scheduler = new Scheduler(reputationSystem, evaluator);
        scheduler.addAllWorkers(workers);

        /* Launch the simulation */
        scheduler.start(stepsNumber, resourcesGroupSizeMean,
                resourcesGroupSizeStdDev, resultArrivalHeterogeneity);
    }

}
