package simdeg.scheduling;

import java.util.Set;
import java.util.HashSet;

import simdeg.reputation.ReputationSystem;
import simdeg.reputation.Worker;

/**
 * Class used for the construction of group of worker.
 */
public abstract class ResourcesGrouper {

    ReputationSystem reputationSystem = null;

    /** Set of workers we are manipulating */
    protected Set<Worker> workers = new HashSet<Worker>();

    /**
     * Specifies the object characterizing the information we have on the grid.
     */
    void setReputationSystem(ReputationSystem reputationSystem) {
        this.reputationSystem = reputationSystem;
    }

    /**
     * Gives participating workers.
     */
    void addAllWorkers(Set<? extends Worker> workers) {
        this.workers.addAll(workers);
    }

    /**
     * Remove participating workers.
     */
    void removeAllWorkers(Set<? extends Worker> workers) {
        this.workers.removeAll(workers);
    }

    /**
     * Gets a group of workers with at least the one given in argument.
     */
    abstract Set<Worker> getGroup(Worker worker);

    /**
     * Gets an extension of the group of workers given in argument (in case
     * of results selection failure). Return null if all the known workers
     * are already in the initial group.
     */
    abstract Set<Worker> getGroupExtension(Set<Worker> workers);

}