package simdeg.scheduling;

import java.util.Set;
import java.util.HashSet;

import simdeg.reputation.Worker;

/**
 * Basic implementation of the ResourcesGrouper process which create groups of
 * maximum size.
 */
public class GluttonyResourcesGrouper extends ResourcesGrouper {

   /**
    * Gets a group containing every worker.
    */
    Set<Worker> getGroup(Worker worker) {
        return new HashSet<Worker>(this.workers);
    }

    /**
     * Return always null under normal circomstances because all the workers
     * should be in the initial group.
     */
    Set<Worker> getGroupExtension(Set<Worker> workers) {
         if (workers.size() == this.workers.size())
            return null;
        Set<Worker> currentWorkers = new HashSet<Worker>(this.workers);
        currentWorkers.removeAll(workers);
        return currentWorkers;
    }

}
