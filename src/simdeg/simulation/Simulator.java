package simdeg.simulation;

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
import simdeg.scheduling.ResourcesGrouper;
import simdeg.scheduling.ResultCertificator;
import simdeg.scheduling.Scheduler;
import simdeg.util.RandomManager;
import simdeg.util.Switcher;
import simgrid.msg.Host;
import simgrid.msg.JniException;
import simgrid.msg.Msg;
import simgrid.msg.NativeException;

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

    private static ResourcesGrouper resourcesGrouper = null;
    private static ResultCertificator resultCertificator = null;
    private static ReputationSystem reputationSystem = null;

    private static int jobsNumber = 0;
    private static double jobsCostMean = 0.0d, jobsCostStdDev = 0.0d;
    private static double jobsArrivalRate = 0.0d;

    private static int workersNumber = 0;
    private static double workersSpeedMin = 0.0d, workersSpeedMax = 0.0d,
            workersSpeedMean = 0.0d, workersSpeedStdDev = 0.0d;

    private static double reliabilityWorkersFraction1 = 0.0d;
    private static double reliabilityProbability1 = 0.0d;
    private static double reliabilityWorkersFraction2 = 0.0d;
    private static double reliabilityProbability2 = 0.0d;
    private static double reliabilitySwitchTime = 0.0d;
    private static double reliabilitySwitchSpeed = 0.0d;
    private static double reliabilityPropagationRate = 0.0d;

    private static int buggingGroupNumber1 = 0;
    private static List<Double> buggingWorkersFraction1 = null;
    private static List<Double> buggingJobsFraction1 = null;
    private static int buggingGroupNumber2 = 0;
    private static List<Double> buggingWorkersFraction2 = null;
    private static List<Double> buggingJobsFraction2 = null;
    private static double buggingSwitchTime = 0.0d;
    private static double buggingSwitchSpeed = 0.0d;
    private static double buggingPropagationRate = 0.0d;

    private static int attackingGroupNumber1 = 0;
    private static List<Double> attackingWorkersFraction1 = null;
    private static List<Double> attackingProbability1 = null;
    private static int attackingGroupNumber2 = 0;
    private static List<Double> attackingWorkersFraction2 = null;
    private static List<Double> attackingProbability2 = null;
    private static double attackingSwitchTime = 0.0d;
    private static double attackingSwitchSpeed = 0.0d;
    private static double attackingPropagationRate = 0.0d;

    private static enum Parameter {
        SchedulerComponents, SchedulerSeed,
        JobsNumber, JobsCostMean, JobsCostStdDev, JobsArrivalRate, JobsSeed,
        WorkersNumber, WorkersSpeedMin, WorkersSpeedMax, WorkersSpeedMean,
        WorkersSpeedStdDev, WorkersSeed,
        ReliabilityWorkersFraction1, ReliabilityProbability1,
        ReliabilityWorkersFraction2, ReliabilityProbability2,
        ReliabilitySwitchTime, ReliabilitySwitchSpeed,
        ReliabilityPropagationRate, ReliabilitySeed,
        BuggingGroupNumber1, BuggingWorkersFraction1, BuggingJobsFraction1,
        BuggingGroupNumber2, BuggingWorkersFraction2, BuggingJobsFraction2,
        BuggingSwitchTime, BuggingSwitchSpeed, BuggingPropagationRate,
        BuggingSeed,
        AttackingGroupNumber1, AttackingWorkersFraction1, AttackingProbability1,
        AttackingGroupNumber2, AttackingWorkersFraction2, AttackingProbability2,
        AttackingSwitchTime, AttackingSwitchSpeed, AttackingPropagationRate,
        AttackingSeed;
    }

    @SuppressWarnings("unchecked")
    private static void addParameter(String name, String value) {
        try {
            switch (Parameter.valueOf(name)) {
                case JobsNumber: case WorkersNumber: case BuggingGroupNumber1:
                case BuggingGroupNumber2: case AttackingGroupNumber1:
                case AttackingGroupNumber2:
                    if (Integer.valueOf(value) < 0)
                        throw new Exception(value + " is negative");
                case JobsCostMean: case JobsCostStdDev: case JobsArrivalRate:
                case WorkersSpeedMin: case WorkersSpeedMax: case WorkersSpeedMean:
                case WorkersSpeedStdDev: case ReliabilityWorkersFraction1:
                case ReliabilityProbability1: case ReliabilityWorkersFraction2:
                case ReliabilityProbability2: case ReliabilitySwitchTime:
                case ReliabilitySwitchSpeed: case ReliabilityPropagationRate:
                case BuggingSwitchTime: case BuggingSwitchSpeed:
                case BuggingPropagationRate: case AttackingSwitchTime:
                case AttackingSwitchSpeed: case AttackingPropagationRate:
                    if (Double.valueOf(value) < 0)
                        throw new Exception(value + " is negative");
                    break;
                default:
            }
            switch (Parameter.valueOf(name)) {
                case SchedulerComponents:
                    List<String> components = parseList(String.class, value);
                    if (components.size() != 3) {
                        logger.severe("Necessarily 3 components in " + components);
                        System.exit(1);
                    }
                    String groupCreatorStr = "simdeg.scheduling." + components.get(0);
                    resourcesGrouper = (ResourcesGrouper)Class.forName
                        (groupCreatorStr).newInstance();
                    String answerSelectorStr = "simdeg.scheduling." + components.get(1);
                    resultCertificator = (ResultCertificator)Class.forName
                        (answerSelectorStr).newInstance();
                    String gridCharacteristicsStr = "simdeg.reputation." + components.get(2);
                    reputationSystem = (ReputationSystem)Class.forName
                        (gridCharacteristicsStr).newInstance();
                    break;
                case SchedulerSeed:
                    RandomManager.setSeed("scheduling", Long.valueOf(value));
                    break;
                case JobsNumber:
                    jobsNumber = Integer.valueOf(value);
                    break;
                case JobsCostMean:
                    jobsCostMean = Double.valueOf(value);
                    break;
                case JobsCostStdDev:
                    jobsCostStdDev = Double.valueOf(value);
                    break;
                case JobsArrivalRate:
                    jobsArrivalRate = Double.valueOf(value);
                    break;
                case JobsSeed:
                    RandomManager.setSeed("workload", Long.valueOf(value));
                    break;
                case WorkersNumber:
                    workersNumber = Integer.valueOf(value);
                    break;
                case WorkersSpeedMin:
                    workersSpeedMin = Double.valueOf(value);
                    break;
                case WorkersSpeedMax:
                    workersSpeedMax = Double.valueOf(value);
                    break;
                case WorkersSpeedMean:
                    workersSpeedMean = Double.valueOf(value);
                    break;
                case WorkersSpeedStdDev:
                    workersSpeedStdDev = Double.valueOf(value);
                    break;
                case WorkersSeed:
                    RandomManager.setSeed("platform", Long.valueOf(value));
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
                case ReliabilitySwitchTime:
                    reliabilitySwitchTime = Double.valueOf(value);
                    break;
                case ReliabilitySwitchSpeed:
                    reliabilitySwitchSpeed = Double.valueOf(value);
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
                case BuggingSwitchTime:
                    buggingSwitchTime = Double.valueOf(value);
                    break;
                case BuggingSwitchSpeed:
                    buggingSwitchSpeed = Double.valueOf(value);
                    break;
                case BuggingPropagationRate:
                    buggingPropagationRate = Double.valueOf(value);
                    break;
                case BuggingSeed:
                    RandomManager.setSeed("bugging", Long.valueOf(value));
                    break;
                case AttackingGroupNumber1:
                    attackingGroupNumber1 = Integer.valueOf(value);
                    break;
                case AttackingWorkersFraction1:
                    attackingWorkersFraction1 = parseList(Double.class, value);
                    break;
                case AttackingProbability1:
                    attackingProbability1 = parseList(Double.class, value);
                    break;
                case AttackingGroupNumber2:
                    attackingGroupNumber2 = Integer.valueOf(value);
                    break;
                case AttackingWorkersFraction2:
                    attackingWorkersFraction2 = parseList(Double.class, value);
                    break;
                case AttackingProbability2:
                    attackingProbability2 = parseList(Double.class, value);
                    break;
                case AttackingSwitchTime:
                    attackingSwitchTime = Double.valueOf(value);
                    break;
                case AttackingSwitchSpeed:
                    attackingSwitchSpeed = Double.valueOf(value);
                    break;
                case AttackingPropagationRate:
                    attackingPropagationRate = Double.valueOf(value);
                    break;
                case AttackingSeed:
                    RandomManager.setSeed("attacking", Long.valueOf(value));
                    break;
            }
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Scheduling component " + name
                    + " is not implemented", e);
            System.exit(1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while instantiating scheduling "
                    + "component " + name, e);
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
                && buggingJobsFraction2.size() >= buggingGroupNumber2
                && attackingWorkersFraction1.size() >= attackingGroupNumber1
                && attackingProbability1.size() >= attackingGroupNumber1
                && attackingWorkersFraction2.size() >= attackingGroupNumber2
                && attackingProbability2.size() >= attackingGroupNumber2)
            : "Not enough values in lists for the groups";
    }

    /**
     * Platform  file creation.
     */
    private static String platformFileGeneration(String fileName) {
        String platformFileName = fileName + "_platform.xml";
        File file = new File(platformFileName);
        DecimalFormat df = new DecimalFormat("0",
                new DecimalFormatSymbols(Locale.ENGLISH));
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("<?xml version='1.0'?>\n");
            fileWriter.write("<!DOCTYPE platform SYSTEM \"simgrid.dtd\">\n");
            fileWriter.write("<platform version=\"2\">\n");
            fileWriter.write("  <random id=\"rand_cluster\" min=\""
                    + df.format(workersSpeedMin) + "\" max=\""
                    + df.format(workersSpeedMax) + "\" mean=\""
                    + df.format(workersSpeedMean) + "\" std_deviation=\""
                    + df.format(workersSpeedStdDev) + "\"/>\n");
            fileWriter.write("  <cluster id=\"cluster\" prefix=\"worker\" " 
                    + "suffix=\"\" radical=\"0-" + (workersNumber-1)
                    + "\" power=\"$rand(rand_cluster)\" bw=\"100\" "
                    + "lat=\"0\" bb_bw=\"100\" bb_lat=\"0\"/>\n");
            fileWriter.write("</platform>\n");
            fileWriter.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while creating platform file "
                    + platformFileName, e);
            System.exit(1);
        }
        return platformFileName;
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
            final double start = reliabilitySwitchTime + offset;
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
            final double start = buggingSwitchTime + offset;
            final double end = start + 1.0d / buggingSwitchSpeed;
            switchers.add(new Switcher<Set<CollusionGroup>>(switcher, start, end));
        }
        assert(switchers.size() == workersNumber)
            : "Not enough bugging switchers";
        assert(startTimes.isEmpty()) : "Non deletion of double in a Set";
        return switchers;
    }

    /**
     * Creates switchers for attacking groups.
     */
    private static Set<Switcher<CollusionGroup>> createAttackingSwitchers() {
        /* Create pairs for attacking groups */
        List<CollusionGroup[]> pairAttacking
            = new ArrayList<CollusionGroup[]>();
        for (int i=0; i<workersNumber; i++)
            pairAttacking.add(new CollusionGroup[2]);
        /* Divide pairs in attacking groups for the first and second phase */
        List<Collection<CollusionGroup[]>> firstAttacking
            = new ArrayList<Collection<CollusionGroup[]>>();
        List<CollusionGroup[]> pairAttacking1
            = new ArrayList<CollusionGroup[]>(pairAttacking);
        for (int i=0; i<attackingGroupNumber1; i++) {
            final Collection<CollusionGroup[]> attack = getRandomSubGroup
                ((int)(attackingWorkersFraction1.get(i) * workersNumber),
                 pairAttacking1, RandomManager.getRandom("attacking"));
            pairAttacking1.removeAll(attack);
            firstAttacking.add(attack);
        }
        List<Collection<CollusionGroup[]>> secondAttacking
            = new ArrayList<Collection<CollusionGroup[]>>();
        List<CollusionGroup[]> pairAttacking2
            = new ArrayList<CollusionGroup[]>(pairAttacking);
        for (int i=0; i<attackingGroupNumber2; i++) {
            final Collection<CollusionGroup[]> attack = getRandomSubGroup
                ((int)(attackingWorkersFraction2.get(i) * workersNumber),
                 pairAttacking2, RandomManager.getRandom("attacking"));
            pairAttacking2.removeAll(attack);
            secondAttacking.add(attack);
        }
        /* Determine start-time offsets */
        Set<Double> startTimes = new HashSet<Double>();
        double last = 0.0d;
        for (int i=0; i<workersNumber; i++) {
            startTimes.add(last);
            last += RandomManager.getRandom("attacking")
                .nextExponential(1.0d / attackingPropagationRate);
        }
        /* Generate attacking groups */
        List<CollusionGroup> attackingGroups1 = new ArrayList<CollusionGroup>();
        for (int i=0; i<attackingGroupNumber1; i++)
            attackingGroups1.add(new CollusionGroup(attackingProbability1.get(i), "attacking"));
        List<CollusionGroup> attackingGroups2 = new ArrayList<CollusionGroup>();
        for (int i=0; i<attackingGroupNumber2; i++)
            attackingGroups2.add(new CollusionGroup(attackingProbability2.get(i), "attacking"));
        /* Generate switchers */
        Set<Switcher<CollusionGroup>> switchers
            = new HashSet<Switcher<CollusionGroup>>();
        for (CollusionGroup[] switcher : pairAttacking) {
            switcher[0] = null;
            for (int i=0; i<attackingGroupNumber1; i++)
                if (firstAttacking.get(i).contains(switcher))
                    switcher[0] = attackingGroups1.get(i);
            switcher[1] = null;
            for (int i=0; i<attackingGroupNumber2; i++)
                if (secondAttacking.get(i).contains(switcher))
                    switcher[1] = attackingGroups2.get(i);
            final Double offset = getRandomSubGroup(1, startTimes,
                    RandomManager.getRandom("attacking")).iterator().next();
            startTimes.remove(offset);
            final double start = attackingSwitchTime + offset;
            final double end = start + 1.0d / attackingSwitchSpeed;
            switchers.add(new Switcher<CollusionGroup>(switcher, start, end));
        }
        assert(switchers.size() == workersNumber)
            : "Not enough attacking switchers";
        assert(startTimes.isEmpty()) : "Non deletion of double in a Set";
        return switchers;
    }

    /**
     * Creation of workers with corresponding reliability and collusion
     * behavior.
     */
    private static Set<Worker> initializeWorkers()
        throws JniException, NativeException {

        /* Reliability, bugging and attacking probabilities generation */
        Set<Switcher<Double>> reliabilitySwitchers
            = createReliabilitySwitchers();
        Set<Switcher<Set<CollusionGroup>>> buggingSwitchers
            = createBuggingSwitchers();
        Set<Switcher<CollusionGroup>> attackingSwitchers
            = createAttackingSwitchers();

        /* Create data structure for the Evaluator */
        Map<Worker,Switcher<Double>> workersReliability
            = new HashMap<Worker,Switcher<Double>>();
        Map<Worker,Switcher<Set<CollusionGroup>>> buggingGroups
            = new HashMap<Worker,Switcher<Set<CollusionGroup>>>();
        Map<Worker,Switcher<CollusionGroup>> attackingGroups
            = new HashMap<Worker,Switcher<CollusionGroup>>();

        /* Build workers with the generated switchers */
        Host[] hosts = Host.all();
        for (int i=0; i<workersNumber; i++) {
            final Switcher<Double> reliable
                = reliabilitySwitchers.iterator().next();
            reliabilitySwitchers.remove(reliable);
            final Switcher<Set<CollusionGroup>> bugging
                = buggingSwitchers.iterator().next();
            buggingSwitchers.remove(bugging);
            final Switcher<CollusionGroup> attack
                = attackingSwitchers.iterator().next();
            attackingSwitchers.remove(attack);
            final Worker worker = new Worker(hosts[i], hosts[i].getName(),
                    reliable, bugging, attack);
            workersReliability.put(worker, reliable);
            buggingGroups.put(worker, bugging);
            attackingGroups.put(worker, attack);
            logger.config("Worker " + worker.getName()
                    + " created with reliability switcher " + reliable
                    + ", bugging groups switcher " + bugging
                    + " and attacking group switcher " + attack);
        }

        /* Inform the evaluator of the workers characteristics */
        Evaluator.setWorkersFaultiness(workersReliability,
                buggingGroups, attackingGroups);

        return workersReliability.keySet();
    }

    /**
     * Initialize the server with correct workload and scheduler.
     */
    private static void initializeServer(Set<Worker> workers)
        throws JniException, NativeException {
        new Server(workers, new Scheduler<Job,Result>(resourcesGrouper,
                    resultCertificator, reputationSystem),
                jobsNumber, jobsCostMean, jobsCostStdDev, jobsArrivalRate);
        Evaluator.setGridCharacteristics(reputationSystem);
    }

    /**
     * Main part of the simulator.
     */
    public static void main(String[] args)
        throws JniException, NativeException {

        Msg.init(args);

        if(args.length < 1) {
            logger.severe("Usage : java simulator.simulator configuration_file");
            System.exit(1);
        }

        /* Parse parameters from the configuration file */
        parseConfiguration(args[0]);

        /* Construct the platform (physical hosts) */
        String platformFileName = platformFileGeneration(args[0]);
        Msg.createEnvironment(platformFileName);

        /* Deploy the application */
        Set<Worker> workers = initializeWorkers();

        /* Server construction */
        initializeServer(workers);

        /* Launch the simulation */
        Msg.run();
    }

}
