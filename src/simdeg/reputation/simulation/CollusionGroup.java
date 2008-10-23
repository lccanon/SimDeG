package simdeg.reputation.simulation;

import java.util.Set;
import java.util.HashSet;
import java.lang.IllegalArgumentException;

import simdeg.util.RandomManager;

class CollusionGroup {

    private double probability = 0.0d;

    private Set<Worker> workers = new HashSet<Worker>();

    private String randomKey;

    protected CollusionGroup(double probability, String randomKey) {
        if (probability < 0.0d || probability > 1.0d)
            throw new IllegalArgumentException("Not a proper probability: "
                    + probability);
        setProbability(probability);
        this.randomKey = randomKey;
    }

    protected double getProbability() {
        return probability;
    }

    protected void setProbability(double probability) {
        this.probability = probability;
    }

    protected boolean isColluding() {
        return RandomManager.getRandom(randomKey).nextDouble() < probability;
    }

    protected Set<Worker> getWorkers() {
        return workers;
    }

    protected void addWorker(Worker worker) {
        this.workers.add(worker);
    }

    protected boolean removeWorker(Worker worker) {
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
