package simdeg.simulation;

import java.util.Set;
import java.util.HashSet;

import simdeg.util.RandomManager;
import simdeg.util.OutOfRangeException;

class CollusionGroup {

    private double probability = 0.0d;

    private Set<Worker> workers = new HashSet<Worker>();

    private String randomKey;

    CollusionGroup(double probability, String randomKey) {
        /* Test for admissibility of parameters */
        if (probability < 0.0d || probability > 1.0d)
            throw new OutOfRangeException(probability, 0.0d, 1.0d);

        setProbability(probability);
        this.randomKey = randomKey;
    }

    double getProbability() {
        return probability;
    }

    void setProbability(double probability) {
        this.probability = probability;
    }

    boolean isColluding() {
        return RandomManager.getRandom(randomKey).nextDouble() < probability;
    }

    Set<Worker> getWorkers() {
        return workers;
    }

    void addWorker(Worker worker) {
        this.workers.add(worker);
    }

    boolean removeWorker(Worker worker) {
        return this.workers.remove(worker);
    }

    private static int count = 0;
    private final int hash = count++;
    public final int hashCode() {
        return hash;
    }

    public String toString() {
        return "{" + probability + " " + workers.toString() + "}";
    }

}
